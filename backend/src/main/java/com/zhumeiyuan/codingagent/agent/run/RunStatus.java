package com.zhumeiyuan.codingagent.agent.run;

public enum RunStatus {
	CREATED(false),
	RUNNING(false),
	WAITING_FOR_APPROVAL(false),
	CANCELLING(false),
	SUCCEEDED(true),
	FAILED(true),
	CANCELLED(true);

	private final boolean terminal;

	RunStatus(boolean terminal) {
		this.terminal = terminal;
	}

	public boolean isTerminal() {
		return this.terminal;
	}
}
