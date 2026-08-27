package com.zhumeiyuan.codingagent.agent.execution;

import com.zhumeiyuan.codingagent.agent.run.RunId;

public class AgentRunNotFoundException extends RuntimeException {

	private final RunId runId;

	public AgentRunNotFoundException(RunId runId) {
		super("Agent run not found: " + runId);
		this.runId = runId;
	}

	public RunId runId() {
		return this.runId;
	}
}
