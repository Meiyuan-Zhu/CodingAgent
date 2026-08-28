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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
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
			If you need workspace information, a file change, or command output, call one of the available tools.
			Call at most one tool at a time, then wait for the tool observation before deciding the next step.
			For code edits, prefer replace_text when changing a small known snippet.
			For commands, use run_command with an argv array and no shell syntax, for example {"command":["python3","-m","unittest","discover","-s","tests","-v"],"cwd":"."}.
			If no tool is needed, set finish_reason to "stop", omit tool_calls, and put your final user-facing answer in message.
			Use only the tools listed below. The application, not you, executes tools and enforces approvals.
			""";

	private static final String PROTOCOL_REPAIR_RETRY_MESSAGE = "Previous provider response could not be accepted: %s. "
			+ "Reply again with exactly one non-empty JSON object following the required local agent schema. "
			+ "If you need to act, include finish_reason=tool_calls and one valid tool_calls item.";

	private static final TypeReference<Map<String, Object>> ARGUMENTS_TYPE = new TypeReference<>() {
	};

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
		if (this.properties.getToolProtocol() == AgentModelProperties.ToolProtocol.NATIVE_TOOLS) {
			return completeWithNativeTools(request);
		}
		return completeWithJsonContent(request);
	}

	private ModelResponse completeWithNativeTools(ModelRequest request) {
		String apiKey = apiKey();
		ModelHttpResponse response = this.transport.send(new ModelHttpRequest(chatCompletionsUri(), headers(apiKey),
				nativeToolRequestBody(request), this.properties.getTimeout()));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new ModelClientException("Model provider returned HTTP " + response.statusCode());
		}
		return parseNativeToolResponse(response.body());
	}

	private ModelResponse completeWithJsonContent(ModelRequest request) {
		String apiKey = apiKey();
		RuntimeException firstRecoverableFailure = null;
		for (int attempt = 1; attempt <= 2; attempt++) {
			ModelRequest effectiveRequest = attempt == 1 ? request : requestWithProtocolRepairReminder(request, firstRecoverableFailure);
			ModelHttpRequest httpRequest = new ModelHttpRequest(chatCompletionsUri(), headers(apiKey),
					jsonContentRequestBody(effectiveRequest), this.properties.getTimeout());
			ModelHttpResponse response = this.transport.send(httpRequest);
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new ModelClientException("Model provider returned HTTP " + response.statusCode());
			}
			try {
				return this.parser.parse(extractMessageContent(response.body()));
			} catch (EmptyModelContentException | ModelParseException ex) {
				if (!isRecoverableProtocolFailure(ex)) {
					throw ex;
				}
				if (firstRecoverableFailure == null) {
					firstRecoverableFailure = ex;
				}
				if (attempt == 2) {
					throw firstRecoverableFailure;
				}
			}
		}
		throw firstRecoverableFailure;
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

	private ModelRequest requestWithProtocolRepairReminder(ModelRequest request, RuntimeException failure) {
		List<ModelMessage> messages = new ArrayList<>(request.messages());
		String reason = failure == null ? "unknown protocol failure" : failure.getMessage();
		messages.add(ModelMessage.user(PROTOCOL_REPAIR_RETRY_MESSAGE.formatted(reason)));
		return new ModelRequest(messages, request.tools());
	}

	private boolean isRecoverableProtocolFailure(RuntimeException ex) {
		if (ex instanceof EmptyModelContentException) {
			return true;
		}
		String message = ex.getMessage();
		return message != null && (message.contains("Model response is not valid JSON")
				|| message.contains("Model field 'message' must be a non-blank string"));
	}


	private String nativeToolRequestBody(ModelRequest request) {
		Map<String, Object> body = baseRequestBody();
		body.put("messages", nativeChatMessages(request));
		if (!request.tools().isEmpty()) {
			body.put("tools", nativeTools(request.tools()));
			body.put("tool_choice", "auto");
		}
		try {
			return this.objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException ex) {
			throw new ModelClientException("Cannot serialize model request", ex);
		}
	}

	private Map<String, Object> baseRequestBody() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", this.properties.getName());
		body.put("temperature", this.properties.getTemperature());
		body.put("thinking", Map.of("type", "disabled"));
		return body;
	}

	private List<Map<String, Object>> nativeTools(List<ToolDefinition> tools) {
		return tools.stream()
				.map(tool -> Map.<String, Object>of(
						"type", "function",
						"function", Map.of(
								"name", tool.name(),
								"description", tool.description(),
								"parameters", tool.inputSchema())))
				.toList();
	}

	private List<Map<String, Object>> nativeChatMessages(ModelRequest request) {
		List<Map<String, Object>> messages = new ArrayList<>();
		messages.add(Map.of("role", "system", "content", nativeToolInstructions()));
		for (ModelMessage message : request.messages()) {
			messages.add(nativeChatMessage(message));
		}
		return messages;
	}

	private Map<String, Object> nativeChatMessage(ModelMessage message) {
		Map<String, Object> chatMessage = new LinkedHashMap<>();
		chatMessage.put("role", chatRole(message.role()));
		chatMessage.put("content", message.content());
		if (message.role() == ModelRole.ASSISTANT && !message.toolCalls().isEmpty()) {
			chatMessage.put("tool_calls", nativeToolCalls(message.toolCalls()));
		}
		if (message.role() == ModelRole.TOOL) {
			chatMessage.put("tool_call_id", message.toolCallId());
		}
		return chatMessage;
	}

	private List<Map<String, Object>> nativeToolCalls(List<ToolCall> calls) {
		return calls.stream().map(call -> Map.<String, Object>of(
				"id", call.id(),
				"type", "function",
				"function", Map.of(
						"name", call.name(),
						"arguments", jsonArguments(call.arguments()))))
				.toList();
	}

	private String nativeToolInstructions() {
		return "You are the model inside a local coding agent. "
				+ "Use the provided function tools when you need workspace information, file changes, or command output. "
				+ "Call at most one tool at a time, then wait for the tool observation before deciding the next step. "
				+ "For code edits, prefer replace_text when changing a small known snippet. "
				+ "For commands, use run_command with an argv array and no shell syntax, for example {\"command\":[\"python3\",\"-m\",\"unittest\",\"discover\",\"-s\",\"tests\",\"-v\"],\"cwd\":\".\"}. "
				+ "The application, not you, executes tools, validates arguments, and enforces approvals. "
				+ "When no more tools are needed, answer the user directly.";
	}

	private ModelResponse parseNativeToolResponse(String rawBody) {
		try {
			JsonNode root = this.objectMapper.readTree(rawBody);
			JsonNode choice = root.path("choices").path(0);
			JsonNode message = choice.path("message");
			JsonNode callsNode = message.path("tool_calls");
			if (callsNode.isArray() && !callsNode.isEmpty()) {
				return new ModelResponse(nativeContent(message, "Model requested tool execution."),
						ModelFinishReason.TOOL_CALLS, nativeToolCallResults(callsNode));
			}
			return new ModelResponse(requiredNativeText(message.path("content"), "choices[0].message.content"),
					nativeFinishReason(choice), List.of());
		} catch (JsonProcessingException ex) {
			throw new ModelClientException("Model provider response is not valid JSON", ex);
		}
	}

	private String nativeContent(JsonNode message, String defaultContent) {
		JsonNode content = message.path("content");
		if (content.isTextual() && !content.asText().isBlank()) {
			return content.asText();
		}
		return defaultContent;
	}

	private ModelFinishReason nativeFinishReason(JsonNode choice) {
		String finishReason = choice.path("finish_reason").asText("stop");
		return "length".equals(finishReason) ? ModelFinishReason.LENGTH : ModelFinishReason.STOP;
	}

	private List<ToolCall> nativeToolCallResults(JsonNode callsNode) {
		List<ToolCall> calls = new ArrayList<>();
		for (JsonNode callNode : callsNode) {
			String id = requiredNativeText(callNode.path("id"), "tool_calls[].id");
			JsonNode function = callNode.path("function");
			String name = requiredNativeText(function.path("name"), "tool_calls[].function.name");
			String argumentsText = requiredNativeText(function.path("arguments"), "tool_calls[].function.arguments");
			calls.add(new ToolCall(id, name, readNativeArguments(argumentsText)));
		}
		return calls;
	}

	private Map<String, Object> readNativeArguments(String arguments) {
		try {
			Map<String, Object> parsed = this.objectMapper.readValue(arguments, ARGUMENTS_TYPE);
			return Map.copyOf(parsed);
		} catch (JsonProcessingException ex) {
			throw new ModelParseException("Native tool call arguments must be a JSON object string", ex);
		}
	}

	private String requiredNativeText(JsonNode node, String fieldName) {
		if (!node.isTextual() || node.asText().isBlank()) {
			throw new ModelParseException("Model field '" + fieldName + "' must be a non-blank string");
		}
		return node.asText();
	}

	private String jsonArguments(Map<String, Object> arguments) {
		try {
			return this.objectMapper.writeValueAsString(arguments);
		} catch (JsonProcessingException ex) {
			throw new ModelClientException("Cannot serialize tool call arguments", ex);
		}
	}

	private String jsonContentRequestBody(ModelRequest request) {
		Map<String, Object> body = baseRequestBody();
		body.put("response_format", Map.of("type", "json_object"));
		body.put("messages", jsonContentChatMessages(request));
		try {
			return this.objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException ex) {
			throw new ModelClientException("Cannot serialize model request", ex);
		}
	}

	private List<Map<String, String>> jsonContentChatMessages(ModelRequest request) {
		List<Map<String, String>> messages = new ArrayList<>();
		messages.add(Map.of("role", "system", "content", JSON_PROTOCOL_INSTRUCTIONS + "\n" + toolsJson(request.tools())));
		for (ModelMessage message : request.messages()) {
			messages.add(Map.of("role", jsonContentChatRole(message.role()), "content", jsonContentMessage(message)));
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

	private String jsonContentMessage(ModelMessage message) {
		if (message.role() == ModelRole.ASSISTANT && !message.toolCalls().isEmpty()) {
			return message.content() + "\nRequested tool calls:\n" + message.toolCalls().stream()
					.map(call -> "tool_call_id=" + call.id() + ", name=" + call.name() + ", arguments=" + call.arguments())
					.reduce((left, right) -> left + "\n" + right)
					.orElse("");
		}
		return message.content();
	}

	private String jsonContentChatRole(ModelRole role) {
		return role == ModelRole.TOOL ? "user" : chatRole(role);
	}

	private String chatRole(ModelRole role) {
		return switch (role) {
			case SYSTEM -> "system";
			case USER -> "user";
			case ASSISTANT -> "assistant";
			case TOOL -> "tool";
		};
	}

	private String extractMessageContent(String rawBody) {
		try {
			JsonNode root = this.objectMapper.readTree(rawBody);
			JsonNode choice = root.path("choices").path(0);
			JsonNode message = choice.path("message");
			JsonNode content = message.path("content");
			if (!content.isTextual() || content.asText().isBlank()) {
				throw new EmptyModelContentException("Model provider response does not contain choices[0].message.content ("
						+ responseShape(choice, message) + ")");
			}
			return content.asText();
		} catch (JsonProcessingException ex) {
			throw new ModelClientException("Model provider response is not valid JSON", ex);
		}
	}

	private String responseShape(JsonNode choice, JsonNode message) {
		List<String> fields = new ArrayList<>();
		message.fieldNames().forEachRemaining(fields::add);
		return "finish_reason=" + choice.path("finish_reason").asText("missing") + ", message_fields=" + fields;
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

	private static class EmptyModelContentException extends ModelClientException {

		EmptyModelContentException(String message) {
			super(message);
		}
	}
}
