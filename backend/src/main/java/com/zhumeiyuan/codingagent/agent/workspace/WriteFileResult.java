package com.zhumeiyuan.codingagent.agent.workspace;

public record WriteFileResult(
		String path,
		boolean created,
		boolean overwritten,
		String previousSha256,
		String sha256,
		long previousSizeBytes,
		long sizeBytes,
		String unifiedDiff) {
}
