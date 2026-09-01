package com.zhumeiyuan.codingagent.agent.workspace;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record WriteFileResult(
		boolean success,
		String message,
		String path,
		boolean created,
		boolean overwritten,
		String previousSha256,
		String sha256,
		long previousSizeBytes,
		long sizeBytes,
		String unifiedDiff,
		@JsonIgnore String previousContent) {
}
