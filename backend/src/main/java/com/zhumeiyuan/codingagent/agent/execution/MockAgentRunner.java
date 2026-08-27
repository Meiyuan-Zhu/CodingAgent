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

import com.zhumeiyuan.codingagent.agent.model.ModelClient;
import com.zhumeiyuan.codingagent.agent.model.ModelFinishReason;
import com.zhumeiyuan.codingagent.agent.model.ModelMessage;
import com.zhumeiyuan.codingagent.agent.model.ModelParseException;
import com.zhumeiyuan.codingagent.agent.model.ModelRequest;
import com.zhumeiyuan.codingagent.agent.model.ModelResponse;
import com.zhumeiyuan.codingagent.agent.model.ModelRole;
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

	private static final String SYSTEM_PROMPT = "You are a local coding agent. Return the agreed JSON response format.";

	private final AgentRunStore store;
	private final RunEventStream runEventStream;
	private final ToolRegistry toolRegistry;
	private final ToolApprovalPolicy toolApprovalPolicy;
	private final ModelClient modelClient;
	private final RunBudget runBudget;
	private final ExecutorService toolExecutor;
	private final Clock clock;

	public MockAgentRunner(AgentRunStore store, RunEventStream runEventStream, ToolRegistry toolRegistry,
			ToolApprovalPolicy toolApprovalPolicy, ModelClient modelClient, RunBudget runBudget, ExecutorService toolExecutor,
			Clock clock) {
		this.store = Objects.requireNonNull(store, "store");
		this.runEventStream = Objects.requireNonNull(runEventStream, "runEventStream");
		this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
		this.toolApprovalPolicy = Objects.requireNonNull(toolApprovalPolicy, "toolApprovalPolicy");
		this.modelClient = Objects.requireNonNull(modelClient, "modelClient");
		this.runBudget = Objects.requireNonNull(runBudget, "runBudget");
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
			emit(runId, RunEventType.RUN_STARTED, Map.of("runner", "mock", "budget", budgetPayload()));

			List<ModelMessage> messages = new ArrayList<>();
			messages.add(ModelMessage.system(SYSTEM_PROMPT));
			messages.add(ModelMessage.user(prompt));
			int toolCallsUsed = 0;

			for (int round = 1; round <= this.runBudget.maxRounds(); round++) {
				if (stopIfCancellationRequested(runId)) {
					return;
				}
				List<ModelMessage> context = contextWindow(messages);
				List<String> toolNames = this.toolRegistry.definitions().stream().map(ToolDefinition::name).toList();
				emit(runId, RunEventType.MODEL_REQUESTED, Map.of(
						"provider", "mock",
						"round", round,
						"availableTools", toolNames,
						"contextMessages", context.size(),
						"toolCallsUsed", toolCallsUsed));

				ModelResponse response = this.modelClient.complete(new ModelRequest(context, this.toolRegistry.definitions()));
				if (stopIfCancellationRequested(runId)) {
					return;
				}
				emit(runId, RunEventType.MODEL_MESSAGE_RECEIVED, Map.of(
						"mock", true,
						"round", round,
						"content", response.message(),
						"finishReason", response.finishReason().name()));
				messages.add(ModelMessage.assistant(response.message()));

				if (response.finishReason() == ModelFinishReason.STOP) {
					finishSuccessfully(runId, round, toolCallsUsed);
					return;
				}
				if (response.finishReason() == ModelFinishReason.LENGTH) {
					fail(runId, StopReason.TOKEN_BUDGET_LIMIT, "Model response stopped because of length limit");
					return;
				}

				List<ToolCall> calls = response.toolCalls();
				if (toolCallsUsed + calls.size() > this.runBudget.maxToolCalls()) {
					fail(runId, StopReason.TOOL_CALL_LIMIT,
							"Run exceeded tool call limit of " + this.runBudget.maxToolCalls());
					return;
				}

				for (ToolCall call : calls) {
					if (stopIfCancellationRequested(runId)) {
						return;
					}
					ToolResult result = executeTool(runId, round, call);
					if (result == null || isTerminal(runId)) {
						return;
					}
					toolCallsUsed++;
					messages.add(ModelMessage.tool(toolObservation(call, result)));
					if (!result.success()) {
						StopReason stopReason = isToolTimeout(result) ? StopReason.TIME_LIMIT : StopReason.TOOL_ERROR;
						fail(runId, stopReason, result.content());
						return;
					}
				}
			}

			fail(runId, StopReason.ROUND_LIMIT, "Run exceeded round limit of " + this.runBudget.maxRounds());
		} catch (ModelParseException ex) {
			fail(runId, StopReason.MODEL_PARSE_ERROR, ex.getMessage());
		} catch (RuntimeException ex) {
			if (!stopIfCancellationRequested(runId)) {
				fail(runId, StopReason.INTERNAL_ERROR, "Mock runner failed");
			}
		}
	}

	private List<ModelMessage> contextWindow(List<ModelMessage> messages) {
		if (messages.size() <= this.runBudget.maxContextMessages()) {
			return List.copyOf(messages);
		}
		int maxTail = this.runBudget.maxContextMessages();
		List<ModelMessage> window = new ArrayList<>();
		ModelMessage first = messages.get(0);
		if (first.role() == ModelRole.SYSTEM) {
			window.add(first);
			maxTail--;
		}
		window.addAll(messages.subList(messages.size() - maxTail, messages.size()));
		return List.copyOf(window);
	}

	private ToolResult executeTool(RunId runId, int round, ToolCall call) {
		ToolApprovalDecision approval = this.toolApprovalPolicy.decide(call);
		emit(runId, RunEventType.TOOL_CALL_REQUESTED, Map.of(
				"round", round,
				"toolCallId", call.id(),
				"name", call.name(),
				"arguments", call.arguments(),
				"approval", approvalPayload(approval)));
		if (approval.requiresUserApproval()) {
			requestApproval(runId, round, call, approval);
			return null;
		}
		emit(runId, RunEventType.TOOL_CALL_STARTED, Map.of("round", round, "toolCallId", call.id(), "name", call.name()));
		ToolResult result = executeToolWithTimeout(runId, call);
		if (result == null || isTerminal(runId)) {
			return result;
		}
		emit(runId, RunEventType.TOOL_CALL_FINISHED, Map.of(
				"round", round,
				"toolCallId", result.toolCallId(),
				"name", call.name(),
				"success", result.success(),
				"content", result.content(),
				"metadata", result.metadata()));
		return result;
	}

	private void requestApproval(RunId runId, int round, ToolCall call, ToolApprovalDecision approval) {
		this.store.transition(runId, run -> run.waitForApproval(this.clock));
		emit(runId, RunEventType.APPROVAL_REQUIRED, Map.of(
				"round", round,
				"toolCallId", call.id(),
				"name", call.name(),
				"arguments", call.arguments(),
				"approval", approvalPayload(approval)));
		emit(runId, RunEventType.APPROVAL_RESOLVED, Map.of(
				"round", round,
				"toolCallId", call.id(),
				"name", call.name(),
				"approved", false,
				"reason", "approval_resume_api_not_implemented"));
		fail(runId, StopReason.APPROVAL_REJECTED,
				"Tool requires user approval before execution: " + call.name());
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
			return ToolResult.failure(call.id(), "Tool timed out after " + this.runBudget.toolTimeout().toMillis() + " ms",
					Map.of(
							"toolName", call.name(),
							"errorCode", ToolExecutionErrorCode.TOOL_TIMEOUT.name(),
							"timeoutMillis", this.runBudget.toolTimeout().toMillis()),
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
				+ "success=" + result.success() + "\n"
				+ result.content();
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

	private boolean isToolTimeout(ToolResult result) {
		return ToolExecutionErrorCode.TOOL_TIMEOUT.name().equals(result.metadata().get("errorCode"));
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
