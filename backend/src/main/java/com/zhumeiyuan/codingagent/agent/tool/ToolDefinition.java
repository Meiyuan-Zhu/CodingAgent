package com.zhumeiyuan.codingagent.agent.tool;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record ToolDefinition(String name, String description, Map<String, Object> inputSchema) {

	private static final Pattern TOOL_NAME = Pattern.compile("[a-z][a-z0-9_]{0,63}");

	public ToolDefinition {
		if (name == null || !TOOL_NAME.matcher(name).matches()) {
			throw new IllegalArgumentException("Tool name must match " + TOOL_NAME.pattern());
		}
		if (description == null || description.isBlank()) {
			throw new IllegalArgumentException("Tool description must not be blank");
		}
		inputSchema = ToolJson.deepCopyObject(Objects.requireNonNull(inputSchema, "inputSchema"));
	}
}
