package com.zhumeiyuan.codingagent.agent.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AgentRunTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T06:00:00Z"), ZoneOffset.UTC);

	@Test
	void runMovesThroughHappyPath() {
		AgentRun created = AgentRun.create(this.clock);
		AgentRun running = created.start(this.clock);
		RunEventEnvelope envelope = running.event(RunEventType.USER_MESSAGE_ACCEPTED, Map.of("content", "hello"),
				this.clock);
		AgentRun succeeded = envelope.run().succeed(this.clock);

		assertThat(created.status()).isEqualTo(RunStatus.CREATED);
		assertThat(running.status()).isEqualTo(RunStatus.RUNNING);
		assertThat(envelope.event().sequence()).isZero();
		assertThat(envelope.run().nextSequence()).isEqualTo(1);
		assertThat(succeeded.status()).isEqualTo(RunStatus.SUCCEEDED);
		assertThat(succeeded.stopReason()).isEqualTo(StopReason.COMPLETED);
	}

	@Test
	void approvalTransitionsAreExplicit() {
		AgentRun running = AgentRun.create(this.clock).start(this.clock);
		AgentRun waiting = running.waitForApproval(this.clock);
		AgentRun resumed = waiting.resumeAfterApproval(this.clock);

		assertThat(waiting.status()).isEqualTo(RunStatus.WAITING_FOR_APPROVAL);
		assertThat(resumed.status()).isEqualTo(RunStatus.RUNNING);
	}

	@Test
	void invalidTransitionsAreRejected() {
		AgentRun created = AgentRun.create(this.clock);

		assertThatThrownBy(() -> created.resumeAfterApproval(this.clock)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> created.succeed(this.clock).start(this.clock)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void failedRunsNeedFailureReasonAndMessage() {
		AgentRun running = AgentRun.create(this.clock).start(this.clock);

		assertThatThrownBy(() -> running.fail(StopReason.COMPLETED, "done", this.clock))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> running.fail(StopReason.TOOL_ERROR, "", this.clock))
				.isInstanceOf(IllegalArgumentException.class);

		AgentRun failed = running.fail(StopReason.TOOL_ERROR, "read_file failed", this.clock);
		assertThat(failed.status()).isEqualTo(RunStatus.FAILED);
		assertThat(failed.errorMessage()).isEqualTo("read_file failed");
	}

	@Test
	void terminalRunMustHaveStopReason() {
		assertThatThrownBy(() -> new AgentRun(RunId.newId(), RunStatus.SUCCEEDED, this.clock.instant(),
				this.clock.instant(), 0, null, null)).isInstanceOf(IllegalArgumentException.class);
	}
}
