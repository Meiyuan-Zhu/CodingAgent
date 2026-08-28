package com.zhumeiyuan.codingagent.agent.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;

import org.junit.jupiter.api.Test;

class HeuristicMockModelClientTests {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final HeuristicMockModelClient client = new HeuristicMockModelClient(
			new ModelResponseParser(this.objectMapper), this.objectMapper);

	@Test
	void choosesReadmeToolForReadmePrompt() {
		ModelResponse response = this.client.complete(request("please inspect README"));

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.TOOL_CALLS);
		assertThat(response.toolCalls()).singleElement().satisfies(call -> {
			assertThat(call.name()).isEqualTo("read_file");
			assertThat(call.arguments()).containsEntry("path", "README.md");
		});
	}

	@Test
	void choosesSearchToolForSearchPrompt() {
		ModelResponse response = this.client.complete(request("search the workspace"));

		assertThat(response.toolCalls()).singleElement().satisfies(call -> {
			assertThat(call.name()).isEqualTo("search_text");
			assertThat(call.arguments()).containsEntry("query", "agent");
		});
	}

	@Test
	void choosesWriteToolForExplicitWritePrompt() {
		ModelResponse response = this.client.complete(request("please write a note"));

		assertThat(response.toolCalls()).singleElement().satisfies(call -> {
			assertThat(call.name()).isEqualTo("write_file");
			assertThat(call.arguments()).containsEntry("path", "src/mock-note.txt");
		});
	}

	@Test
	void choosesRunCommandToolForCommandPrompt() {
		ModelResponse response = this.client.complete(request("please run tests"));

		assertThat(response.toolCalls()).singleElement().satisfies(call -> {
			assertThat(call.name()).isEqualTo("run_command");
			assertThat(call.arguments()).containsKey("command");
		});
	}

	@Test
	void defaultsToListFiles() {
		ModelResponse response = this.client.complete(request("inspect the workspace"));

		assertThat(response.toolCalls()).singleElement().satisfies(call -> {
			assertThat(call.name()).isEqualTo("list_files");
			assertThat(call.arguments()).containsEntry("path", ".");
		});
	}

	private ModelRequest request(String prompt) {
		return new ModelRequest(List.of(ModelMessage.user(prompt)),
				List.of(new ToolDefinition("list_files", "List", Map.of("type", "object"))));
	}
}
