package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
	private final Clock clock;

	public MockAgentRunner(AgentRunStore store, RunEventStream runEventStream, ToolRegistry toolRegistry, Clock clock) {
		this.store = Objects.requireNonNull(store, "store");
		this.runEventStream = Objects.requireNonNull(runEventStream, "runEventStream");
		this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public void run(RunId runId, String prompt) {
		try {
			this.store.transition(runId, run -> run.start(this.clock));
			emit(runId, RunEventType.RUN_STARTED, Map.of("runner", "mock"));
			List<String> toolNames = this.toolRegistry.definitions().stream().map(ToolDefinition::name).toList();
			emit(runId, RunEventType.MODEL_REQUESTED, Map.of("provider", "mock", "availableTools", toolNames));
			emit(runId, RunEventType.MODEL_MESSAGE_RECEIVED,
					Map.of("mock", true, "content", "Mock model selected a read-only workspace inspection plan."));

			ToolCall firstCall = chooseToolCall(prompt);
			ToolResult firstResult = executeTool(runId, firstCall);
			if (!firstResult.success()) {
				fail(runId, firstResult.content());
				return;
			}

			String summary = "Mock run completed. It used " + firstCall.name()
					+ " through the same registry that the real model loop will use later.";
			emit(runId, RunEventType.MODEL_MESSAGE_RECEIVED, Map.of("mock", true, "content", summary));
			this.store.transition(runId, run -> run.succeed(this.clock));
			AgentRun run = this.store.get(runId);
			emit(runId, RunEventType.RUN_FINISHED,
					Map.of("status", run.status().name(), "stopReason", run.stopReason().name()));
		} catch (RuntimeException ex) {
			fail(runId, "Mock runner failed");
		}
	}

	private ToolCall chooseToolCall(String prompt) {
		String lowerPrompt = prompt.toLowerCase();
		if (lowerPrompt.contains("readme")) {
			return new ToolCall(newToolCallId(), "read_file", Map.of("path", "README.md"));
		}
		if (lowerPrompt.contains("search") || prompt.contains("搜索") || prompt.contains("查找")) {
			return new ToolCall(newToolCallId(), "search_text", Map.of("query", "agent", "max_matches", 10));
		}
		return new ToolCall(newToolCallId(), "list_files", Map.of("path", ".", "max_entries", 50));
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

	private void fail(RunId runId, String message) {
		AgentRun current = this.store.get(runId);
		if (!current.status().isTerminal()) {
			this.store.transition(runId, run -> run.fail(StopReason.INTERNAL_ERROR, message, this.clock));
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

	private String newToolCallId() {
		return "tool-" + UUID.randomUUID();
	}
}
