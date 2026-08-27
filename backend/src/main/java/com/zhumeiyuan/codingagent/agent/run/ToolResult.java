package com.zhumeiyuan.codingagent.agent.run;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record ToolResult(
		String toolCallId,
		boolean success,
		String content,
		Map<String, Object> metadata,
		Instant completedAt) {

	public ToolResult {
		if (toolCallId == null || toolCallId.isBlank()) {
			throw new IllegalArgumentException("Tool call id must not be blank");
		}
		Objects.requireNonNull(content, "content");
		metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
		Objects.requireNonNull(completedAt, "completedAt");
	}

	public static ToolResult success(String toolCallId, String content, Map<String, Object> metadata,
			Instant completedAt) {
		return new ToolResult(toolCallId, true, content, metadata, completedAt);
	}

	public static ToolResult failure(String toolCallId, String message, Map<String, Object> metadata,
			Instant completedAt) {
		return new ToolResult(toolCallId, false, message, metadata, completedAt);
	}
}
