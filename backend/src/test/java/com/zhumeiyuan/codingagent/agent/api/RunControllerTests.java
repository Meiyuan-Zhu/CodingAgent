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

@SpringBootTest(properties = {
		"agent.model.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:coding-agent-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
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
	void listRunsReturnsCreatedRuns() throws Exception {
		MvcResult createResult = this.mvc.perform(post("/api/runs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"prompt\":\"list workspace files\"}"))
				.andExpect(status().isAccepted())
				.andReturn();
		String runId = this.objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

		MvcResult listResult = this.mvc.perform(get("/api/runs"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode runs = this.objectMapper.readTree(listResult.getResponse().getContentAsString());

		assertThat(runs).anySatisfy(run -> assertThat(run.get("id").asText()).isEqualTo(runId));
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
	void cancelRunEndpointReturnsRunState() throws Exception {
		MvcResult createResult = this.mvc.perform(post("/api/runs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"prompt\":\"list workspace files\"}"))
				.andExpect(status().isAccepted())
				.andReturn();
		String runId = this.objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

		this.mvc.perform(post("/api/runs/{runId}/cancel", runId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(runId));
	}


	@Test
	void rejectApprovalEndpointReturnsFailedRun() throws Exception {
		MvcResult createResult = this.mvc.perform(post("/api/runs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"prompt\":\"please write a note\"}"))
				.andExpect(status().isAccepted())
				.andReturn();
		String runId = this.objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
		waitForStatus(runId, "WAITING_FOR_APPROVAL");

		this.mvc.perform(post("/api/runs/{runId}/approvals/{toolCallId}/reject", runId, "mock-call-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("FAILED"))
				.andExpect(jsonPath("$.stopReason").value("APPROVAL_REJECTED"));
	}

	@Test
	void undoWorkspaceChangeEndpointRevertsApprovedWrite() throws Exception {
		MvcResult createResult = this.mvc.perform(post("/api/runs")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"prompt\":\"please write a note\"}"))
				.andExpect(status().isAccepted())
				.andReturn();
		String runId = this.objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
		waitForStatus(runId, "WAITING_FOR_APPROVAL");

		this.mvc.perform(post("/api/runs/{runId}/approvals/{toolCallId}/approve", runId, "mock-call-1"))
				.andExpect(status().isOk());
		waitForStatus(runId, "SUCCEEDED");

		this.mvc.perform(post("/api/runs/{runId}/changes/{toolCallId}/undo", runId, "mock-call-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.toolCallId").value("mock-call-1"))
				.andExpect(jsonPath("$.state").value("UNDONE"))
				.andExpect(jsonPath("$.deleted").value(true));

		MvcResult eventsResult = this.mvc.perform(get("/api/runs/{runId}/events", runId))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode events = this.objectMapper.readTree(eventsResult.getResponse().getContentAsString());
		assertThat(events.toString()).contains("CHANGE_UNDONE");
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

	private void waitForStatus(String runId, String expectedStatus) throws Exception {
		for (int index = 0; index < 40; index++) {
			MvcResult result = this.mvc.perform(get("/api/runs/{runId}", runId))
					.andExpect(status().isOk())
					.andReturn();
			String status = this.objectMapper.readTree(result.getResponse().getContentAsString()).get("status").asText();
			if (expectedStatus.equals(status)) {
				return;
			}
			Thread.sleep(25);
		}
		throw new AssertionError("Run did not reach status " + expectedStatus);
	}
}
