package com.zhumeiyuan.codingagent.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunId;

import org.junit.jupiter.api.Test;

class AgentRunStoreTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T09:00:00Z"), ZoneOffset.UTC);
	private final AgentRunStore store = new AgentRunStore();

	@Test
	void createsRunAndAppendsOrderedEvents() {
		AgentRun run = this.store.create(this.clock);

		RunEvent first = this.store.appendEvent(run.id(), RunEventType.RUN_CREATED, Map.of("runId", run.id().value()),
				this.clock);
		RunEvent second = this.store.appendEvent(run.id(), RunEventType.USER_MESSAGE_ACCEPTED, Map.of("prompt", "hi"),
				this.clock);

		assertThat(first.sequence()).isZero();
		assertThat(second.sequence()).isEqualTo(1);
		assertThat(this.store.get(run.id()).nextSequence()).isEqualTo(2);
		assertThat(this.store.listEvents(run.id(), 0)).containsExactly(second);
	}

	@Test
	void transitionsRunState() {
		AgentRun run = this.store.create(this.clock);

		AgentRun updated = this.store.transition(run.id(), current -> current.start(this.clock));

		assertThat(updated.status().name()).isEqualTo("RUNNING");
		assertThat(this.store.get(run.id()).status()).isEqualTo(updated.status());
	}

	@Test
	void missingRunThrowsNotFound() {
		RunId missing = RunId.from("missing");

		assertThatThrownBy(() -> this.store.get(missing))
				.isInstanceOf(AgentRunNotFoundException.class)
				.hasMessageContaining("missing");
	}
}
