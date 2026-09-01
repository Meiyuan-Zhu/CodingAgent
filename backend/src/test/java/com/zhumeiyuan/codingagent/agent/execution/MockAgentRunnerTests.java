package com.zhumeiyuan.codingagent.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.model.ModelClient;
import com.zhumeiyuan.codingagent.agent.model.ModelClientException;
import com.zhumeiyuan.codingagent.agent.model.ModelFinishReason;
import com.zhumeiyuan.codingagent.agent.model.ModelMessage;
import com.zhumeiyuan.codingagent.agent.model.ModelRole;
import com.zhumeiyuan.codingagent.agent.model.ModelParseException;
import com.zhumeiyuan.codingagent.agent.model.ModelRequest;
import com.zhumeiyuan.codingagent.agent.model.ModelResponse;
import com.zhumeiyuan.codingagent.agent.model.ModelStreamListener;
import com.zhumeiyuan.codingagent.agent.model.StreamingModelClient;
import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunStatus;
import com.zhumeiyuan.codingagent.agent.run.StopReason;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalPolicy;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionErrorCode;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionResult;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;
import com.zhumeiyuan.codingagent.agent.tool.workspace.WorkspaceToolFactory;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspacePathResolver;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceReadTools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MockAgentRunnerTests {

	@TempDir
	Path tempDir;

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T09:30:00Z"), ZoneOffset.UTC);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RunBudget budget = new RunBudget(4, 12, 30);
	private final ExecutorService toolExecutor = Executors.newSingleThreadExecutor();

	@AfterEach
	void shutdownToolExecutor() {
		this.toolExecutor.shutdownNow();
	}

	@Test
	void runsMultipleRoundsThroughRegistryAndFinishesSuccessfully() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		SequencedModelClient model = new SequencedModelClient(
				new ModelResponse("List files", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-1", "list_files", Map.of("path", ".")))),
				new ModelResponse("Done after observing files", ModelFinishReason.STOP, List.of()));
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), new ToolApprovalPolicy(), model,
				this.budget, this.toolExecutor, this.clock);

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
		ModelRequest secondRequest = model.requests().get(1);
		assertThat(secondRequest.messages()).filteredOn(message -> message.role() == ModelRole.ASSISTANT)
				.singleElement()
				.satisfies(message -> assertThat(message.toolCalls()).singleElement().satisfies(call -> {
					assertThat(call.id()).isEqualTo("call-1");
					assertThat(call.name()).isEqualTo("list_files");
				}));
	}

	@Test
	void systemPromptDefinesAgentOperatingPolicy() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		List<ModelRequest> requests = new ArrayList<>();
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(),
				new ToolApprovalPolicy(), request -> {
					requests.add(request);
					return new ModelResponse("done", ModelFinishReason.STOP, List.of());
				}, this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(requests).hasSize(1);
		String prompt = requests.get(0).messages().get(0).content();
		assertThat(prompt)
				.contains("autonomous coding agent")
				.contains("local project workspace")
				.contains("do not have direct access to the filesystem or terminal")
				.contains("Inspect relevant project files before modifying them")
				.contains("Treat tool errors and command failures as observations")
				.contains("Avoid repeatedly retrying the same failed action")
				.contains("reasonably verified");
	}

	@Test
	void emitsModelMessageDeltaEventsForStreamingClients() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		StreamingModelClient model = new StreamingModelClient() {
			@Override
			public ModelResponse complete(ModelRequest request) {
				return new ModelResponse("Hello", ModelFinishReason.STOP, List.of());
			}

			@Override
			public ModelResponse completeStreaming(ModelRequest request, ModelStreamListener listener) {
				listener.onTextDelta("Hel");
				listener.onTextDelta("lo");
				return new ModelResponse("Hello", ModelFinishReason.STOP, List.of());
			}

			@Override
			public String providerName() {
				return "streaming-test";
			}
		};
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(),
				new ToolApprovalPolicy(), model, this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "say hello");

		assertThat(store.get(run.id()).status()).isEqualTo(RunStatus.SUCCEEDED);
		assertThat(store.listEvents(run.id(), -1)).filteredOn(event -> event.type() == RunEventType.MODEL_MESSAGE_DELTA)
				.extracting(event -> event.payload().get("delta"))
				.containsExactly("Hel", "lo");
		assertThat(store.listEvents(run.id(), -1)).filteredOn(event -> event.type() == RunEventType.MODEL_MESSAGE_RECEIVED)
				.singleElement()
				.satisfies(event -> assertThat(event.payload()).containsEntry("content", "Hello"));
	}

	@Test
	void failedToolResultIsReturnedToModelAsObservation() {
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
		SequencedModelClient model = new SequencedModelClient(
				new ModelResponse("List files",
						ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-1", "list_files", Map.of("path", ".")))),
				new ModelResponse("Recovered after the failed tool observation", ModelFinishReason.STOP, List.of()));
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registry, new ToolApprovalPolicy(),
				model, this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "please inspect workspace");

		assertThat(store.get(run.id()).status().name()).isEqualTo("SUCCEEDED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.COMPLETED);
		assertThat(model.requests()).hasSize(2);
		assertThat(model.requests().get(1).messages()).filteredOn(message -> message.role() == ModelRole.TOOL)
				.singleElement()
				.satisfies(message -> assertThat(message.content())
						.contains("tool_call_id=call-1")
						.contains("tool_name=list_files")
						.contains("tool_execution_success=false")
						.contains("tool failed"));
	}

	@Test
	void recoverableWorkspaceErrorCanDriveTheNextToolAttempt() throws IOException {
		Path root = this.tempDir.resolve("workspace");
		Files.createDirectories(root);
		Files.writeString(root.resolve("README.md"), "hello from the real file\n");
		WorkspaceReadTools readTools = new WorkspaceReadTools(new WorkspacePathResolver(root));
		ToolRegistry registry = new ToolRegistry(List.of(
				WorkspaceToolFactory.listFiles(readTools, this.objectMapper),
				WorkspaceToolFactory.readFile(readTools, this.objectMapper)),
				this.clock);
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		SequencedModelClient model = new SequencedModelClient(
				new ModelResponse("Try the requested file", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-1", "read_file", Map.of("path", "src/foo.py")))),
				new ModelResponse("The path was wrong, inspect files", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-2", "list_files", Map.of("path", ".")))),
				new ModelResponse("Read the discovered file", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-3", "read_file", Map.of("path", "README.md")))),
				new ModelResponse("Done after recovering from the missing file", ModelFinishReason.STOP, List.of()));
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registry, new ToolApprovalPolicy(),
				model, new RunBudget(6, 6, 30), this.toolExecutor, this.clock);

		runner.run(run.id(), "read the project file");

		assertThat(store.get(run.id()).status()).isEqualTo(RunStatus.SUCCEEDED);
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.COMPLETED);
		assertThat(model.requests()).hasSize(4);
		assertThat(model.requests().get(1).messages()).filteredOn(message -> message.role() == ModelRole.TOOL)
				.singleElement()
				.satisfies(message -> assertThat(message.content())
						.contains("tool_execution_success=false")
						.contains("\"errorCode\":\"" + ToolExecutionErrorCode.WORKSPACE_NOT_FOUND.name() + "\"")
						.contains("\"failureKind\":\"RECOVERABLE_TOOL_ERROR\"")
						.contains("\"recoverable\":true")
						.contains("list_files"));
		assertThat(store.listEvents(run.id(), -1)).filteredOn(event -> event.type() == RunEventType.TOOL_CALL_FINISHED)
				.extracting(event -> event.payload().get("success"))
				.containsExactly(false, true, true);
	}

	@Test
	void modelParseErrorFinishesRunWithParseStopReason() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		ModelClient failingModel = request -> {
			throw new ModelParseException("bad response");
		};
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), new ToolApprovalPolicy(), failingModel,
				this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.MODEL_PARSE_ERROR);
		assertThat(store.get(run.id()).errorMessage()).isEqualTo("bad response");
	}

	@Test
	void modelClientErrorFinishesRunWithModelError() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		ModelClient failingModel = request -> {
			throw new ModelClientException("provider unavailable");
		};
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(),
				new ToolApprovalPolicy(), failingModel, this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.MODEL_ERROR);
		assertThat(store.get(run.id()).errorMessage()).isEqualTo("provider unavailable");
	}

	@Test
	void lengthFinishReasonStopsWithTokenBudgetLimit() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), new ToolApprovalPolicy(),
				request -> new ModelResponse("partial", ModelFinishReason.LENGTH, List.of()), this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.TOKEN_BUDGET_LIMIT);
	}

	@Test
	void roundLimitStopsRunWhenModelKeepsRequestingTools() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		RunBudget smallBudget = new RunBudget(2, 10, 30);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), new ToolApprovalPolicy(),
				request -> new ModelResponse("again", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-" + request.messages().size(), "list_files", Map.of("path", ".")))),
				smallBudget, this.toolExecutor, this.clock);

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
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), new ToolApprovalPolicy(),
				request -> new ModelResponse("two calls", ModelFinishReason.TOOL_CALLS,
						List.of(
								new ToolCall("call-1", "list_files", Map.of("path", ".")),
								new ToolCall("call-2", "read_file", Map.of("path", "README.md")))),
				smallBudget, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("FAILED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.TOOL_CALL_LIMIT);
		assertThat(store.listEvents(run.id(), -1)).filteredOn(event -> event.type() == RunEventType.TOOL_CALL_STARTED)
				.hasSize(1);
	}

	@Test
	void acceptsOnlyFirstToolCallFromEachModelResponse() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		SequencedModelClient model = new SequencedModelClient(
				new ModelResponse("multiple calls", ModelFinishReason.TOOL_CALLS,
						List.of(
								new ToolCall("call-1", "list_files", Map.of("path", ".")),
								new ToolCall("call-2", "read_file", Map.of("path", "README.md")))),
				new ModelResponse("Done after first observation", ModelFinishReason.STOP, List.of()));
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(),
				new ToolApprovalPolicy(), model, this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status()).isEqualTo(RunStatus.SUCCEEDED);
		assertThat(store.listEvents(run.id(), -1)).filteredOn(event -> event.type() == RunEventType.TOOL_CALL_STARTED)
				.singleElement()
				.satisfies(event -> assertThat(event.payload()).containsEntry("toolCallId", "call-1"));
		assertThat(model.requests()).hasSize(2);
		assertThat(model.requests().get(1).messages()).filteredOn(message -> message.role() == ModelRole.ASSISTANT)
				.singleElement()
				.satisfies(message -> assertThat(message.toolCalls()).singleElement()
						.satisfies(call -> assertThat(call.id()).isEqualTo("call-1")));
		assertThat(model.requests().get(1).messages()).filteredOn(message -> message.role() == ModelRole.TOOL)
				.singleElement()
				.satisfies(message -> assertThat(message.toolCallId()).isEqualTo("call-1"));
	}

	@Test
	void contextWindowKeepsSystemPromptAndRecentMessages() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		List<ModelRequest> requests = new ArrayList<>();
		RunBudget smallContext = new RunBudget(3, 10, 4);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), new ToolApprovalPolicy(), request -> {
			requests.add(request);
			if (requests.size() < 3) {
				return new ModelResponse("again", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-" + requests.size(), "list_files", Map.of("path", "."))));
			}
			return new ModelResponse("done", ModelFinishReason.STOP, List.of());
		}, smallContext, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("SUCCEEDED");
		assertThat(requests).hasSize(3);
		ModelRequest thirdRequest = requests.get(2);
		assertThat(thirdRequest.messages()).hasSize(4);
		assertThat(thirdRequest.messages().get(0).role().name()).isEqualTo("SYSTEM");
		assertThat(thirdRequest.messages().get(1).role().name()).isEqualTo("USER");
		assertToolResultsHaveMatchingAssistantCalls(thirdRequest.messages());
		assertThat(thirdRequest.messages()).extracting(ModelMessage::content).last().asString().contains("tool_call_id=call-2");
	}

	@Test
	void contextWindowDoesNotKeepOrphanToolMessagesWhenTrimming() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		List<ModelRequest> requests = new ArrayList<>();
		RunBudget smallContext = new RunBudget(4, 10, 4);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(),
				new ToolApprovalPolicy(), request -> {
					requests.add(request);
					if (requests.size() < 4) {
						return new ModelResponse("again", ModelFinishReason.TOOL_CALLS,
								List.of(new ToolCall("call-" + requests.size(), "list_files", Map.of("path", "."))));
					}
					return new ModelResponse("done", ModelFinishReason.STOP, List.of());
				}, smallContext, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("SUCCEEDED");
		ModelRequest finalRequest = requests.get(3);
		assertThat(finalRequest.messages()).hasSizeLessThanOrEqualTo(smallContext.maxContextMessages());
		assertThat(finalRequest.messages()).extracting(ModelMessage::role)
				.containsExactly(ModelRole.SYSTEM, ModelRole.USER, ModelRole.ASSISTANT, ModelRole.TOOL);
		assertThat(finalRequest.messages()).extracting(ModelMessage::content).last().asString().contains("tool_call_id=call-3");
		assertToolResultsHaveMatchingAssistantCalls(finalRequest.messages());
	}

	@Test
	void toolTimeoutIsReturnedToModelAsObservation() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		RunBudget timeoutBudget = new RunBudget(4, 12, 30, Duration.ofMillis(20));
		ToolRegistry registry = new ToolRegistry(List.of(new RegisteredTool(
				new ToolDefinition("slow_tool", "Slow tool", Map.of("type", "object")),
				arguments -> {
					try {
						Thread.sleep(Duration.ofSeconds(5));
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					return ToolExecutionResult.of("too late", Map.of());
				})), this.clock);
		SequencedModelClient model = new SequencedModelClient(
				new ModelResponse("slow", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-1", "slow_tool", Map.of()))),
				new ModelResponse("Stopped after timeout observation", ModelFinishReason.STOP, List.of()));
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registry, new ToolApprovalPolicy(),
				model, timeoutBudget, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(store.get(run.id()).status().name()).isEqualTo("SUCCEEDED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.COMPLETED);
		assertThat(store.listEvents(run.id(), -1)).filteredOn(event -> event.type() == RunEventType.TOOL_CALL_FINISHED)
				.singleElement()
				.satisfies(event -> assertThat(event.payload().get("metadata").toString()).contains("TOOL_TIMEOUT"));
		assertThat(model.requests()).hasSize(2);
		assertThat(model.requests().get(1).messages()).filteredOn(message -> message.role() == ModelRole.TOOL)
				.singleElement()
				.satisfies(message -> assertThat(message.content())
						.contains("tool_execution_success=false")
						.contains("\"success\":false")
						.contains("\"timedOut\":true")
						.contains("Tool timed out"));
	}

	@Test
	void cancellingRunBeforeStartSkipsModelAndFinishesCancelled() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		store.transition(run.id(), current -> current.requestCancel(this.clock));
		AtomicBoolean modelCalled = new AtomicBoolean(false);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registryReturningSuccess(), new ToolApprovalPolicy(), request -> {
			modelCalled.set(true);
			return new ModelResponse("done", ModelFinishReason.STOP, List.of());
		}, this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "inspect");

		assertThat(modelCalled).isFalse();
		assertThat(store.get(run.id()).status().name()).isEqualTo("CANCELLED");
		assertThat(store.get(run.id()).stopReason()).isEqualTo(StopReason.USER_CANCELLED);
	}

	@Test
	void mutationToolWaitsForApprovalAndIsNotExecuted() {
		AgentRunStore store = new AgentRunStore();
		AgentRun run = store.create(this.clock);
		AtomicBoolean toolExecuted = new AtomicBoolean(false);
		ToolRegistry registry = new ToolRegistry(List.of(new RegisteredTool(
				new ToolDefinition("write_file", "Write file", Map.of("type", "object")),
				arguments -> {
					toolExecuted.set(true);
					return ToolExecutionResult.of("{}", Map.of());
				})), this.clock);
		MockAgentRunner runner = new MockAgentRunner(store, new RunEventStream(), registry, new ToolApprovalPolicy(),
				request -> new ModelResponse("write", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-1", "write_file", Map.of("path", "x.txt", "content", "x")))),
				this.budget, this.toolExecutor, this.clock);

		runner.run(run.id(), "write a file");

		assertThat(toolExecuted).isFalse();
		assertThat(store.get(run.id()).status()).isEqualTo(RunStatus.WAITING_FOR_APPROVAL);
		assertThat(store.get(run.id()).stopReason()).isNull();
		assertThat(store.listEvents(run.id(), -1)).extracting(RunEvent::type)
				.contains(RunEventType.APPROVAL_REQUIRED)
				.doesNotContain(RunEventType.APPROVAL_RESOLVED, RunEventType.RUN_FINISHED,
						RunEventType.TOOL_CALL_STARTED);
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

	private void assertToolResultsHaveMatchingAssistantCalls(List<ModelMessage> messages) {
		for (int index = 0; index < messages.size(); index++) {
			ModelMessage message = messages.get(index);
			if (message.role() != ModelRole.TOOL) {
				continue;
			}
			assertThat(index).as("tool message must not be first").isGreaterThan(0);
			ModelMessage previous = messages.get(index - 1);
			assertThat(previous.role()).as("tool message must follow assistant tool_calls").isEqualTo(ModelRole.ASSISTANT);
			assertThat(previous.toolCalls()).extracting(ToolCall::id).contains(message.toolCallId());
		}
	}

	private static class SequencedModelClient implements ModelClient {
		private final List<ModelResponse> responses;
		private final List<ModelRequest> requests = new ArrayList<>();
		private int index;

		SequencedModelClient(ModelResponse... responses) {
			this.responses = List.of(responses);
		}

		@Override
		public ModelResponse complete(ModelRequest request) {
			this.requests.add(request);
			ModelResponse response = this.responses.get(Math.min(this.index, this.responses.size() - 1));
			this.index++;
			return response;
		}

		List<ModelRequest> requests() {
			return this.requests;
		}
	}
}
