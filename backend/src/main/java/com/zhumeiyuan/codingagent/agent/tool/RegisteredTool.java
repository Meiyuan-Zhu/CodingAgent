package com.zhumeiyuan.codingagent.agent.tool;

import java.util.Objects;

public record RegisteredTool(ToolDefinition definition, ToolHandler handler) {

	public RegisteredTool {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(handler, "handler");
	}
}
