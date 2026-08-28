package com.zhumeiyuan.codingagent.agent.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;

import org.junit.jupiter.api.Test;

class OpenAiCompatibleModelClientTests {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ModelResponseParser parser = new ModelResponseParser(this.objectMapper);

	@Test
	void sendsChatCompletionRequestAndParsesJsonContent() throws Exception {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200, completionResponse("""
				{"message":"Need files","finish_reason":"tool_calls","tool_calls":[{"id":"call-1","name":"list_files","arguments":{"path":"."}}]}
				""")));
		OpenAiCompatibleModelClient client = client(transport, env -> "test-key");

		ModelResponse response = client.complete(new ModelRequest(List.of(ModelMessage.user("inspect workspace")),
				List.of(new ToolDefinition("list_files", "List workspace files", Map.of("type", "object")))));

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.TOOL_CALLS);
		assertThat(response.toolCalls()).singleElement().satisfies(call -> {
			assertThat(call.id()).isEqualTo("call-1");
			assertThat(call.name()).isEqualTo("list_files");
			assertThat(call.arguments()).containsEntry("path", ".");
		});
		assertThat(transport.request.headers()).containsEntry("Authorization", "Bearer test-key");
		assertThat(transport.request.uri().toString()).isEqualTo("https://api.deepseek.com/chat/completions");
		JsonNode body = this.objectMapper.readTree(transport.request.body());
		assertThat(body.path("model").asText()).isEqualTo("deepseek-v4-flash");
		assertThat(body.path("thinking").path("type").asText()).isEqualTo("disabled");
		assertThat(body.path("response_format").path("type").asText()).isEqualTo("json_object");
		assertThat(body.path("messages")).hasSize(2);
		assertThat(body.path("messages").path(0).path("content").asText()).contains("Available tools");
	}

	@Test
	void mapsToolObservationsToUserMessagesForCompatibility() throws Exception {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200, completionResponse("""
				{"message":"Done","finish_reason":"stop"}
				""")));
		OpenAiCompatibleModelClient client = client(transport, env -> "test-key");

		client.complete(new ModelRequest(List.of(ModelMessage.tool("tool result")), List.of()));

		JsonNode body = this.objectMapper.readTree(transport.request.body());
		assertThat(body.path("messages").path(1).path("role").asText()).isEqualTo("user");
		assertThat(body.path("messages").path(1).path("content").asText()).isEqualTo("tool result");
	}

	@Test
	void failsWhenApiKeyEnvironmentVariableIsMissing() {
		OpenAiCompatibleModelClient client = client(new CapturingTransport(new ModelHttpResponse(200, "{}")), env -> null);

		assertThatThrownBy(() -> client.complete(new ModelRequest(List.of(ModelMessage.user("hi")), List.of())))
				.isInstanceOf(ModelClientException.class)
				.hasMessageContaining("DEEPSEEK_API_KEY");
	}

	@Test
	void failsOnProviderHttpError() {
		OpenAiCompatibleModelClient client = client(new CapturingTransport(new ModelHttpResponse(429, "rate limited")),
				env -> "test-key");

		assertThatThrownBy(() -> client.complete(new ModelRequest(List.of(ModelMessage.user("hi")), List.of())))
				.isInstanceOf(ModelClientException.class)
				.hasMessageContaining("HTTP 429");
	}

	@Test
	void failsWhenProviderResponseHasNoContent() {
		OpenAiCompatibleModelClient client = client(new CapturingTransport(new ModelHttpResponse(200, "{}")),
				env -> "test-key");

		assertThatThrownBy(() -> client.complete(new ModelRequest(List.of(ModelMessage.user("hi")), List.of())))
				.isInstanceOf(ModelClientException.class)
				.hasMessageContaining("choices[0].message.content")
				.hasMessageContaining("message_fields");
	}

	private String completionResponse(String content) throws Exception {
		return this.objectMapper.writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
	}

	private OpenAiCompatibleModelClient client(ModelHttpTransport transport,
			java.util.function.Function<String, String> environment) {
		AgentModelProperties properties = new AgentModelProperties();
		properties.setProvider(AgentModelProperties.Provider.OPENAI_COMPATIBLE);
		properties.setBaseUrl("https://api.deepseek.com/");
		properties.setName("deepseek-v4-flash");
		properties.setApiKeyEnv("DEEPSEEK_API_KEY");
		properties.setTimeout(Duration.ofSeconds(12));
		return new OpenAiCompatibleModelClient(properties, this.parser, this.objectMapper, transport, environment);
	}

	private static class CapturingTransport implements ModelHttpTransport {

		private final ModelHttpResponse response;

		private ModelHttpRequest request;

		CapturingTransport(ModelHttpResponse response) {
			this.response = response;
		}

		@Override
		public ModelHttpResponse send(ModelHttpRequest request) {
			this.request = request;
			return this.response;
		}
	}
}
