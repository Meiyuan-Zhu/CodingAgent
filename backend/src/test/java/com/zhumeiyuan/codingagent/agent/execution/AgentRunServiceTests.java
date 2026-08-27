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

import com.zhumeiyuan.codingagent.agent.model.ModelFinishReason;
import com.zhumeiyuan.codingagent.agent.model.ModelResponse;
import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunStatus;
import com.zhumeiyuan.codingagent.agent.run.StopReason;
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
		MockAgentRunner runner = new MockAgentRunner(store, stream,
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
	void cancellingTerminalRunIsIdempotent() {
		AgentRunStore store = new AgentRunStore();
		RunEventStream stream = new RunEventStream();
		MockAgentRunner runner = new MockAgentRunner(store, stream,
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

	private void awaitTerminal(AgentRunService service, AgentRun run) {
		for (int index = 0; index < 20 && !service.getRun(run.id()).status().isTerminal(); index++) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
