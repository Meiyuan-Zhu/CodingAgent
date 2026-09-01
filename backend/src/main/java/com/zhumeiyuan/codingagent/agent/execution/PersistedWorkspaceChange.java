package com.zhumeiyuan.codingagent.agent.execution;

import java.util.Objects;

import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceChangeUndoResult;

record PersistedWorkspaceChange(
		RunId runId,
		String toolCallId,
		WorkspaceChangeUndoSnapshot snapshot,
		WorkspaceChangeUndoState state,
		WorkspaceChangeUndoResult result) {

	PersistedWorkspaceChange {
		Objects.requireNonNull(runId, "runId");
		if (toolCallId == null || toolCallId.isBlank()) {
			throw new IllegalArgumentException("toolCallId must not be blank");
		}
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(state, "state");
	}
}
