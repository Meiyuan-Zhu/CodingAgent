package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunId;

public class AgentRunService {

	private static final int MAX_PROMPT_LENGTH = 4_000;

	private final AgentRunStore store;
	private final RunEventStream runEventStream;
	private final MockAgentRunner mockAgentRunner;
	private final RunTaskManager runTaskManager;
	private final Clock clock;

	public AgentRunService(AgentRunStore store, RunEventStream runEventStream, MockAgentRunner mockAgentRunner,
			RunTaskManager runTaskManager, Clock clock) {
		this.store = Objects.requireNonNull(store, "store");
		this.runEventStream = Objects.requireNonNull(runEventStream, "runEventStream");
		this.mockAgentRunner = Objects.requireNonNull(mockAgentRunner, "mockAgentRunner");
		this.runTaskManager = Objects.requireNonNull(runTaskManager, "runTaskManager");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public AgentRun createRun(String prompt) {
		String normalizedPrompt = validatePrompt(prompt);
		AgentRun run = this.store.create(this.clock);
		emit(run.id(), RunEventType.RUN_CREATED, Map.of("runId", run.id().value()));
		emit(run.id(), RunEventType.USER_MESSAGE_ACCEPTED, Map.of("prompt", normalizedPrompt));
		this.runTaskManager.start(run.id(), () -> this.mockAgentRunner.run(run.id(), normalizedPrompt));
		return getRun(run.id());
	}

	public AgentRun cancelRun(RunId runId) {
		AgentRun cancelling = this.store.transition(runId, run -> {
			if (run.status().isTerminal()) {
				return run;
			}
			return run.requestCancel(this.clock);
		});
		if (cancelling.status().isTerminal()) {
			return cancelling;
		}
		emit(runId, RunEventType.RUN_CANCELLING, Map.of("reason", "user_requested"));
		boolean interruptRequested = this.runTaskManager.cancel(runId);
		return completeCancellation(runId, interruptRequested);
	}

	public AgentRun getRun(RunId runId) {
		return this.store.get(runId);
	}

	public List<RunEvent> listEvents(RunId runId, long afterSequence) {
		return this.store.listEvents(runId, afterSequence);
	}

	public void transition(RunId runId, UnaryOperator<AgentRun> transition) {
		this.store.transition(runId, transition);
	}

	public RunEvent emit(RunId runId, RunEventType type, Map<String, Object> payload) {
		RunEvent event = this.store.appendEvent(runId, type, payload, this.clock);
		this.runEventStream.publish(event, getRun(runId).status().isTerminal());
		return event;
	}

	private AgentRun completeCancellation(RunId runId, boolean interruptRequested) {
		AgentRun current = getRun(runId);
		if (current.status().isTerminal()) {
			return current;
		}
		this.store.transition(runId, run -> run.cancel(this.clock));
		AgentRun cancelled = getRun(runId);
		emit(runId, RunEventType.RUN_FINISHED, Map.of(
				"status", cancelled.status().name(),
				"stopReason", cancelled.stopReason().name(),
				"interruptRequested", interruptRequested));
		return getRun(runId);
	}

	private String validatePrompt(String prompt) {
		if (prompt == null || prompt.isBlank()) {
			throw new IllegalArgumentException("Prompt must not be blank");
		}
		String normalized = prompt.strip();
		if (normalized.length() > MAX_PROMPT_LENGTH) {
			throw new IllegalArgumentException("Prompt must not exceed " + MAX_PROMPT_LENGTH + " characters");
		}
		return normalized;
	}
}
