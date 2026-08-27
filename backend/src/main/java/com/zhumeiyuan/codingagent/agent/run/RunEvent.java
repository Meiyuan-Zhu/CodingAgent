package com.zhumeiyuan.codingagent.agent.run;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record RunEvent(
		String eventId,
		RunId runId,
		long sequence,
		Instant occurredAt,
		RunEventType type,
		Map<String, Object> payload) {

	public RunEvent {
		if (eventId == null || eventId.isBlank()) {
			throw new IllegalArgumentException("Event id must not be blank");
		}
		Objects.requireNonNull(runId, "runId");
		if (sequence < 0) {
			throw new IllegalArgumentException("Event sequence must not be negative");
		}
		Objects.requireNonNull(occurredAt, "occurredAt");
		Objects.requireNonNull(type, "type");
		payload = Map.copyOf(Objects.requireNonNull(payload, "payload"));
	}

	public static RunEvent create(RunId runId, long sequence, Instant occurredAt, RunEventType type,
			Map<String, Object> payload) {
		return new RunEvent(UUID.randomUUID().toString(), runId, sequence, occurredAt, type, payload);
	}
}
