package com.zhumeiyuan.codingagent.agent.model;

import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HeuristicMockModelClient implements ModelClient {

	private final ModelResponseParser parser;
	private final ObjectMapper objectMapper;

	public HeuristicMockModelClient(ModelResponseParser parser, ObjectMapper objectMapper) {
		this.parser = Objects.requireNonNull(parser, "parser");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public String providerName() {
		return "mock";
	}

	@Override
	public ModelResponse complete(ModelRequest request) {
		Objects.requireNonNull(request, "request");
		ModelMessage lastMessage = request.messages().get(request.messages().size() - 1);
		if (lastMessage.role() == ModelRole.TOOL) {
			return this.parser.parse(stopResponse("Mock model observed the tool result and finished."));
		}
		String userPrompt = request.lastUserMessage()
				.map(ModelMessage::content)
				.orElse("");
		return this.parser.parse(scriptedResponse(userPrompt));
	}

	private String scriptedResponse(String userPrompt) {
		String lowerPrompt = userPrompt.toLowerCase(Locale.ROOT);
		if (lowerPrompt.contains("write") || lowerPrompt.contains("create") || userPrompt.contains("写入")
				|| userPrompt.contains("创建")) {
			return response("Mock model asks to write a workspace file.", "write_file",
					"{\"path\":\"src/mock-note.txt\",\"content\":\"mock note\\n\"}");
		}
		if (lowerPrompt.contains("readme")) {
			return response("Mock model asks to read README.md.", "read_file", "{\"path\":\"README.md\"}");
		}
		if (lowerPrompt.contains("search") || userPrompt.contains("搜索") || userPrompt.contains("查找")) {
			return response("Mock model asks to search workspace text.", "search_text",
					"{\"query\":\"agent\",\"max_matches\":10}");
		}
		if (lowerPrompt.contains("test") || lowerPrompt.contains("command") || lowerPrompt.contains("build")
				|| userPrompt.contains("命令") || userPrompt.contains("测试") || userPrompt.contains("构建")) {
			return response("Mock model asks to run a workspace command.", "run_command",
					"{\"command\":[\"/bin/echo\",\"mock command\"],\"cwd\":\".\"}");
		}
		return response("Mock model asks to inspect workspace files.", "list_files",
				"{\"path\":\".\",\"max_entries\":50}");
	}

	private String stopResponse(String message) {
		try {
			return """
					{
					  "message": %s,
					  "finish_reason": "stop"
					}
					""".formatted(this.objectMapper.writeValueAsString(message));
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Cannot serialize mock model response", ex);
		}
	}

	private String response(String message, String toolName, String argumentsJson) {
		try {
			return """
					{
					  "message": %s,
					  "finish_reason": "tool_calls",
					  "tool_calls": [
					    {
					      "id": "mock-call-1",
					      "name": %s,
					      "arguments": %s
					    }
					  ]
					}
					""".formatted(this.objectMapper.writeValueAsString(message),
					this.objectMapper.writeValueAsString(toolName),
					argumentsJson);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Cannot serialize mock model response", ex);
		}
	}
}
