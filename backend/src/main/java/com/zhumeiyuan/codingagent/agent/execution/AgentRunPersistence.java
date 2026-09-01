package com.zhumeiyuan.codingagent.agent.execution;

import java.util.List;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunId;

interface AgentRunPersistence {

	List<PersistedRun> loadRuns();

	void saveRun(AgentRun run);

	void insertEvent(RunEvent event);

	void savePendingApproval(PendingToolApproval approval);

	void deletePendingApproval(RunId runId);
}
