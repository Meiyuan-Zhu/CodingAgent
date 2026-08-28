package com.zhumeiyuan.codingagent.agent.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;

public class ModelResponseParser {

	private static final int MAX_MESSAGE_CHARS = 20_000;
	private static final int MAX_TOOL_CALLS = 8;
	private static final TypeReference<Map<String, Object>> ARGUMENTS_TYPE = new TypeReference<>() {
	};

	private final ObjectMapper objectMapper;

	public ModelResponseParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ModelResponse parse(String rawResponse) {
		if (rawResponse == null || rawResponse.isBlank()) {
			throw new ModelParseException("Model response is blank");
		}
		JsonNode root = parseJson(rawResponse);
		if (!root.isObject()) {
			throw new ModelParseException("Model response must be a JSON object");
		}

		ModelFinishReason finishReason = finishReason(root);
		List<ToolCall> toolCalls = toolCalls(root);
		String message = message(root, finishReason, toolCalls);
		if (message.length() > MAX_MESSAGE_CHARS) {
			throw new ModelParseException("Model response message is too long");
		}
		try {
			return new ModelResponse(message, finishReason, toolCalls);
		} catch (IllegalArgumentException ex) {
			throw new ModelParseException(ex.getMessage(), ex);
		}
	}

	private JsonNode parseJson(String rawResponse) {
		try {
			return this.objectMapper.readTree(rawResponse);
		} catch (JsonProcessingException firstFailure) {
			String extracted = extractFirstJsonObject(rawResponse);
			if (extracted == null) {
				throw new ModelParseException("Model response is not valid JSON", firstFailure);
			}
			try {
				return this.objectMapper.readTree(extracted);
			} catch (JsonProcessingException secondFailure) {
				throw new ModelParseException("Model response is not valid JSON", secondFailure);
			}
		}
	}

	private String extractFirstJsonObject(String rawResponse) {
		int start = rawResponse.indexOf('{');
		if (start < 0) {
			return null;
		}
		boolean inString = false;
		boolean escaped = false;
		int depth = 0;
		for (int index = start; index < rawResponse.length(); index++) {
			char current = rawResponse.charAt(index);
			if (escaped) {
				escaped = false;
				continue;
			}
			if (current == '\\' && inString) {
				escaped = true;
				continue;
			}
			if (current == '"') {
				inString = !inString;
				continue;
			}
			if (inString) {
				continue;
			}
			if (current == '{') {
				depth++;
			}
			else if (current == '}') {
				depth--;
				if (depth == 0) {
					return rawResponse.substring(start, index + 1);
				}
			}
		}
		return null;
	}

	private ModelFinishReason finishReason(JsonNode root) {
		String rawReason = optionalText(root, "finish_reason", "stop").toUpperCase(Locale.ROOT);
		try {
			return ModelFinishReason.valueOf(rawReason);
		} catch (IllegalArgumentException ex) {
			throw new ModelParseException("Unknown model finish_reason: " + rawReason, ex);
		}
	}

	private String message(JsonNode root, ModelFinishReason finishReason, List<ToolCall> toolCalls) {
		JsonNode value = root.get("message");
		if (value != null && !value.isNull() && !value.isTextual()) {
			throw new ModelParseException("Model field 'message' must be a string");
		}
		if (value != null && value.isTextual() && !value.asText().isBlank()) {
			return value.asText();
		}
		if (finishReason == ModelFinishReason.TOOL_CALLS && !toolCalls.isEmpty()) {
			return "Model requested tool execution.";
		}
		throw new ModelParseException("Model field 'message' must be a non-blank string");
	}

	private List<ToolCall> toolCalls(JsonNode root) {
		JsonNode callsNode = root.get("tool_calls");
		if (callsNode == null || callsNode.isNull()) {
			return List.of();
		}
		if (!callsNode.isArray()) {
			throw new ModelParseException("Model tool_calls must be an array");
		}
		if (callsNode.size() > MAX_TOOL_CALLS) {
			throw new ModelParseException("Model tool_calls exceeds limit of " + MAX_TOOL_CALLS);
		}

		List<ToolCall> calls = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		for (JsonNode callNode : callsNode) {
			if (!callNode.isObject()) {
				throw new ModelParseException("Each model tool call must be an object");
			}
			String id = requiredText(callNode, "id");
			if (!ids.add(id)) {
				throw new ModelParseException("Duplicate model tool call id: " + id);
			}
			String name = requiredText(callNode, "name");
			JsonNode argumentsNode = callNode.get("arguments");
			if (argumentsNode == null || !argumentsNode.isObject()) {
				throw new ModelParseException("Model tool call arguments must be an object");
			}
			calls.add(new ToolCall(id, name, readArguments(argumentsNode)));
		}
		return calls;
	}

	private Map<String, Object> readArguments(JsonNode argumentsNode) {
		try {
			return this.objectMapper.convertValue(argumentsNode, ARGUMENTS_TYPE);
		} catch (IllegalArgumentException ex) {
			throw new ModelParseException("Cannot parse model tool call arguments", ex);
		}
	}

	private String requiredText(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		if (value == null || !value.isTextual() || value.asText().isBlank()) {
			throw new ModelParseException("Model field '" + fieldName + "' must be a non-blank string");
		}
		return value.asText();
	}

	private String optionalText(JsonNode node, String fieldName, String defaultValue) {
		JsonNode value = node.get(fieldName);
		if (value == null || value.isNull()) {
			return defaultValue;
		}
		if (!value.isTextual()) {
			throw new ModelParseException("Model field '" + fieldName + "' must be a string");
		}
		return value.asText();
	}
}
