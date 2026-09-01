package com.zhumeiyuan.codingagent.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"agent.model.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:coding-agent-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class ToolRegistrySpringContextTests {

	@Autowired
	private ToolRegistry toolRegistry;

	@Test
	void springContextRegistersWorkspaceReadTools() {
		assertThat(this.toolRegistry.definitions()).extracting(ToolDefinition::name)
				.contains("list_files", "read_file", "search_text", "write_file", "replace_text", "edit_file",
						"run_command");
	}
}
