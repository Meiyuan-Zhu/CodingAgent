package com.zhumeiyuan.codingagent.agent.execution;

import java.util.List;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunId;

final class NoOpAgentRunPersistence implements AgentRunPersistence {

	static final NoOpAgentRunPersistence INSTANCE = new NoOpAgentRunPersistence();

	private NoOpAgentRunPersistence() {
	}

	@Override
	public List<PersistedRun> loadRuns() {
		return List.of();
	}

	@Override
	public void saveRun(AgentRun run) {
	}

	@Override
	public void insertEvent(RunEvent event) {
	}

	@Override
	public void savePendingApproval(PendingToolApproval approval) {
	}

	@Override
	public void deletePendingApproval(RunId runId) {
	}
}
