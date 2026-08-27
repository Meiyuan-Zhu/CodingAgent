package com.zhumeiyuan.codingagent.agent.tool;

import java.util.Map;
import java.util.Objects;

public record ToolExecutionResult(String content, Map<String, Object> metadata) {

	public ToolExecutionResult {
		Objects.requireNonNull(content, "content");
		metadata = ToolJson.deepCopyObject(Objects.requireNonNull(metadata, "metadata"));
	}

	public static ToolExecutionResult of(String content, Map<String, Object> metadata) {
		return new ToolExecutionResult(content, metadata);
	}
}
