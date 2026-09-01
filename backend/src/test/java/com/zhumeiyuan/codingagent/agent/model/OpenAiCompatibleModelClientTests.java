package com.zhumeiyuan.codingagent.agent.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;

import org.junit.jupiter.api.Test;

class OpenAiCompatibleModelClientTests {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ModelResponseParser parser = new ModelResponseParser(this.objectMapper);

	@Test
	void sendsNativeToolRequestAndParsesToolCalls() throws Exception {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200, nativeToolCallResponse()));
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
		assertThat(body.has("response_format")).isFalse();
		assertThat(body.path("tool_choice").asText()).isEqualTo("auto");
		assertThat(body.path("tools")).hasSize(1);
		assertThat(body.path("tools").path(0).path("type").asText()).isEqualTo("function");
		assertThat(body.path("tools").path(0).path("function").path("name").asText()).isEqualTo("list_files");
		assertThat(body.path("tools").path(0).path("function").path("parameters").path("type").asText())
				.isEqualTo("object");
		assertThat(body.path("messages")).hasSize(2);
		assertThat(body.path("messages").path(0).path("content").asText()).contains("provided function tools");
	}

	@Test
	void streamsNativeTextDeltasAndReturnsFinalResponse() throws Exception {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200, """
				data: {"choices":[{"delta":{"content":"Hel"}}]}

				data: {"choices":[{"delta":{"content":"lo"},"finish_reason":"stop"}]}

				data: [DONE]
				"""));
		OpenAiCompatibleModelClient client = client(transport, env -> "test-key");
		List<String> deltas = new java.util.ArrayList<>();

		ModelResponse response = client.completeStreaming(new ModelRequest(List.of(ModelMessage.user("say hi")), List.of()),
				deltas::add);

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.STOP);
		assertThat(response.message()).isEqualTo("Hello");
		assertThat(deltas).containsExactly("Hel", "lo");
		JsonNode body = this.objectMapper.readTree(transport.request.body());
		assertThat(body.path("stream").asBoolean()).isTrue();
	}

	@Test
	void parsesFragmentedNativeToolCallsFromStream() {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200, """
				data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"call-1","function":{"name":"list_files","arguments":"{\\\"pa"}}]}}]}

				data: {"choices":[{"delta":{"tool_calls":[{"index":0,"function":{"arguments":"th\\\":\\\".\\\"}"}}]},"finish_reason":"tool_calls"}]}

				data: [DONE]
				"""));
		OpenAiCompatibleModelClient client = client(transport, env -> "test-key");

		ModelResponse response = client.completeStreaming(new ModelRequest(List.of(ModelMessage.user("inspect")), List.of()),
				ignored -> {});

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.TOOL_CALLS);
		assertThat(response.message()).isEqualTo("Model requested tool execution.");
		assertThat(response.toolCalls()).singleElement().satisfies(call -> {
			assertThat(call.id()).isEqualTo("call-1");
			assertThat(call.name()).isEqualTo("list_files");
			assertThat(call.arguments()).containsEntry("path", ".");
		});
	}

	@Test
	void fallsBackToNonStreamingNativeRequestWhenProviderStreamIsEmpty() throws Exception {
		CapturingTransport transport = new CapturingTransport(
				new ModelHttpResponse(200, "data: [DONE]\n"),
				new ModelHttpResponse(200, nativeStopResponse("Recovered with non-streaming response")));
		OpenAiCompatibleModelClient client = client(transport, env -> "test-key");

		ModelResponse response = client.completeStreaming(new ModelRequest(List.of(ModelMessage.user("hi")), List.of()),
				ignored -> {});

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.STOP);
		assertThat(response.message()).isEqualTo("Recovered with non-streaming response");
		assertThat(transport.requestCount()).isEqualTo(2);
		assertThat(this.objectMapper.readTree(transport.requests().get(0).body()).path("stream").asBoolean()).isTrue();
		assertThat(this.objectMapper.readTree(transport.requests().get(1).body()).path("stream").asBoolean()).isFalse();
	}

	@Test
	void fallsBackToNonStreamingNativeRequestWhenProviderStreamTransportFails() throws Exception {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200,
				nativeStopResponse("Recovered after streaming transport failure"))) {
			@Override
			public ModelHttpStreamResponse stream(ModelHttpRequest request) {
				this.requests.add(request);
				this.requestCount++;
				throw new ModelClientException("Model HTTP streaming request failed", new IOException("connection reset"));
			}
		};
		OpenAiCompatibleModelClient client = client(transport, env -> "test-key");

		ModelResponse response = client.completeStreaming(new ModelRequest(List.of(ModelMessage.user("hi")), List.of()),
				ignored -> {});

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.STOP);
		assertThat(response.message()).isEqualTo("Recovered after streaming transport failure");
		assertThat(transport.requestCount()).isEqualTo(2);
		assertThat(this.objectMapper.readTree(transport.requests().get(0).body()).path("stream").asBoolean()).isTrue();
		assertThat(this.objectMapper.readTree(transport.requests().get(1).body()).path("stream").asBoolean()).isFalse();
	}

	@Test
	void sendsNativeAssistantToolCallsAndToolResults() throws Exception {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200, nativeStopResponse("Done")));
		OpenAiCompatibleModelClient client = client(transport, env -> "test-key");

		client.complete(new ModelRequest(List.of(
				ModelMessage.assistant("Need files", List.of(new ToolCall("call-1", "list_files", Map.of("path", ".")))),
				ModelMessage.tool("call-1", "list_files", "tool result")), List.of()));

		JsonNode body = this.objectMapper.readTree(transport.request.body());
		JsonNode assistant = body.path("messages").path(1);
		assertThat(assistant.path("role").asText()).isEqualTo("assistant");
		assertThat(assistant.path("tool_calls").path(0).path("id").asText()).isEqualTo("call-1");
		assertThat(assistant.path("tool_calls").path(0).path("function").path("name").asText()).isEqualTo("list_files");
		JsonNode arguments = this.objectMapper.readTree(
				assistant.path("tool_calls").path(0).path("function").path("arguments").asText());
		assertThat(arguments.path("path").asText()).isEqualTo(".");
		JsonNode tool = body.path("messages").path(2);
		assertThat(tool.path("role").asText()).isEqualTo("tool");
		assertThat(tool.path("tool_call_id").asText()).isEqualTo("call-1");
		assertThat(tool.path("content").asText()).isEqualTo("tool result");
	}

	@Test
	void sendsJsonContentRequestWhenCompatibilityProtocolIsConfigured() throws Exception {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200, completionResponse("""
				{"message":"Need files","finish_reason":"tool_calls","tool_calls":[{"id":"call-1","name":"list_files","arguments":{"path":"."}}]}
				""")));
		OpenAiCompatibleModelClient client = jsonContentClient(transport, env -> "test-key");

		ModelResponse response = client.complete(new ModelRequest(List.of(ModelMessage.user("inspect workspace")),
				List.of(new ToolDefinition("list_files", "List workspace files", Map.of("type", "object")))));

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.TOOL_CALLS);
		JsonNode body = this.objectMapper.readTree(transport.request.body());
		assertThat(body.path("response_format").path("type").asText()).isEqualTo("json_object");
		assertThat(body.has("tools")).isFalse();
		assertThat(body.path("messages").path(0).path("content").asText()).contains("Available tools");
	}

	@Test
	void mapsToolObservationsToUserMessagesForCompatibility() throws Exception {
		CapturingTransport transport = new CapturingTransport(new ModelHttpResponse(200, completionResponse("""
				{"message":"Done","finish_reason":"stop"}
				""")));
		OpenAiCompatibleModelClient client = jsonContentClient(transport, env -> "test-key");

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
	void retriesOnceWithProtocolRepairReminderWhenProviderReturnsEmptyContent() throws Exception {
		CapturingTransport transport = new CapturingTransport(
				new ModelHttpResponse(200, completionResponseAllowingEmpty("")),
				new ModelHttpResponse(200, completionResponse("{\"message\":\"Done\",\"finish_reason\":\"stop\"}")));
		OpenAiCompatibleModelClient client = jsonContentClient(transport, env -> "test-key");

		ModelResponse response = client.complete(new ModelRequest(List.of(ModelMessage.user("hi")), List.of()));

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.STOP);
		assertThat(transport.requestCount()).isEqualTo(2);
		assertThat(transport.requests().get(0).body()).doesNotContain("Previous provider response could not be accepted");
		assertThat(transport.requests().get(1).body()).contains("Previous provider response could not be accepted");
	}

	@Test
	void retriesOnceWithProtocolRepairReminderWhenModelProtocolMessageIsBlank() throws Exception {
		CapturingTransport transport = new CapturingTransport(
				new ModelHttpResponse(200, completionResponse("{\"message\":\"   \",\"finish_reason\":\"stop\"}")),
				new ModelHttpResponse(200, completionResponse("{\"message\":\"Done\",\"finish_reason\":\"stop\"}")));
		OpenAiCompatibleModelClient client = jsonContentClient(transport, env -> "test-key");

		ModelResponse response = client.complete(new ModelRequest(List.of(ModelMessage.user("hi")), List.of()));

		assertThat(response.finishReason()).isEqualTo(ModelFinishReason.STOP);
		assertThat(transport.requestCount()).isEqualTo(2);
		assertThat(transport.requests().get(1).body()).contains("Model field 'message' must be a non-blank string");
	}

	@Test
	void failsWhenProviderResponseHasNoContent() {
		OpenAiCompatibleModelClient client = jsonContentClient(new CapturingTransport(new ModelHttpResponse(200, "{}")),
				env -> "test-key");

		assertThatThrownBy(() -> client.complete(new ModelRequest(List.of(ModelMessage.user("hi")), List.of())))
				.isInstanceOf(ModelClientException.class)
				.hasMessageContaining("choices[0].message.content")
				.hasMessageContaining("message_fields");
	}


	private String nativeToolCallResponse() {
		return """
				{"choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":"Need files","tool_calls":[{"id":"call-1","type":"function","function":{"name":"list_files","arguments":"{\\\"path\\\":\\\".\\\"}"}}]}}]}
				""";
	}

	private String nativeStopResponse(String content) throws Exception {
		return this.objectMapper.writeValueAsString(
				Map.of("choices", List.of(Map.of("finish_reason", "stop", "message", Map.of("content", content)))));
	}

	private String completionResponse(String content) throws Exception {
		return this.objectMapper.writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
	}

	private String completionResponseAllowingEmpty(String content) {
		return "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":\""
				+ content + "\"}}]}";
	}

	private OpenAiCompatibleModelClient client(ModelHttpTransport transport,
			java.util.function.Function<String, String> environment) {
		return client(transport, environment, AgentModelProperties.ToolProtocol.NATIVE_TOOLS);
	}

	private OpenAiCompatibleModelClient jsonContentClient(ModelHttpTransport transport,
			java.util.function.Function<String, String> environment) {
		return client(transport, environment, AgentModelProperties.ToolProtocol.JSON_CONTENT);
	}

	private OpenAiCompatibleModelClient client(ModelHttpTransport transport,
			java.util.function.Function<String, String> environment, AgentModelProperties.ToolProtocol toolProtocol) {
		AgentModelProperties properties = new AgentModelProperties();
		properties.setProvider(AgentModelProperties.Provider.OPENAI_COMPATIBLE);
		properties.setBaseUrl("https://api.deepseek.com/");
		properties.setName("deepseek-v4-flash");
		properties.setApiKeyEnv("DEEPSEEK_API_KEY");
		properties.setTimeout(Duration.ofSeconds(12));
		properties.setToolProtocol(toolProtocol);
		return new OpenAiCompatibleModelClient(properties, this.parser, this.objectMapper, transport, environment);
	}

	private static class CapturingTransport implements ModelHttpTransport {

		private final List<ModelHttpResponse> responses;

		private ModelHttpRequest request;

		protected final List<ModelHttpRequest> requests = new java.util.ArrayList<>();

		protected int requestCount;

		CapturingTransport(ModelHttpResponse response) {
			this(List.of(response));
		}

		CapturingTransport(ModelHttpResponse firstResponse, ModelHttpResponse secondResponse) {
			this(List.of(firstResponse, secondResponse));
		}

		CapturingTransport(List<ModelHttpResponse> responses) {
			this.responses = responses;
		}

		@Override
		public ModelHttpResponse send(ModelHttpRequest request) {
			this.request = request;
			this.requests.add(request);
			ModelHttpResponse response = this.responses.get(Math.min(this.requestCount, this.responses.size() - 1));
			this.requestCount++;
			return response;
		}

		List<ModelHttpRequest> requests() {
			return this.requests;
		}

		int requestCount() {
			return this.requestCount;
		}
	}
}
