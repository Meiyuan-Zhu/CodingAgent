package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
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
	private final Executor executor;
	private final Clock clock;

	public AgentRunService(AgentRunStore store, RunEventStream runEventStream, MockAgentRunner mockAgentRunner,
			Executor executor, Clock clock) {
		this.store = Objects.requireNonNull(store, "store");
		this.runEventStream = Objects.requireNonNull(runEventStream, "runEventStream");
		this.mockAgentRunner = Objects.requireNonNull(mockAgentRunner, "mockAgentRunner");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public AgentRun createRun(String prompt) {
		String normalizedPrompt = validatePrompt(prompt);
		AgentRun run = this.store.create(this.clock);
		emit(run.id(), RunEventType.RUN_CREATED, Map.of("runId", run.id().value()));
		emit(run.id(), RunEventType.USER_MESSAGE_ACCEPTED, Map.of("prompt", normalizedPrompt));
		this.executor.execute(() -> this.mockAgentRunner.run(run.id(), normalizedPrompt));
		return getRun(run.id());
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
