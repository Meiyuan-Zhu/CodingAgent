package com.zhumeiyuan.codingagent.agent.execution;

public record RunBudget(int maxRounds, int maxToolCalls, int maxContextMessages) {

	public RunBudget {
		if (maxRounds < 1) {
			throw new IllegalArgumentException("Run budget maxRounds must be positive");
		}
		if (maxToolCalls < 1) {
			throw new IllegalArgumentException("Run budget maxToolCalls must be positive");
		}
		if (maxContextMessages < 2) {
			throw new IllegalArgumentException("Run budget maxContextMessages must be at least 2");
		}
	}

	public static RunBudget defaults() {
		return new RunBudget(4, 12, 30);
	}
}
