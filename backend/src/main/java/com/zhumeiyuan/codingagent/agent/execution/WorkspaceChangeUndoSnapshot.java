package com.zhumeiyuan.codingagent.agent.execution;

public record WorkspaceChangeUndoSnapshot(
		String path,
		boolean created,
		String previousContent,
		String expectedCurrentSha256) {
}
