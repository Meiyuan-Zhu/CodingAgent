package com.zhumeiyuan.codingagent.agent.workspace;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record TextReplacementResult(
		boolean success,
		String message,
		String path,
		int replacements,
		String previousSha256,
		String sha256,
		long sizeBytes,
		String unifiedDiff,
		@JsonIgnore String previousContent) {
}
