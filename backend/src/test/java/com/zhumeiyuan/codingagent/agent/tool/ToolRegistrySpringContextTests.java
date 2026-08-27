package com.zhumeiyuan.codingagent.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ToolRegistrySpringContextTests {

	@Autowired
	private ToolRegistry toolRegistry;

	@Test
	void springContextRegistersWorkspaceReadTools() {
		assertThat(this.toolRegistry.definitions()).extracting(ToolDefinition::name)
				.contains("list_files", "read_file", "search_text", "write_file", "replace_text");
	}
}
