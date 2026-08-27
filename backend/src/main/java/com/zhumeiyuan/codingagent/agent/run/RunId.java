package com.zhumeiyuan.codingagent.agent.run;

import java.util.Objects;
import java.util.UUID;

public record RunId(String value) {

	public RunId {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Run id must not be blank");
		}
	}

	public static RunId newId() {
		return new RunId(UUID.randomUUID().toString());
	}

	@Override
	public String toString() {
		return this.value;
	}

	public static RunId from(String value) {
		return new RunId(Objects.requireNonNull(value, "value"));
	}
}
