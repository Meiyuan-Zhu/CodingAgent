package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Duration;
import java.util.Objects;

public record RunBudget(int maxRounds, int maxToolCalls, int maxContextMessages, Duration toolTimeout) {

	public RunBudget(int maxRounds, int maxToolCalls, int maxContextMessages) {
		this(maxRounds, maxToolCalls, maxContextMessages, Duration.ofSeconds(5));
	}

	public RunBudget {
		Objects.requireNonNull(toolTimeout, "toolTimeout");
		if (maxRounds < 1) {
			throw new IllegalArgumentException("Run budget maxRounds must be positive");
		}
		if (maxToolCalls < 1) {
			throw new IllegalArgumentException("Run budget maxToolCalls must be positive");
		}
		if (maxContextMessages < 2) {
			throw new IllegalArgumentException("Run budget maxContextMessages must be at least 2");
		}
		if (toolTimeout.isZero() || toolTimeout.isNegative()) {
			throw new IllegalArgumentException("Run budget toolTimeout must be positive");
		}
	}

	public static RunBudget defaults() {
		return new RunBudget(8, 16, 30, Duration.ofSeconds(5));
	}
}
