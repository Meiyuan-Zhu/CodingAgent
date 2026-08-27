package com.zhumeiyuan.codingagent.agent.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class RunControllerTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createRunReturnsAcceptedRunAndEventsCanBeReplayed() throws Exception {
		MvcResult createResult = this.mvc.perform(post("/api/runs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"prompt\":\"list workspace files\"}"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andReturn();
		String runId = this.objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

		this.mvc.perform(get("/api/runs/{runId}", runId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(runId));

		MvcResult eventsResult = this.mvc.perform(get("/api/runs/{runId}/events", runId))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode events = this.objectMapper.readTree(eventsResult.getResponse().getContentAsString());
		assertThat(events).isNotEmpty();
		assertThat(events.get(0).get("type").asText()).isEqualTo("RUN_CREATED");
	}

	@Test
	void createRunRejectsBlankPrompt() throws Exception {
		this.mvc.perform(post("/api/runs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"prompt\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("bad_request"));
	}

	@Test
	void missingRunReturnsNotFound() throws Exception {
		this.mvc.perform(get("/api/runs/{runId}", "missing"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("run_not_found"));
	}

	@Test
	void eventStreamEndpointStartsAsyncResponse() throws Exception {
		MvcResult createResult = this.mvc.perform(post("/api/runs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"prompt\":\"list workspace files\"}"))
				.andExpect(status().isAccepted())
				.andReturn();
		String runId = this.objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

		this.mvc.perform(get("/api/runs/{runId}/events/stream", runId).accept(MediaType.TEXT_EVENT_STREAM))
				.andExpect(request().asyncStarted());
	}
}
