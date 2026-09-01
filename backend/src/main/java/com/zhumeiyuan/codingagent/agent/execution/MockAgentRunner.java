package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.model.ModelClient;
import com.zhumeiyuan.codingagent.agent.model.ModelClientException;
import com.zhumeiyuan.codingagent.agent.model.ModelFinishReason;
import com.zhumeiyuan.codingagent.agent.model.ModelMessage;
import com.zhumeiyuan.codingagent.agent.model.ModelParseException;
import com.zhumeiyuan.codingagent.agent.model.ModelRequest;
import com.zhumeiyuan.codingagent.agent.model.ModelResponse;
import com.zhumeiyuan.codingagent.agent.model.ModelRole;
import com.zhumeiyuan.codingagent.agent.model.StreamingModelClient;
import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.run.RunStatus;
import com.zhumeiyuan.codingagent.agent.run.StopReason;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalDecision;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalPolicy;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionErrorCode;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;

public class MockAgentRunner {

	private static final String SYSTEM_PROMPT = """
			You are an autonomous coding agent operating inside a local project workspace.

			Your goal is to complete the user's programming task by inspecting, modifying, and verifying the project
			using the available tools.

			You do not have direct access to the filesystem or terminal. Always use the provided tools when you need to
			inspect files, modify files, or execute commands.

			Follow these principles:

			1. Inspect relevant project files before modifying them.
			2. Understand the existing implementation and avoid unnecessary changes.
			3. Make focused, minimal edits that directly address the user's request.
			4. Use the available tools instead of assuming file contents or execution results.
			5. After modifying code, run appropriate tests, builds, or commands when available.
			6. Treat tool errors and command failures as observations. Analyze the error, adjust your approach, and recover
			   when possible.
			7. Avoid repeatedly retrying the same failed action without changing your approach.
			8. Avoid unnecessary exploration or unrelated modifications.
			9. Do not claim the task is complete unless the result has been reasonably verified.
			10. When the task is complete, provide a concise summary of the changes and any verification performed.
			""";

	private final AgentRunStore store;
	private final RunEventStream runEventStream;
	private final ToolRegistry toolRegistry;
	private final ToolApprovalPolicy toolApprovalPolicy;
	private final ModelClient modelClient;
	private final RunBudget runBudget;
	private final WorkspaceChangeJournal changeJournal;
	private final ExecutorService toolExecutor;
	private final Clock clock;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public MockAgentRunner(AgentRunStore store, RunEventStream runEventStream, ToolRegistry toolRegistry,
			ToolApprovalPolicy toolApprovalPolicy, ModelClient modelClient, RunBudget runBudget, ExecutorService toolExecutor,
			Clock clock) {
		this(store, runEventStream, toolRegistry, toolApprovalPolicy, modelClient, runBudget, null, toolExecutor, clock);
	}

	public MockAgentRunner(AgentRunStore store, RunEventStream runEventStream, ToolRegistry toolRegistry,
			ToolApprovalPolicy toolApprovalPolicy, ModelClient modelClient, RunBudget runBudget,
			WorkspaceChangeJournal changeJournal, ExecutorService toolExecutor, Clock clock) {
		this.store = Objects.requireNonNull(store, "store");
		this.runEventStream = Objects.requireNonNull(runEventStream, "runEventStream");
		this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
		this.toolApprovalPolicy = Objects.requireNonNull(toolApprovalPolicy, "toolApprovalPolicy");
		this.modelClient = Objects.requireNonNull(modelClient, "modelClient");
		this.runBudget = Objects.requireNonNull(runBudget, "runBudget");
		this.changeJournal = changeJournal;
		this.toolExecutor = Objects.requireNonNull(toolExecutor, "toolExecutor");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public void run(RunId runId, String prompt) {
		try {
			AgentRun started = this.store.transition(runId, run -> {
				if (run.status() == RunStatus.CANCELLING || run.status().isTerminal()) {
					return run;
				}
				return run.start(this.clock);
			});
			if (started.status().isTerminal()) {
				return;
			}
			if (started.status() == RunStatus.CANCELLING) {
				completeCancellation(runId, false);
				return;
			}
			if (stopIfCancellationRequested(runId)) {
				return;
			}
			emit(runId, RunEventType.RUN_STARTED, Map.of("runner", this.modelClient.providerName(), "budget", budgetPayload()));

			List<ModelMessage> messages = new ArrayList<>();
			messages.add(ModelMessage.system(SYSTEM_PROMPT));
			messages.add(ModelMessage.user(prompt));
			continueRunLoop(runId, messages, 1, 0);
		} catch (ModelParseException ex) {
			fail(runId, StopReason.MODEL_PARSE_ERROR, ex.getMessage());
		} catch (ModelClientException ex) {
			fail(runId, StopReason.MODEL_ERROR, ex.getMessage());
		} catch (RuntimeException ex) {
			if (!stopIfCancellationRequested(runId)) {
				fail(runId, StopReason.INTERNAL_ERROR, "Agent runner failed");
			}
		}
	}

	public void resumeAfterApproval(PendingToolApproval approval) {
		Objects.requireNonNull(approval, "approval");
		RunId runId = approval.runId();
		try {
			if (stopIfCancellationRequested(runId)) {
				return;
			}
			ToolCall call = approval.toolCall();
			ToolResult result = executeApprovedTool(runId, approval.round(), call);
			if (result == null || isTerminal(runId)) {
				return;
			}
			int toolCallsUsed = approval.toolCallsUsed() + 1;
			List<ModelMessage> messages = new ArrayList<>(approval.messages());
			messages.add(ModelMessage.tool(call.id(), call.name(), toolObservation(call, result)));
			continueRunLoop(runId, messages, approval.round() + 1, toolCallsUsed);
		} catch (ModelParseException ex) {
			fail(runId, StopReason.MODEL_PARSE_ERROR, ex.getMessage());
		} catch (ModelClientException ex) {
			fail(runId, StopReason.MODEL_ERROR, ex.getMessage());
		} catch (RuntimeException ex) {
			if (!stopIfCancellationRequested(runId)) {
				fail(runId, StopReason.INTERNAL_ERROR, "Agent runner failed");
			}
		}
	}

	private void continueRunLoop(RunId runId, List<ModelMessage> messages, int firstRound, int toolCallsUsed) {
		for (int round = firstRound; round <= this.runBudget.maxRounds(); round++) {
			if (stopIfCancellationRequested(runId)) {
				return;
			}
			List<ModelMessage> context = contextWindow(messages);
			List<String> toolNames = this.toolRegistry.definitions().stream().map(ToolDefinition::name).toList();
			emit(runId, RunEventType.MODEL_REQUESTED, Map.of(
					"provider", this.modelClient.providerName(),
					"round", round,
					"availableTools", toolNames,
					"contextMessages", context.size(),
					"toolCallsUsed", toolCallsUsed));

			ModelResponse response = completeModel(runId, round, new ModelRequest(context, this.toolRegistry.definitions()));
			if (stopIfCancellationRequested(runId)) {
				return;
			}
			List<ToolCall> calls = acceptedToolCalls(response);
			emit(runId, RunEventType.MODEL_MESSAGE_RECEIVED, Map.of(
					"provider", this.modelClient.providerName(),
					"round", round,
					"content", response.message(),
					"finishReason", response.finishReason().name()));
			messages.add(ModelMessage.assistant(response.message(), calls));

			if (response.finishReason() == ModelFinishReason.STOP) {
				finishSuccessfully(runId, round, toolCallsUsed);
				return;
			}
			if (response.finishReason() == ModelFinishReason.LENGTH) {
				fail(runId, StopReason.TOKEN_BUDGET_LIMIT, "Model response stopped because of length limit");
				return;
			}

			if (toolCallsUsed + calls.size() > this.runBudget.maxToolCalls()) {
				fail(runId, StopReason.TOOL_CALL_LIMIT,
						"Run exceeded tool call limit of " + this.runBudget.maxToolCalls());
				return;
			}

			for (ToolCall call : calls) {
				if (stopIfCancellationRequested(runId)) {
					return;
				}
				ToolResult result = executeTool(runId, round, call, messages, toolCallsUsed);
				if (result == null || isTerminal(runId)) {
					return;
				}
				toolCallsUsed++;
				messages.add(ModelMessage.tool(call.id(), call.name(), toolObservation(call, result)));
			}
		}

		fail(runId, StopReason.ROUND_LIMIT, "Run exceeded round limit of " + this.runBudget.maxRounds());
	}

	private List<ToolCall> acceptedToolCalls(ModelResponse response) {
		if (response.finishReason() != ModelFinishReason.TOOL_CALLS) {
			return List.of();
		}
		return List.of(response.toolCalls().get(0));
	}

	private ModelResponse completeModel(RunId runId, int round, ModelRequest request) {
		if (this.modelClient instanceof StreamingModelClient streamingModelClient) {
			return streamingModelClient.completeStreaming(request, delta -> {
				if (!delta.isEmpty() && !isTerminal(runId)) {
					emit(runId, RunEventType.MODEL_MESSAGE_DELTA, Map.of(
							"provider", this.modelClient.providerName(),
							"round", round,
							"delta", delta));
				}
			});
		}
		return this.modelClient.complete(request);
	}

	private List<ModelMessage> contextWindow(List<ModelMessage> messages) {
		if (messages.size() <= this.runBudget.maxContextMessages()) {
			return List.copyOf(messages);
		}
		List<ModelMessage> window = new ArrayList<>();
		int start = 0;
		if (messages.get(0).role() == ModelRole.SYSTEM) {
			window.add(messages.get(0));
			start = 1;
		}

		int firstUser = firstUserIndex(messages, start);
		if (firstUser >= 0 && window.size() < this.runBudget.maxContextMessages()) {
			window.add(messages.get(firstUser));
			start = firstUser + 1;
		}

		List<ModelMessage> recent = new ArrayList<>();
		for (int index = messages.size() - 1; index >= start;) {
			List<ModelMessage> group = contextGroupEndingAt(messages, index, start);
			if (group.isEmpty()) {
				index--;
				continue;
			}
			if (window.size() + recent.size() + group.size() > this.runBudget.maxContextMessages()) {
				break;
			}
			recent.addAll(0, group);
			index -= group.size();
		}
		window.addAll(recent);
		return List.copyOf(window);
	}

	private int firstUserIndex(List<ModelMessage> messages, int start) {
		for (int index = start; index < messages.size(); index++) {
			if (messages.get(index).role() == ModelRole.USER) {
				return index;
			}
		}
		return -1;
	}

	private List<ModelMessage> contextGroupEndingAt(List<ModelMessage> messages, int end, int start) {
		ModelMessage last = messages.get(end);
		if (last.role() != ModelRole.TOOL) {
			return List.of(last);
		}
		int firstTool = end;
		while (firstTool > start && messages.get(firstTool - 1).role() == ModelRole.TOOL) {
			firstTool--;
		}
		int assistantIndex = firstTool - 1;
		if (assistantIndex < start || messages.get(assistantIndex).role() != ModelRole.ASSISTANT
				|| messages.get(assistantIndex).toolCalls().isEmpty()) {
			return List.of();
		}
		return messages.subList(assistantIndex, end + 1);
	}

	private ToolResult executeTool(RunId runId, int round, ToolCall call, List<ModelMessage> messages, int toolCallsUsed) {
		ToolApprovalDecision approval = this.toolApprovalPolicy.decide(call);
		emit(runId, RunEventType.TOOL_CALL_REQUESTED, Map.of(
				"round", round,
				"toolCallId", call.id(),
				"name", call.name(),
				"arguments", call.arguments(),
				"approval", approvalPayload(approval)));
		if (approval.requiresUserApproval()) {
			requestApproval(runId, round, call, approval, messages, toolCallsUsed);
			return null;
		}
		return executeApprovedTool(runId, round, call);
	}

	private ToolResult executeApprovedTool(RunId runId, int round, ToolCall call) {
		emit(runId, RunEventType.TOOL_CALL_STARTED, Map.of("round", round, "toolCallId", call.id(), "name", call.name()));
		ToolResult result = executeToolWithTimeout(runId, call);
		if (result == null || isTerminal(runId)) {
			return result;
		}
		if (this.changeJournal != null) {
			this.changeJournal.recordIfUndoable(runId, call.name(), result);
		}
		emit(runId, RunEventType.TOOL_CALL_FINISHED, Map.of(
				"round", round,
				"toolCallId", result.toolCallId(),
				"name", call.name(),
				"success", result.success(),
				"content", result.content(),
				"metadata", result.metadata(),
				"undoable", result.privateMetadata().containsKey(WorkspaceChangeJournal.UNDO_SNAPSHOT_KEY)));
		return result;
	}

	private void requestApproval(RunId runId, int round, ToolCall call, ToolApprovalDecision approval,
			List<ModelMessage> messages, int toolCallsUsed) {
		this.store.savePendingApproval(new PendingToolApproval(runId, round, call, approval, messages, toolCallsUsed));
		this.store.transition(runId, run -> run.waitForApproval(this.clock));
		emit(runId, RunEventType.APPROVAL_REQUIRED, Map.of(
				"round", round,
				"toolCallId", call.id(),
				"name", call.name(),
				"arguments", call.arguments(),
				"approval", approvalPayload(approval)));
	}

	private Map<String, Object> approvalPayload(ToolApprovalDecision approval) {
		return Map.of("mode", approval.mode().name(), "reason", approval.reason());
	}

	private ToolResult executeToolWithTimeout(RunId runId, ToolCall call) {
		Future<ToolResult> future = this.toolExecutor.submit(() -> this.toolRegistry.execute(call));
		try {
			return future.get(this.runBudget.toolTimeout().toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException ex) {
			future.cancel(true);
			Map<String, Object> metadata = Map.of(
					"toolName", call.name(),
					"errorCode", ToolExecutionErrorCode.TOOL_TIMEOUT.name(),
					"timeoutMillis", this.runBudget.toolTimeout().toMillis());
			return ToolResult.failure(call.id(), structuredToolFailure(call.name(), ToolExecutionErrorCode.TOOL_TIMEOUT,
					"Tool timed out after " + this.runBudget.toolTimeout().toMillis() + " ms", metadata), metadata,
					this.clock.instant());
		} catch (InterruptedException ex) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			completeCancellation(runId, true);
			return null;
		} catch (ExecutionException ex) {
			return ToolResult.failure(call.id(), "Tool failed unexpectedly",
					Map.of("toolName", call.name(), "errorCode", ToolExecutionErrorCode.TOOL_RUNTIME_ERROR.name()),
					this.clock.instant());
		}
	}

	private String toolObservation(ToolCall call, ToolResult result) {
		return "tool_call_id=" + call.id() + "\n"
				+ "tool_name=" + call.name() + "\n"
				+ "tool_execution_success=" + result.success() + "\n"
				+ result.content();
	}

	private String structuredToolFailure(String toolName, ToolExecutionErrorCode code, String message,
			Map<String, Object> metadata) {
		Map<String, Object> body = Map.of(
				"success", false,
				"message", message,
				"toolName", toolName,
				"errorCode", code.name(),
				"failureKind", "RECOVERABLE_TOOL_ERROR",
				"recoverable", true,
				"recoveryHint", "Use a narrower command or tool request, then try again within the budget.",
				"timedOut", code == ToolExecutionErrorCode.TOOL_TIMEOUT,
				"metadata", metadata);
		try {
			return this.objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException ex) {
			return "{\"success\":false,\"message\":\"Tool failed and the failure result could not be serialized.\"}";
		}
	}

	private void finishSuccessfully(RunId runId, int roundsUsed, int toolCallsUsed) {
		if (isTerminal(runId)) {
			return;
		}
		this.store.transition(runId, run -> run.succeed(this.clock));
		AgentRun run = this.store.get(runId);
		emit(runId, RunEventType.RUN_FINISHED, Map.of(
				"status", run.status().name(),
				"stopReason", run.stopReason().name(),
				"roundsUsed", roundsUsed,
				"toolCallsUsed", toolCallsUsed));
	}

	private boolean stopIfCancellationRequested(RunId runId) {
		AgentRun current = this.store.get(runId);
		if (current.status().isTerminal()) {
			return true;
		}
		if (current.status() == RunStatus.CANCELLING || Thread.currentThread().isInterrupted()) {
			completeCancellation(runId, Thread.currentThread().isInterrupted());
			return true;
		}
		return false;
	}

	private void completeCancellation(RunId runId, boolean interruptObserved) {
		AgentRun current = this.store.get(runId);
		if (current.status().isTerminal()) {
			return;
		}
		this.store.transition(runId, run -> run.status() == RunStatus.CANCELLING ? run : run.requestCancel(this.clock));
		emit(runId, RunEventType.RUN_CANCELLING, Map.of("reason", "runner_observed", "interruptObserved", interruptObserved));
		this.store.transition(runId, run -> run.cancel(this.clock));
		AgentRun cancelled = this.store.get(runId);
		emit(runId, RunEventType.RUN_FINISHED, Map.of(
				"status", cancelled.status().name(),
				"stopReason", cancelled.stopReason().name(),
				"interruptObserved", interruptObserved));
	}

	private void fail(RunId runId, StopReason stopReason, String message) {
		AgentRun current = this.store.get(runId);
		if (!current.status().isTerminal()) {
			this.store.transition(runId, run -> run.fail(stopReason, message, this.clock));
			AgentRun failed = this.store.get(runId);
			emit(runId, RunEventType.RUN_FINISHED, Map.of(
					"status", failed.status().name(),
					"stopReason", failed.stopReason().name(),
					"errorMessage", failed.errorMessage()));
		}
	}

	private boolean isTerminal(RunId runId) {
		return this.store.get(runId).status().isTerminal();
	}

	private Map<String, Object> budgetPayload() {
		return Map.of(
				"maxRounds", this.runBudget.maxRounds(),
				"maxToolCalls", this.runBudget.maxToolCalls(),
				"maxContextMessages", this.runBudget.maxContextMessages(),
				"toolTimeoutMillis", this.runBudget.toolTimeout().toMillis());
	}

	private RunEvent emit(RunId runId, RunEventType type, Map<String, Object> payload) {
		if (type != RunEventType.RUN_FINISHED && this.store.get(runId).status().isTerminal()) {
			return null;
		}
		RunEvent event = this.store.appendEvent(runId, type, payload, this.clock);
		this.runEventStream.publish(event, this.store.get(runId).status().isTerminal());
		return event;
	}
}
