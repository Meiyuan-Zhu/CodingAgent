package com.zhumeiyuan.codingagent.agent.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
		"agent.model.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:coding-agent-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
class WorkspaceControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void listsWorkspaceDirectoryForFileInspector() throws Exception {
		MvcResult result = this.mvc.perform(get("/api/workspace/files").queryParam("path", "."))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.root").value("."))
				.andExpect(jsonPath("$.files").isArray())
				.andReturn();

		JsonNode files = this.objectMapper.readTree(result.getResponse().getContentAsString()).get("files");
		assertThat(files).anySatisfy(file -> assertThat(file.get("path").asText()).isEqualTo("README.md"));
	}

	@Test
	void readsWorkspaceFileForFileInspector() throws Exception {
		this.mvc.perform(get("/api/workspace/file").queryParam("path", "README.md"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.path").value("README.md"))
				.andExpect(jsonPath("$.content").isNotEmpty());
	}

	@Test
	void rejectsWorkspaceEscapeForFileInspector() throws Exception {
		this.mvc.perform(get("/api/workspace/file").queryParam("path", "../README.md"))
				.andExpect(status().isBadRequest());
	}
}
