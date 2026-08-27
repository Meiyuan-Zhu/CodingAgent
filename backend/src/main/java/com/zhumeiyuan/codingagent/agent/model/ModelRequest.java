package com.zhumeiyuan.codingagent.agent.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;

public record ModelRequest(List<ModelMessage> messages, List<ToolDefinition> tools) {

	public ModelRequest {
		messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
		tools = List.copyOf(Objects.requireNonNull(tools, "tools"));
		if (messages.isEmpty()) {
			throw new IllegalArgumentException("Model request must contain at least one message");
		}
	}

	public Optional<ModelMessage> lastUserMessage() {
		for (int index = this.messages.size() - 1; index >= 0; index--) {
			ModelMessage message = this.messages.get(index);
			if (message.role() == ModelRole.USER) {
				return Optional.of(message);
			}
		}
		return Optional.empty();
	}
}
