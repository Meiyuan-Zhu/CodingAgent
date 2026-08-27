package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventEnvelope;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunId;

public class AgentRunStore {

	private final Map<RunId, StoredRun> runs = new ConcurrentHashMap<>();

	public AgentRun create(Clock clock) {
		AgentRun run = AgentRun.create(clock);
		StoredRun previous = this.runs.putIfAbsent(run.id(), new StoredRun(run));
		if (previous != null) {
			throw new IllegalStateException("Duplicate run id generated: " + run.id());
		}
		return run;
	}

	public AgentRun get(RunId runId) {
		return stored(runId).snapshot();
	}

	public AgentRun transition(RunId runId, UnaryOperator<AgentRun> transition) {
		return stored(runId).transition(transition);
	}

	public RunEvent appendEvent(RunId runId, RunEventType type, Map<String, Object> payload, Clock clock) {
		return stored(runId).appendEvent(type, payload, clock);
	}

	public List<RunEvent> listEvents(RunId runId, long afterSequence) {
		return stored(runId).eventsAfter(afterSequence);
	}

	private StoredRun stored(RunId runId) {
		Objects.requireNonNull(runId, "runId");
		StoredRun storedRun = this.runs.get(runId);
		if (storedRun == null) {
			throw new AgentRunNotFoundException(runId);
		}
		return storedRun;
	}

	private static final class StoredRun {

		private AgentRun run;
		private final List<RunEvent> events = new ArrayList<>();

		private StoredRun(AgentRun run) {
			this.run = Objects.requireNonNull(run, "run");
		}

		private synchronized AgentRun snapshot() {
			return this.run;
		}

		private synchronized AgentRun transition(UnaryOperator<AgentRun> transition) {
			this.run = Objects.requireNonNull(transition, "transition").apply(this.run);
			return this.run;
		}

		private synchronized RunEvent appendEvent(RunEventType type, Map<String, Object> payload, Clock clock) {
			RunEventEnvelope envelope = this.run.event(type, payload, clock);
			this.run = envelope.run();
			this.events.add(envelope.event());
			return envelope.event();
		}

		private synchronized List<RunEvent> eventsAfter(long afterSequence) {
			return this.events.stream()
					.filter(event -> event.sequence() > afterSequence)
					.toList();
		}
	}
}
