package com.zhumeiyuan.codingagent.agent.execution;

import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceChangeUndoResult;

public record WorkspaceChangeUndo(
		String toolCallId,
		WorkspaceChangeUndoState state,
		WorkspaceChangeUndoResult result) {
}
