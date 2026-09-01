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
	private final AgentRunPersistence persistence;

	public AgentRunStore() {
		this(NoOpAgentRunPersistence.INSTANCE);
	}

	AgentRunStore(AgentRunPersistence persistence) {
		this.persistence = Objects.requireNonNull(persistence, "persistence");
		for (PersistedRun persisted : this.persistence.loadRuns()) {
			this.runs.put(persisted.run().id(), StoredRun.from(persisted));
		}
	}

	public AgentRun create(Clock clock) {
		AgentRun run = AgentRun.create(clock);
		StoredRun previous = this.runs.putIfAbsent(run.id(), new StoredRun(run));
		if (previous != null) {
			throw new IllegalStateException("Duplicate run id generated: " + run.id());
		}
		this.persistence.saveRun(run);
		return run;
	}

	public AgentRun get(RunId runId) {
		return stored(runId).snapshot();
	}

	public List<AgentRun> listRuns() {
		return this.runs.values().stream()
				.map(StoredRun::snapshot)
				.sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
				.toList();
	}

	public AgentRun transition(RunId runId, UnaryOperator<AgentRun> transition) {
		AgentRun run = stored(runId).transition(transition);
		this.persistence.saveRun(run);
		return run;
	}

	public RunEvent appendEvent(RunId runId, RunEventType type, Map<String, Object> payload, Clock clock) {
		StoredRun storedRun = stored(runId);
		RunEvent event = storedRun.appendEvent(type, payload, clock);
		this.persistence.saveRun(storedRun.snapshot());
		this.persistence.insertEvent(event);
		return event;
	}

	public void savePendingApproval(PendingToolApproval approval) {
		Objects.requireNonNull(approval, "approval");
		stored(approval.runId()).savePendingApproval(approval);
		this.persistence.savePendingApproval(approval);
	}

	public PendingToolApproval consumePendingApproval(RunId runId, String toolCallId) {
		PendingToolApproval approval = stored(runId).consumePendingApproval(toolCallId);
		this.persistence.deletePendingApproval(runId);
		return approval;
	}

	public void clearPendingApproval(RunId runId) {
		stored(runId).clearPendingApproval();
		this.persistence.deletePendingApproval(runId);
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
		private PendingToolApproval pendingApproval;
		private final List<RunEvent> events = new ArrayList<>();

		private StoredRun(AgentRun run) {
			this.run = Objects.requireNonNull(run, "run");
		}

		private static StoredRun from(PersistedRun persisted) {
			StoredRun storedRun = new StoredRun(persisted.run());
			storedRun.events.addAll(persisted.events());
			storedRun.pendingApproval = persisted.pendingApproval();
			return storedRun;
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

		private synchronized void savePendingApproval(PendingToolApproval approval) {
			if (this.pendingApproval != null) {
				throw new IllegalStateException("Run already has a pending approval");
			}
			this.pendingApproval = approval;
		}

		private synchronized PendingToolApproval consumePendingApproval(String toolCallId) {
			Objects.requireNonNull(toolCallId, "toolCallId");
			if (this.pendingApproval == null) {
				throw new IllegalArgumentException("Run has no pending approval");
			}
			if (!this.pendingApproval.toolCall().id().equals(toolCallId)) {
				throw new IllegalArgumentException("Pending approval is for tool call "
						+ this.pendingApproval.toolCall().id());
			}
			PendingToolApproval approval = this.pendingApproval;
			this.pendingApproval = null;
			return approval;
		}

		private synchronized void clearPendingApproval() {
			this.pendingApproval = null;
		}

		private synchronized List<RunEvent> eventsAfter(long afterSequence) {
			return this.events.stream()
					.filter(event -> event.sequence() > afterSequence)
					.toList();
		}
	}
}
