package com.zhumeiyuan.codingagent.agent.execution;

import java.util.List;

import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceChangeUndoResult;

interface WorkspaceChangePersistence {

	List<PersistedWorkspaceChange> loadChanges();

	void saveChange(RunId runId, String toolCallId, WorkspaceChangeUndoSnapshot snapshot,
			WorkspaceChangeUndoState state, WorkspaceChangeUndoResult result);
}
