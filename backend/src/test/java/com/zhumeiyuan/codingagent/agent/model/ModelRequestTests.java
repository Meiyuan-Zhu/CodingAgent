package com.zhumeiyuan.codingagent.agent.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;

import org.junit.jupiter.api.Test;

class ModelRequestTests {

	@Test
	void findsLastUserMessage() {
		ModelRequest request = new ModelRequest(List.of(
				ModelMessage.user("first"),
				ModelMessage.assistant("reply"),
				ModelMessage.user("second")),
				List.of());

		assertThat(request.lastUserMessage()).hasValue(ModelMessage.user("second"));
	}

	@Test
	void rejectsEmptyMessages() {
		assertThatThrownBy(() -> new ModelRequest(List.of(),
				List.of(new ToolDefinition("list_files", "List", Map.of("type", "object")))))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
