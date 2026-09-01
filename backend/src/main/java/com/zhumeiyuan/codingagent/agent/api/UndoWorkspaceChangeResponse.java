package com.zhumeiyuan.codingagent.agent.api;

import com.zhumeiyuan.codingagent.agent.execution.WorkspaceChangeUndo;

record UndoWorkspaceChangeResponse(
		String toolCallId,
		String state,
		String path,
		boolean deleted,
		boolean restored) {

	static UndoWorkspaceChangeResponse from(WorkspaceChangeUndo undo) {
		return new UndoWorkspaceChangeResponse(undo.toolCallId(), undo.state().name(), undo.result().path(),
				undo.result().deleted(), undo.result().restored());
	}
}
