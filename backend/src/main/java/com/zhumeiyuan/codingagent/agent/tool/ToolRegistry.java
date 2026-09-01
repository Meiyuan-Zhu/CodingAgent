package com.zhumeiyuan.codingagent.agent.tool;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;

public class ToolRegistry {

	private final Map<String, RegisteredTool> tools;
	private final Clock clock;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ToolRegistry(List<RegisteredTool> tools, Clock clock) {
		Objects.requireNonNull(tools, "tools");
		this.clock = Objects.requireNonNull(clock, "clock");
		Map<String, RegisteredTool> byName = new LinkedHashMap<>();
		for (RegisteredTool tool : tools.stream()
				.sorted(Comparator.comparing(registered -> registered.definition().name()))
				.toList()) {
			RegisteredTool previous = byName.putIfAbsent(tool.definition().name(), tool);
			if (previous != null) {
				throw new IllegalArgumentException("Duplicate tool name: " + tool.definition().name());
			}
		}
		this.tools = Collections.unmodifiableMap(byName);
	}

	public List<ToolDefinition> definitions() {
		List<ToolDefinition> definitions = new ArrayList<>();
		for (RegisteredTool tool : this.tools.values()) {
			definitions.add(tool.definition());
		}
		return List.copyOf(definitions);
	}

	public ToolResult execute(ToolCall call) {
		Objects.requireNonNull(call, "call");
		RegisteredTool tool = this.tools.get(call.name());
		if (tool == null) {
			return failure(call, ToolExecutionErrorCode.UNKNOWN_TOOL, "Unknown tool: " + call.name(), Map.of());
		}

		try {
			ToolExecutionResult result = tool.handler().execute(call.arguments());
			return ToolResult.success(call.id(), result.content(),
					withBaseMetadata(result.metadata(), call.name(), null), result.privateMetadata(), this.clock.instant());
		} catch (ToolExecutionException ex) {
			return failure(call, ex.code(), ex.getMessage(), Map.of("toolName", nullSafe(ex.toolName())));
		} catch (RuntimeException ex) {
			return failure(call, ToolExecutionErrorCode.TOOL_RUNTIME_ERROR, "Tool failed unexpectedly", Map.of());
		}
	}

	private ToolResult failure(ToolCall call, ToolExecutionErrorCode code, String message, Map<String, Object> metadata) {
		Map<String, Object> enrichedMetadata = withBaseMetadata(metadata, call.name(), code);
		return ToolResult.failure(call.id(), failureContent(call.name(), code, message, enrichedMetadata), enrichedMetadata,
				this.clock.instant());
	}

	private Map<String, Object> withBaseMetadata(Map<String, Object> metadata, String toolName,
			ToolExecutionErrorCode errorCode) {
		Map<String, Object> enriched = new LinkedHashMap<>();
		enriched.putAll(metadata);
		enriched.put("toolName", toolName);
		if (errorCode != null) {
			enriched.put("errorCode", errorCode.name());
		}
		return enriched;
	}

	private String nullSafe(String value) {
		return value == null ? "" : value;
	}

	private String failureContent(String toolName, ToolExecutionErrorCode code, String message, Map<String, Object> metadata) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("success", false);
		body.put("message", message);
		body.put("toolName", toolName);
		body.put("errorCode", code.name());
		body.put("failureKind", "RECOVERABLE_TOOL_ERROR");
		body.put("recoverable", true);
		body.put("recoveryHint", recoveryHint(code));
		body.put("timedOut", code == ToolExecutionErrorCode.TOOL_TIMEOUT);
		body.put("metadata", metadata);
		try {
			return this.objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException ex) {
			return "{\"success\":false,\"message\":\"Tool failed and the failure result could not be serialized.\"}";
		}
	}

	private String recoveryHint(ToolExecutionErrorCode code) {
		return switch (code) {
			case UNKNOWN_TOOL -> "Use one of the available tool names from the current tool list.";
			case INVALID_ARGUMENTS -> "Check the tool schema, correct the arguments, and try again.";
			case WORKSPACE_NOT_FOUND -> "Inspect the workspace with list_files or search_text, then try the correct path.";
			case WORKSPACE_INVALID_PATH, WORKSPACE_ACCESS_DENIED, WORKSPACE_PERMISSION_DENIED ->
				"Choose a safe relative path inside the configured workspace and try again.";
			case WORKSPACE_CONFLICT -> "Read the current file state, then retry with overwrite or expected_sha256 if appropriate.";
			case WORKSPACE_EDIT_MISS -> "Read the target file, choose exact current text, then retry the edit.";
			case TOOL_TIMEOUT -> "Use a narrower command or tool request, then try again within the budget.";
			case TOOL_RUNTIME_ERROR -> "Try a simpler tool request or inspect the workspace before choosing another approach.";
		};
	}
}
