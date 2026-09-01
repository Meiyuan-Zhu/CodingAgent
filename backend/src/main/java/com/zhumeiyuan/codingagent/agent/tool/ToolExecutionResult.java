package com.zhumeiyuan.codingagent.agent.tool;

import java.util.Map;
import java.util.Objects;

public record ToolExecutionResult(String content, Map<String, Object> metadata, Map<String, Object> privateMetadata) {

	public ToolExecutionResult {
		Objects.requireNonNull(content, "content");
		metadata = ToolJson.deepCopyObject(Objects.requireNonNull(metadata, "metadata"));
		privateMetadata = Map.copyOf(Objects.requireNonNull(privateMetadata, "privateMetadata"));
	}

	public static ToolExecutionResult of(String content, Map<String, Object> metadata) {
		return new ToolExecutionResult(content, metadata, Map.of());
	}

	public static ToolExecutionResult of(String content, Map<String, Object> metadata,
			Map<String, Object> privateMetadata) {
		return new ToolExecutionResult(content, metadata, privateMetadata);
	}
}
