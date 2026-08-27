package com.zhumeiyuan.codingagent.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionResult;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;

import org.junit.jupiter.api.Test;

class MockAgentRunnerTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T09:30:00Z"), ZoneOffset.UTC);

	@Test
	void runsThroughRegistryAndFinishesSuccessfully() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), this.clock);

		runner.run(run.id(), "please inspect workspace");

		AgentRun finished = store.get(run.id());
		List<RunEvent> events = store.listEvents(run.id(), -1);
		assertThat(finished.status().name()).isEqualTo("SUCCEEDED");
		assertThat(events).extracting(RunEvent::type)
				.contains(RunEventType.RUN_STARTED, RunEventType.MODEL_REQUESTED,
						RunEventType.TOOL_CALL_REQUESTED, RunEventType.TOOL_CALL_FINISHED, RunEventType.RUN_FINISHED);
		assertThat(events).filteredOn(event -> event.type() == RunEventType.TOOL_CALL_REQUESTED)
				.singleElement()
				.satisfies(event -> assertThat(event.payload()).containsEntry("name", "list_files"));
	}

	@Test
	void failedToolResultFinishesRunAsFailed() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		ToolRegistry registry = new ToolRegistry(List.of(new RegisteredTool(
				new ToolDefinition("list_files", "List files", Map.of("type", "object")),
				arguments -> ToolExecutionResult.of("ignored", Map.of()))),
				this.clock) {
			@Override
			public ToolResult execute(ToolCall call) {
				return ToolResult.failure(call.id(), "tool failed", Map.of("toolName", call.name()),
						MockAgentRunnerTests.this.clock.instant());
			}
		};
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registry, this.clock);

		runner.run(run.id(), "please inspect workspace");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).errorMessage()).isEqualTo("tool failed");
	}

	private ToolRegistry registryReturningSuccess() {
		return new ToolRegistry(List.of(
				new RegisteredTool(new ToolDefinition("list_files", "List files", Map.of("type", "object")),
						arguments -> ToolExecutionResult.of("{\"files\":[]}", Map.of("path", "."))),
				new RegisteredTool(new ToolDefinition("read_file", "Read file", Map.of("type", "object")),
						arguments -> ToolExecutionResult.of("{\"content\":\"hello\"}", Map.of("path", "README.md"))),
				new RegisteredTool(new ToolDefinition("search_text", "Search text", Map.of("type", "object")),
						arguments -> ToolExecutionResult.of("{\"matches\":[]}", Map.of("query", "agent")))),
				this.clock);
	}
}
