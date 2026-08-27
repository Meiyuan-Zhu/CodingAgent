package com.zhumeiyuan.codingagent.agent.workspace;

public record TextReplacementResult(
		String path,
		int replacements,
		String previousSha256,
		String sha256,
		long sizeBytes,
		String unifiedDiff) {
}
