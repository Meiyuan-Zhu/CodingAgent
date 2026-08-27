package com.zhumeiyuan.codingagent.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zhumeiyuan.codingagent.agent.model.ModelClient;
import com.zhumeiyuan.codingagent.agent.model.ModelFinishReason;
import com.zhumeiyuan.codingagent.agent.model.ModelMessage;
import com.zhumeiyuan.codingagent.agent.model.ModelParseException;
import com.zhumeiyuan.codingagent.agent.model.ModelRequest;
import com.zhumeiyuan.codingagent.agent.model.ModelResponse;
import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.StopReason;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionResult;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;

import org.junit.jupiter.api.Test;

class MockAgentRunnerTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T09:30:00Z"), ZoneOffset.UTC);
	private final RunBudget budget = new RunBudget(4, 12, 30);

	@Test
	void runsMultipleRoundsThroughRegistryAndFinishesSuccessfully() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		ModelClient model = new SequencedModelClient(
				new ModelResponse("List files", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-1", "list_files", Map.of("path", ".")))),
				new ModelResponse("Done after observing files", ModelFinishReason.STOP, List.of()));
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), model,
				this.budget, this.clock);

		runner.run(run.id(), "please inspect workspace");

		AgentRun finished = store.get(run.id());
		List<RunEvent> events = store.listEvents(run.id(), -1);
		assertThat(finished.status().name()).isEqualTo("SUCCEEDED");
		assertThat(events).extracting(RunEvent::type)
				.contains(RunEventType.RUN_STARTED, RunEventType.MODEL_REQUESTED,
						RunEventType.TOOL_CALL_REQUESTED, RunEventType.TOOL_CALL_FINISHED, RunEventType.RUN_FINISHED);
		assertThat(events).filteredOn(event -> event.type() == RunEventType.MODEL_REQUESTED).hasSize(2);
		assertThat(events).filteredOn(event -> event.type() == RunEventType.TOOL_CALL_REQUESTED)
				.singleElement()
				.satisfies(event -> assertThat(event.payload()).containsEntry("name", "list_files"));
		assertThat(events).filteredOn(event -> event.type() == RunEventType.RUN_FINISHED)
				.singleElement()
				.satisfies(event -> assertThat(event.payload())
						.containsEntry("roundsUsed", 2)
						.containsEntry("toolCallsUsed", 1));
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
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registry,
				request -> new ModelResponse("List files",
						ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-1", "list_files", Map.of("path", ".")))),
				this.budget, this.clock);

		runner.run(run.id(), "please inspect workspace");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.TOOL_ERROR);
		assertThat(store.get(run.id()).errorMessage()).isEqualTo("tool failed");
	}

	@Test
	void modelParseErrorFinishesRunWithParseStopReason() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		ModelClient failingModel = request -> {
			throw new ModelParseException("bad response");
		};
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), failingModel,
				this.budget, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.MODEL_PARSE_ERROR);
		assertThat(store.get(run.id()).errorMessage()).isEqualTo("bad response");
	}

	@Test
	void lengthFinishReasonStopsWithTokenBudgetLimit() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(),
				request -> new ModelResponse("partial", ModelFinishReason.LENGTH, List.of()), this.budget, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.TOKEN_BUDGET_LIMIT);
	}

	@Test
	void roundLimitStopsRunWhenModelKeepsRequestingTools() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		RunBudget smallBudget = new RunBudget(2, 10, 30);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(),
				request -> new ModelResponse("again", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-" + request.messages().size(), "list_files", Map.of("path", ".")))),
				smallBudget, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.ROUND_LIMIT);
		assertThat(store.listEvents(run.id(), -1)).filteredOn(event -> event.type() == RunEventType.MODEL_REQUESTED)
				.hasSize(2);
	}

	@Test
	void toolCallLimitStopsBeforeExecutingTooManyCalls() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		RunBudget smallBudget = new RunBudget(4, 1, 30);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(),
				request -> new ModelResponse("two calls", ModelFinishReason.TOOL_CALLS,
						List.of(
								new ToolCall("call-1", "list_files", Map.of("path", ".")),
								new ToolCall("call-2", "read_file", Map.of("path", "README.md")))),
				smallBudget, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.TOOL_CALL_LIMIT);
		assertThat(store.listEvents(run.id(), -1)).filteredOn(event -> event.type() == RunEventType.TOOL_CALL_STARTED)
				.isEmpty();
	}

	@Test
	void contextWindowKeepsSystemPromptAndRecentMessages() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		List<ModelRequest> requests = new ArrayList<>();
		RunBudget smallContext = new RunBudget(3, 10, 4);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), request -> {
			requests.add(request);
			if (requests.size() < 3) {
				return new ModelResponse("again", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-" + requests.size(), "list_files", Map.of("path", "."))));
			}
			return new ModelResponse("done", ModelFinishReason.STOP, List.of());
		}, smallContext, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("SUCCEEDED");
		assertThat(requests).hasSize(3);
		ModelRequest thirdRequest = requests.get(2);
		assertThat(thirdRequest.messages()).hasSize(4);
		assertThat(thirdRequest.messages().get(0).role().name()).isEqualTo("SYSTEM");
		assertThat(thirdRequest.messages()).extracting(ModelMessage::content).last().asString().contains("tool_call_id=call-2");
	}

	private ToolRegistry registryReturningSuccess() {
		return new ToolRegistry(List.of(
				new RegisteredTool(new ToolDefinition("list_files", "List files", Map.of("type", "object")),
						arguments -> ToolExecutionResult.of("{\"files\":[]}", Map.of("path", "."))),
				new RegisteredTool(new ToolDefinition("read_file", "Read file", Map.of("type", "object")),
						arguments -> ToolExecutionResult.of("{\"content\":\"hello\"}", Map.of("path", "README.md"))),
				new RegisteredTool(new ToolDefinition("search_text", "Search text", Map.of("type", "object")),
						arguments -> ToolExecutionResult.of("{\"matches\":[]}", Map.of("query", "agent"))),
				new RegisteredTool(new ToolDefinition("write_file", "Write file", Map.of("type", "object")),
						arguments -> ToolExecutionResult.of("{\"created\":true}", Map.of("path", "new.txt"))),
				new RegisteredTool(new ToolDefinition("replace_text", "Replace text", Map.of("type", "object")),
						arguments -> ToolExecutionResult.of("{\"replacements\":1}", Map.of("path", "README.md")))),
				this.clock);
	}

	private static class SequencedModelClient implements ModelClient {
		private final List<ModelResponse> responses;
		private int index;

		SequencedModelClient(ModelResponse... responses) {
			this.responses = List.of(responses);
		}

		@Override
		public ModelResponse complete(ModelRequest request) {
			ModelResponse response = this.responses.get(Math.min(this.index, this.responses.size() - 1));
			this.index++;
			return response;
		}
	}
}
