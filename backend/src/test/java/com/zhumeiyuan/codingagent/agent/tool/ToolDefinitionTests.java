package com.zhumeiyuan.codingagent.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ToolDefinitionTests {

	@Test
	void validatesToolNameAndDescription() {
		assertThatThrownBy(() -> new ToolDefinition("ReadFile", "read", Map.of("type", "object")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ToolDefinition("read_file", " ", Map.of("type", "object")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void deepCopiesNestedSchema() {
		Map<String, Object> schema = Map.of(
				"type", "object",
				"required", List.of("path"),
				"properties", Map.of("path", Map.of("type", "string")));

		ToolDefinition definition = new ToolDefinition("read_file", "Read a file", schema);

		assertThat(definition.inputSchema()).containsEntry("type", "object");
		Map<String, Object> properties = (Map<String, Object>) definition.inputSchema().get("properties");
		assertThat(properties).containsKey("path");
		assertThatThrownBy(() -> properties.put("other", Map.of())).isInstanceOf(UnsupportedOperationException.class);
	}
}
