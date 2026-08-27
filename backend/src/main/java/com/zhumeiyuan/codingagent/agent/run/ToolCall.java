package com.zhumeiyuan.codingagent.agent.run;

import java.util.Map;
import java.util.Objects;

public record ToolCall(String id, String name, Map<String, Object> arguments) {

	public ToolCall {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("Tool call id must not be blank");
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Tool name must not be blank");
		}
		arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments"));
	}
}
