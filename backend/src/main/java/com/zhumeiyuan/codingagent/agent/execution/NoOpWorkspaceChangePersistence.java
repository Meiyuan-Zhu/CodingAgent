package com.zhumeiyuan.codingagent.agent.execution;

import java.util.List;

import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceChangeUndoResult;

final class NoOpWorkspaceChangePersistence implements WorkspaceChangePersistence {

	static final NoOpWorkspaceChangePersistence INSTANCE = new NoOpWorkspaceChangePersistence();

	private NoOpWorkspaceChangePersistence() {
	}

	@Override
	public List<PersistedWorkspaceChange> loadChanges() {
		return List.of();
	}

	@Override
	public void saveChange(RunId runId, String toolCallId, WorkspaceChangeUndoSnapshot snapshot,
			WorkspaceChangeUndoState state, WorkspaceChangeUndoResult result) {
	}
}
