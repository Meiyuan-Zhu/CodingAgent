package com.zhumeiyuan.codingagent.agent.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
		"agent.model.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:coding-agent-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkspaceControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@Order(1)
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
	@Order(2)
	void readsWorkspaceFileForFileInspector() throws Exception {
		this.mvc.perform(get("/api/workspace/file").queryParam("path", "README.md"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.path").value("README.md"))
				.andExpect(jsonPath("$.content").isNotEmpty());
	}

	@Test
	@Order(3)
	void rejectsWorkspaceEscapeForFileInspector() throws Exception {
		this.mvc.perform(get("/api/workspace/file").queryParam("path", "../README.md"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(4)
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void addsAndSelectsLocalProjectDirectory() throws Exception {
		Path project = Files.createTempDirectory("coding-agent-project-");
		Files.writeString(project.resolve("PROJECT.md"), "new project\n");

		MvcResult addResult = this.mvc.perform(post("/api/workspace/projects")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"path\":\"" + project.toString().replace("\\", "\\\\") + "\",\"create\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true))
				.andReturn();
		String projectId = this.objectMapper.readTree(addResult.getResponse().getContentAsString()).get("id").asText();

		this.mvc.perform(post("/api/workspace/projects/{projectId}/select", projectId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(true));

		MvcResult filesResult = this.mvc.perform(get("/api/workspace/files").queryParam("path", "."))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode files = this.objectMapper.readTree(filesResult.getResponse().getContentAsString()).get("files");
		assertThat(files).anySatisfy(file -> assertThat(file.get("path").asText()).isEqualTo("PROJECT.md"));
	}
}
