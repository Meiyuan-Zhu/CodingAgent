package com.zhumeiyuan.codingagent.agent.run;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record AgentRun(
		RunId id,
		RunStatus status,
		Instant createdAt,
		Instant updatedAt,
		long nextSequence,
		StopReason stopReason,
		String errorMessage) {

	public AgentRun {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(createdAt, "createdAt");
		Objects.requireNonNull(updatedAt, "updatedAt");
		if (updatedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("Run updatedAt must not be before createdAt");
		}
		if (nextSequence < 0) {
			throw new IllegalArgumentException("Run next sequence must not be negative");
		}
		if (status.isTerminal() && stopReason == null) {
			throw new IllegalArgumentException("Terminal runs must have a stop reason");
		}
		if (!status.isTerminal() && stopReason != null) {
			throw new IllegalArgumentException("Non-terminal runs must not have a stop reason");
		}
	}

	public static AgentRun create(Clock clock) {
		Instant now = clock.instant();
		return new AgentRun(RunId.newId(), RunStatus.CREATED, now, now, 0, null, null);
	}

	public RunEventEnvelope event(RunEventType type, Map<String, Object> payload, Clock clock) {
		RunEvent event = RunEvent.create(this.id, this.nextSequence, clock.instant(), type, payload);
		return new RunEventEnvelope(this.withNextSequence(), event);
	}

	public AgentRun start(Clock clock) {
		requireStatus(RunStatus.CREATED);
		return withStatus(RunStatus.RUNNING, clock.instant());
	}

	public AgentRun waitForApproval(Clock clock) {
		requireStatus(RunStatus.RUNNING);
		return withStatus(RunStatus.WAITING_FOR_APPROVAL, clock.instant());
	}

	public AgentRun resumeAfterApproval(Clock clock) {
		requireStatus(RunStatus.WAITING_FOR_APPROVAL);
		return withStatus(RunStatus.RUNNING, clock.instant());
	}

	public AgentRun requestCancel(Clock clock) {
		if (this.status.isTerminal()) {
			throw new IllegalStateException("Terminal run cannot be cancelled");
		}
		return withStatus(RunStatus.CANCELLING, clock.instant());
	}

	public AgentRun succeed(Clock clock) {
		requireNonTerminal();
		return finish(RunStatus.SUCCEEDED, StopReason.COMPLETED, null, clock.instant());
	}

	public AgentRun cancel(Clock clock) {
		requireNonTerminal();
		return finish(RunStatus.CANCELLED, StopReason.USER_CANCELLED, null, clock.instant());
	}

	public AgentRun fail(StopReason stopReason, String errorMessage, Clock clock) {
		requireNonTerminal();
		if (stopReason == StopReason.COMPLETED || stopReason == StopReason.USER_CANCELLED) {
			throw new IllegalArgumentException("Failure runs need a failure stop reason");
		}
		if (errorMessage == null || errorMessage.isBlank()) {
			throw new IllegalArgumentException("Failure runs need an error message");
		}
		return finish(RunStatus.FAILED, stopReason, errorMessage, clock.instant());
	}

	private AgentRun withNextSequence() {
		return new AgentRun(this.id, this.status, this.createdAt, this.updatedAt, this.nextSequence + 1,
				this.stopReason, this.errorMessage);
	}

	private AgentRun withStatus(RunStatus status, Instant updatedAt) {
		return new AgentRun(this.id, status, this.createdAt, updatedAt, this.nextSequence, null, null);
	}

	private AgentRun finish(RunStatus status, StopReason stopReason, String errorMessage, Instant updatedAt) {
		return new AgentRun(this.id, status, this.createdAt, updatedAt, this.nextSequence, stopReason, errorMessage);
	}

	private void requireStatus(RunStatus expected) {
		if (this.status != expected) {
			throw new IllegalStateException("Expected run status " + expected + " but was " + this.status);
		}
	}

	private void requireNonTerminal() {
		if (this.status.isTerminal()) {
			throw new IllegalStateException("Terminal run cannot transition");
		}
	}
}
