package com.zhumeiyuan.codingagent.agent.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RunEventTests {

	@Test
	void eventPayloadIsImmutableSnapshot() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("path", "README.md");

		RunEvent event = RunEvent.create(RunId.newId(), 0, Instant.parse("2026-08-27T06:00:00Z"),
				RunEventType.TOOL_CALL_STARTED, payload);
		payload.put("path", "changed.md");

		assertThat(event.payload()).containsEntry("path", "README.md");
		assertThatThrownBy(() -> event.payload().put("other", "value")).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void eventSequenceMustNotBeNegative() {
		assertThatThrownBy(() -> RunEvent.create(RunId.newId(), -1, Instant.parse("2026-08-27T06:00:00Z"),
				RunEventType.RUN_CREATED, Map.of())).isInstanceOf(IllegalArgumentException.class);
	}
}
