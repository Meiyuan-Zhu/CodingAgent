package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.zhumeiyuan.codingagent.agent.model.ModelClient;
import com.zhumeiyuan.codingagent.agent.model.ModelFinishReason;
import com.zhumeiyuan.codingagent.agent.model.ModelMessage;
import com.zhumeiyuan.codingagent.agent.model.ModelParseException;
import com.zhumeiyuan.codingagent.agent.model.ModelRequest;
import com.zhumeiyuan.codingagent.agent.model.ModelResponse;
import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.run.StopReason;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;

public class MockAgentRunner {

	private final AgentRunStore store;
	private final RunEventStream runEventStream;
	private final ToolRegistry toolRegistry;
	private final ModelClient modelClient;
	private final Clock clock;

	public MockAgentRunner(AgentRunStore store, RunEventStream runEventStream, ToolRegistry toolRegistry,
			ModelClient modelClient, Clock clock) {
		this.store = Objects.requireNonNull(store, "store");
		this.runEventStream = Objects.requireNonNull(runEventStream, "runEventStream");
		this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
		this.modelClient = Objects.requireNonNull(modelClient, "modelClient");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public void run(RunId runId, String prompt) {
		try {
			this.store.transition(runId, run -> run.start(this.clock));
			emit(runId, RunEventType.RUN_STARTED, Map.of("runner", "mock"));
			List<String> toolNames = this.toolRegistry.definitions().stream().map(ToolDefinition::name).toList();
			emit(runId, RunEventType.MODEL_REQUESTED, Map.of("provider", "mock", "availableTools", toolNames));

			ModelResponse response = this.modelClient.complete(new ModelRequest(List.of(
					ModelMessage.system("You are a local coding agent. Return the agreed JSON response format."),
					ModelMessage.user(prompt)), this.toolRegistry.definitions()));
			emit(runId, RunEventType.MODEL_MESSAGE_RECEIVED,
					Map.of("mock", true, "content", response.message(), "finishReason", response.finishReason().name()));

			if (response.finishReason() == ModelFinishReason.TOOL_CALLS) {
				for (ToolCall call : response.toolCalls()) {
					ToolResult result = executeTool(runId, call);
					if (!result.success()) {
						fail(runId, StopReason.TOOL_ERROR, result.content());
						return;
					}
				}
			}

			String summary = "Mock run completed through ModelClient, ModelResponseParser, and ToolRegistry.";
			emit(runId, RunEventType.MODEL_MESSAGE_RECEIVED, Map.of("mock", true, "content", summary));
			this.store.transition(runId, run -> run.succeed(this.clock));
			AgentRun run = this.store.get(runId);
			emit(runId, RunEventType.RUN_FINISHED,
					Map.of("status", run.status().name(), "stopReason", run.stopReason().name()));
		} catch (ModelParseException ex) {
			fail(runId, StopReason.MODEL_PARSE_ERROR, ex.getMessage());
		} catch (RuntimeException ex) {
			fail(runId, StopReason.INTERNAL_ERROR, "Mock runner failed");
		}
	}

	private ToolResult executeTool(RunId runId, ToolCall call) {
		emit(runId, RunEventType.TOOL_CALL_REQUESTED, Map.of(
				"toolCallId", call.id(),
				"name", call.name(),
				"arguments", call.arguments()));
		emit(runId, RunEventType.TOOL_CALL_STARTED, Map.of("toolCallId", call.id(), "name", call.name()));
		ToolResult result = this.toolRegistry.execute(call);
		emit(runId, RunEventType.TOOL_CALL_FINISHED, Map.of(
				"toolCallId", result.toolCallId(),
				"name", call.name(),
				"success", result.success(),
				"content", result.content(),
				"metadata", result.metadata()));
		return result;
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

	private RunEvent emit(RunId runId, RunEventType type, Map<String, Object> payload) {
		RunEvent event = this.store.appendEvent(runId, type, payload, this.clock);
		this.runEventStream.publish(event, this.store.get(runId).status().isTerminal());
		return event;
	}
}
