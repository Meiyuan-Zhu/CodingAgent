package com.zhumeiyuan.codingagent.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.zhumeiyuan.codingagent.agent.model.ModelFinishReason;
import com.zhumeiyuan.codingagent.agent.model.ModelResponse;
import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunStatus;
import com.zhumeiyuan.codingagent.agent.run.StopReason;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionResult;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalPolicy;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgentRunServiceTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T10:30:00Z"), ZoneOffset.UTC);
	private final ExecutorService runExecutor = Executors.newSingleThreadExecutor();
	private final ExecutorService toolExecutor = Executors.newSingleThreadExecutor();

	@AfterEach
	void shutdown() {
		this.runExecutor.shutdownNow();
		this.toolExecutor.shutdownNow();
	}

	@Test
	void cancelRunMovesNonTerminalRunToCancelledAndEmitsEvents() {
		AgentRunStore store = new AgentRunStore();
		RunEventStream stream = new RunEventStream();
		AgentRunner runner = new AgentRunner(store, stream,
				new ToolRegistry(List.of(), this.clock), new ToolApprovalPolicy(), request -> {
					try {
						Thread.sleep(Duration.ofSeconds(10));
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					return new ModelResponse("done", ModelFinishReason.STOP, List.of());
				}, new RunBudget(4, 12, 30), this.toolExecutor, this.clock);
		AgentRunService service = new AgentRunService(store, stream, runner, new RunTaskManager(this.runExecutor), this.clock);

		AgentRun created = service.createRun("long task");
		AgentRun cancelled = service.cancelRun(created.id());

		assertThat(cancelled.status()).isEqualTo(RunStatus.CANCELLED);
		assertThat(cancelled.stopReason()).isEqualTo(StopReason.USER_CANCELLED);
		assertThat(service.listEvents(created.id(), -1)).extracting(event -> event.type())
				.contains(RunEventType.RUN_CANCELLING, RunEventType.RUN_FINISHED);
	}


	@Test
	void approveToolCallResumesRunAndExecutesPendingTool() {
		AgentRunStore store = new AgentRunStore();
		RunEventStream stream = new RunEventStream();
		AtomicBoolean toolExecuted = new AtomicBoolean(false);
		ToolRegistry registry = new ToolRegistry(List.of(new RegisteredTool(
				new ToolDefinition("write_file", "Write file", Map.of("type", "object")),
				arguments -> {
					toolExecuted.set(true);
					return ToolExecutionResult.of("{\"path\":\"x.txt\",\"unifiedDiff\":\"+x\"}",
							Map.of("path", "x.txt"));
				})), this.clock);
		AgentRunner runner = new AgentRunner(store, stream, registry, new ToolApprovalPolicy(), request -> {
			if (request.messages().get(request.messages().size() - 1).role().name().equals("TOOL")) {
				return new ModelResponse("done", ModelFinishReason.STOP, List.of());
			}
			return new ModelResponse("write", ModelFinishReason.TOOL_CALLS,
					List.of(new ToolCall("call-1", "write_file", Map.of("path", "x.txt", "content", "x"))));
		}, new RunBudget(4, 12, 30), this.toolExecutor, this.clock);
		AgentRunService service = new AgentRunService(store, stream, runner, new RunTaskManager(this.runExecutor), this.clock);

		AgentRun created = service.createRun("write a file");
		awaitStatus(service, created, RunStatus.WAITING_FOR_APPROVAL);
		AgentRun approving = service.approveToolCall(created.id(), "call-1");
		awaitTerminal(service, approving);

		assertThat(toolExecuted).isTrue();
		assertThat(service.getRun(created.id()).status()).isEqualTo(RunStatus.SUCCEEDED);
		assertThat(service.listEvents(created.id(), -1)).extracting(event -> event.type())
				.contains(RunEventType.APPROVAL_REQUIRED, RunEventType.APPROVAL_RESOLVED,
						RunEventType.TOOL_CALL_STARTED, RunEventType.TOOL_CALL_FINISHED, RunEventType.RUN_FINISHED);
	}

	@Test
	void rejectToolCallFailsRunWithoutExecutingPendingTool() {
		AgentRunStore store = new AgentRunStore();
		RunEventStream stream = new RunEventStream();
		AtomicBoolean toolExecuted = new AtomicBoolean(false);
		ToolRegistry registry = new ToolRegistry(List.of(new RegisteredTool(
				new ToolDefinition("write_file", "Write file", Map.of("type", "object")),
				arguments -> {
					toolExecuted.set(true);
					return ToolExecutionResult.of("{}", Map.of());
				})), this.clock);
		AgentRunner runner = new AgentRunner(store, stream, registry, new ToolApprovalPolicy(),
				request -> new ModelResponse("write", ModelFinishReason.TOOL_CALLS,
						List.of(new ToolCall("call-1", "write_file", Map.of("path", "x.txt", "content", "x")))),
				new RunBudget(4, 12, 30), this.toolExecutor, this.clock);
		AgentRunService service = new AgentRunService(store, stream, runner, new RunTaskManager(this.runExecutor), this.clock);

		AgentRun created = service.createRun("write a file");
		awaitStatus(service, created, RunStatus.WAITING_FOR_APPROVAL);
		AgentRun rejected = service.rejectToolCall(created.id(), "call-1");

		assertThat(toolExecuted).isFalse();
		assertThat(rejected.status()).isEqualTo(RunStatus.FAILED);
		assertThat(rejected.stopReason()).isEqualTo(StopReason.APPROVAL_REJECTED);
		assertThat(service.listEvents(created.id(), -1)).extracting(event -> event.type())
				.contains(RunEventType.APPROVAL_RESOLVED, RunEventType.RUN_FINISHED)
				.doesNotContain(RunEventType.TOOL_CALL_STARTED);
	}

	@Test
	void cancellingTerminalRunIsIdempotent() {
		AgentRunStore store = new AgentRunStore();
		RunEventStream stream = new RunEventStream();
		AgentRunner runner = new AgentRunner(store, stream,
				new ToolRegistry(List.of(), this.clock), new ToolApprovalPolicy(),
				request -> new ModelResponse("done", ModelFinishReason.STOP, List.of()),
				new RunBudget(4, 12, 30), this.toolExecutor, this.clock);
		AgentRunService service = new AgentRunService(store, stream, runner, new RunTaskManager(this.runExecutor), this.clock);

		AgentRun created = service.createRun("short task");
		awaitTerminal(service, created);
		int eventCount = service.listEvents(created.id(), -1).size();

		AgentRun afterCancel = service.cancelRun(created.id());

		assertThat(afterCancel.status()).isEqualTo(RunStatus.SUCCEEDED);
		assertThat(service.listEvents(created.id(), -1)).hasSize(eventCount);
	}

	private void awaitStatus(AgentRunService service, AgentRun run, RunStatus status) {
		for (int index = 0; index < 40 && service.getRun(run.id()).status() != status; index++) {
			sleepBriefly();
		}
		assertThat(service.getRun(run.id()).status()).isEqualTo(status);
	}

	private void awaitTerminal(AgentRunService service, AgentRun run) {
		for (int index = 0; index < 40 && !service.getRun(run.id()).status().isTerminal(); index++) {
			sleepBriefly();
		}
	}

	private void sleepBriefly() {
		try {
			Thread.sleep(25);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}
