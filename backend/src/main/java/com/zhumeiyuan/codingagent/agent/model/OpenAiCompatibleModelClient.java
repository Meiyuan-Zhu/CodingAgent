package com.zhumeiyuan.codingagent.agent.model;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;

public class OpenAiCompatibleModelClient implements ModelClient {

	private static final String JSON_PROTOCOL_INSTRUCTIONS = """
			You are the model inside a local coding agent. You must reply with one JSON object only.
			Do not wrap the JSON in markdown.

			Response schema:
			{
			  "message": "short explanation for the user",
			  "finish_reason": "stop" | "tool_calls" | "length",
			  "tool_calls": [
			    {
			      "id": "unique id chosen by you",
			      "name": "registered_tool_name",
			      "arguments": { }
			    }
			  ]
			}

			The message field is required and must never be empty.
			If you need workspace information or a file change, call one of the available tools.
			If no tool is needed, set finish_reason to "stop", omit tool_calls, and put your final user-facing answer in message.
			Use only the tools listed below. The application, not you, executes tools and enforces approvals.
			""";

	private final AgentModelProperties properties;
	private final ModelResponseParser parser;
	private final ObjectMapper objectMapper;
	private final ModelHttpTransport transport;
	private final Function<String, String> environment;

	public OpenAiCompatibleModelClient(AgentModelProperties properties, ModelResponseParser parser, ObjectMapper objectMapper,
			ModelHttpTransport transport, Function<String, String> environment) {
		this.properties = Objects.requireNonNull(properties, "properties");
		this.parser = Objects.requireNonNull(parser, "parser");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.transport = Objects.requireNonNull(transport, "transport");
		this.environment = Objects.requireNonNull(environment, "environment");
	}

	@Override
	public String providerName() {
		return "openai-compatible:" + this.properties.getName();
	}

	@Override
	public ModelResponse complete(ModelRequest request) {
		Objects.requireNonNull(request, "request");
		validateProperties();
		String apiKey = apiKey();
		ModelHttpResponse response = this.transport.send(new ModelHttpRequest(chatCompletionsUri(), headers(apiKey),
				requestBody(request), this.properties.getTimeout()));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new ModelClientException("Model provider returned HTTP " + response.statusCode());
		}
		return this.parser.parse(extractMessageContent(response.body()));
	}

	private void validateProperties() {
		if (isBlank(this.properties.getBaseUrl())) {
			throw new ModelClientException("agent.model.base-url must not be blank");
		}
		if (isBlank(this.properties.getName())) {
			throw new ModelClientException("agent.model.name must not be blank");
		}
		if (isBlank(this.properties.getApiKeyEnv())) {
			throw new ModelClientException("agent.model.api-key-env must not be blank");
		}
		Duration timeout = this.properties.getTimeout();
		if (timeout == null || timeout.isZero() || timeout.isNegative()) {
			throw new ModelClientException("agent.model.timeout must be positive");
		}
	}

	private String apiKey() {
		String apiKey = this.environment.apply(this.properties.getApiKeyEnv());
		if (isBlank(apiKey)) {
			throw new ModelClientException("Missing model API key environment variable: " + this.properties.getApiKeyEnv());
		}
		return apiKey;
	}

	private Map<String, String> headers(String apiKey) {
		return Map.of(
				"Authorization", "Bearer " + apiKey,
				"Content-Type", "application/json");
	}

	private URI chatCompletionsUri() {
		String baseUrl = trimTrailingSlash(this.properties.getBaseUrl());
		return URI.create(baseUrl + "/chat/completions");
	}

	private String requestBody(ModelRequest request) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", this.properties.getName());
		body.put("temperature", this.properties.getTemperature());
		body.put("thinking", Map.of("type", "disabled"));
		body.put("response_format", Map.of("type", "json_object"));
		body.put("messages", chatMessages(request));
		try {
			return this.objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException ex) {
			throw new ModelClientException("Cannot serialize model request", ex);
		}
	}

	private List<Map<String, String>> chatMessages(ModelRequest request) {
		List<Map<String, String>> messages = new ArrayList<>();
		messages.add(Map.of("role", "system", "content", JSON_PROTOCOL_INSTRUCTIONS + "\n" + toolsJson(request.tools())));
		for (ModelMessage message : request.messages()) {
			messages.add(Map.of("role", chatRole(message.role()), "content", message.content()));
		}
		return messages;
	}

	private String toolsJson(List<ToolDefinition> tools) {
		try {
			return "Available tools:\n" + this.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tools);
		} catch (JsonProcessingException ex) {
			throw new ModelClientException("Cannot serialize tool definitions", ex);
		}
	}

	private String chatRole(ModelRole role) {
		return switch (role) {
			case SYSTEM -> "system";
			case USER -> "user";
			case ASSISTANT -> "assistant";
			case TOOL -> "user";
		};
	}

	private String extractMessageContent(String rawBody) {
		try {
			JsonNode root = this.objectMapper.readTree(rawBody);
			JsonNode content = root.path("choices").path(0).path("message").path("content");
			if (!content.isTextual() || content.asText().isBlank()) {
				throw new ModelClientException("Model provider response does not contain choices[0].message.content");
			}
			return content.asText();
		} catch (JsonProcessingException ex) {
			throw new ModelClientException("Model provider response is not valid JSON", ex);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String trimTrailingSlash(String value) {
		String trimmed = value.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}
}
