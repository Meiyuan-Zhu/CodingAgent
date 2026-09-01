package com.zhumeiyuan.codingagent.agent.workspace;

public record WorkspaceChangeUndoResult(
		String path,
		boolean deleted,
		boolean restored,
		String previousSha256,
		String sha256,
		String unifiedDiff) {
}
