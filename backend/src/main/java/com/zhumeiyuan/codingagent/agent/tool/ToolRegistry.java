package com.zhumeiyuan.codingagent.agent.tool;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;

public class ToolRegistry {

	private final Map<String, RegisteredTool> tools;
	private final Clock clock;

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
					withBaseMetadata(result.metadata(), call.name(), null), this.clock.instant());
		} catch (ToolExecutionException ex) {
			return failure(call, ex.code(), ex.getMessage(), Map.of("toolName", nullSafe(ex.toolName())));
		} catch (RuntimeException ex) {
			return failure(call, ToolExecutionErrorCode.TOOL_RUNTIME_ERROR, "Tool failed unexpectedly", Map.of());
		}
	}

	private ToolResult failure(ToolCall call, ToolExecutionErrorCode code, String message, Map<String, Object> metadata) {
		return ToolResult.failure(call.id(), message, withBaseMetadata(metadata, call.name(), code), this.clock.instant());
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
}
