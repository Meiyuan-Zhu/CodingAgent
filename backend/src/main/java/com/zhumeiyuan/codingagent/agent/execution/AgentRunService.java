package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunStatus;
import com.zhumeiyuan.codingagent.agent.run.StopReason;
import com.zhumeiyuan.codingagent.agent.run.RunId;

public class AgentRunService {

	private static final int MAX_PROMPT_LENGTH = 4_000;

	private final AgentRunStore store;
	private final RunEventStream runEventStream;
	private final MockAgentRunner mockAgentRunner;
	private final RunTaskManager runTaskManager;
	private final WorkspaceChangeJournal workspaceChangeJournal;
	private final Clock clock;

	public AgentRunService(AgentRunStore store, RunEventStream runEventStream, MockAgentRunner mockAgentRunner,
			RunTaskManager runTaskManager, WorkspaceChangeJournal workspaceChangeJournal, Clock clock) {
		this.store = Objects.requireNonNull(store, "store");
		this.runEventStream = Objects.requireNonNull(runEventStream, "runEventStream");
		this.mockAgentRunner = Objects.requireNonNull(mockAgentRunner, "mockAgentRunner");
		this.runTaskManager = Objects.requireNonNull(runTaskManager, "runTaskManager");
		this.workspaceChangeJournal = Objects.requireNonNull(workspaceChangeJournal, "workspaceChangeJournal");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public AgentRunService(AgentRunStore store, RunEventStream runEventStream, MockAgentRunner mockAgentRunner,
			RunTaskManager runTaskManager, Clock clock) {
		this.store = Objects.requireNonNull(store, "store");
		this.runEventStream = Objects.requireNonNull(runEventStream, "runEventStream");
		this.mockAgentRunner = Objects.requireNonNull(mockAgentRunner, "mockAgentRunner");
		this.runTaskManager = Objects.requireNonNull(runTaskManager, "runTaskManager");
		this.workspaceChangeJournal = null;
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

	public List<AgentRun> listRuns() {
		return this.store.listRuns();
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
		this.store.clearPendingApproval(runId);
		return completeCancellation(runId, interruptRequested);
	}

	public void deleteRun(RunId runId) {
		AgentRun run = getRun(runId);
		if (!run.status().isTerminal()) {
			this.runTaskManager.cancel(runId);
		}
		this.store.delete(runId);
	}

	public AgentRun approveToolCall(RunId runId, String toolCallId) {
		PendingToolApproval approval = consumeWaitingApproval(runId, toolCallId);
		this.store.transition(runId, run -> run.resumeAfterApproval(this.clock));
		emit(runId, RunEventType.APPROVAL_RESOLVED, Map.of(
				"round", approval.round(),
				"toolCallId", approval.toolCall().id(),
				"name", approval.toolCall().name(),
				"approved", true,
				"reason", "user_approved"));
		this.runTaskManager.start(runId, () -> this.mockAgentRunner.resumeAfterApproval(approval));
		return getRun(runId);
	}

	public AgentRun rejectToolCall(RunId runId, String toolCallId) {
		PendingToolApproval approval = consumeWaitingApproval(runId, toolCallId);
		emit(runId, RunEventType.APPROVAL_RESOLVED, Map.of(
				"round", approval.round(),
				"toolCallId", approval.toolCall().id(),
				"name", approval.toolCall().name(),
				"approved", false,
				"reason", "user_rejected"));
		this.store.transition(runId, run -> run.fail(StopReason.APPROVAL_REJECTED,
				"User rejected tool call: " + approval.toolCall().name(), this.clock));
		AgentRun rejected = getRun(runId);
		emit(runId, RunEventType.RUN_FINISHED, Map.of(
				"status", rejected.status().name(),
				"stopReason", rejected.stopReason().name(),
				"errorMessage", rejected.errorMessage()));
		return getRun(runId);
	}

	public WorkspaceChangeUndo undoWorkspaceChange(RunId runId, String toolCallId) {
		if (this.workspaceChangeJournal == null) {
			throw new IllegalStateException("Workspace change undo is not configured");
		}
		WorkspaceChangeUndo undo = this.workspaceChangeJournal.undo(runId, toolCallId);
		emit(runId, RunEventType.CHANGE_UNDONE, Map.of(
				"toolCallId", undo.toolCallId(),
				"state", undo.state().name(),
				"path", undo.result().path(),
				"deleted", undo.result().deleted(),
				"restored", undo.result().restored(),
				"previousSha256", nullSafe(undo.result().previousSha256()),
				"sha256", nullSafe(undo.result().sha256())));
		return undo;
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

	private PendingToolApproval consumeWaitingApproval(RunId runId, String toolCallId) {
		AgentRun run = getRun(runId);
		if (run.status() != RunStatus.WAITING_FOR_APPROVAL) {
			throw new IllegalArgumentException("Run is not waiting for approval");
		}
		return this.store.consumePendingApproval(runId, toolCallId);
	}

	private AgentRun completeCancellation(RunId runId, boolean interruptRequested) {
		AgentRun cancelled = this.store.transition(runId, run -> run.status().isTerminal() ? run : run.cancel(this.clock));
		if (cancelled.status() != RunStatus.CANCELLED) {
			return cancelled;
		}
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

	private String nullSafe(String value) {
		return value == null ? "" : value;
	}
}
