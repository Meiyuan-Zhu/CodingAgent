package com.zhumeiyuan.codingagent.agent.model;

import java.util.List;

import com.zhumeiyuan.codingagent.agent.run.ToolCall;

public record ModelMessage(ModelRole role, String content, List<ToolCall> toolCalls, String toolCallId, String toolName) {

	public ModelMessage(ModelRole role, String content) {
		this(role, content, List.of(), "", "");
	}

	public ModelMessage {
		if (role == null) {
			throw new IllegalArgumentException("Model message role must not be null");
		}
		if (content == null) {
			throw new IllegalArgumentException("Model message content must not be null");
		}
		toolCalls = List.copyOf(toolCalls == null ? List.of() : toolCalls);
		toolCallId = toolCallId == null ? "" : toolCallId;
		toolName = toolName == null ? "" : toolName;
		if (role != ModelRole.ASSISTANT && !toolCalls.isEmpty()) {
			throw new IllegalArgumentException("Only assistant messages may carry tool calls");
		}
		if (role == ModelRole.TOOL && toolCallId.isBlank()) {
			throw new IllegalArgumentException("Tool messages must include toolCallId");
		}
		if (role != ModelRole.TOOL && (!toolCallId.isBlank() || !toolName.isBlank())) {
			throw new IllegalArgumentException("Only tool messages may carry tool result identifiers");
		}
	}

	public static ModelMessage system(String content) {
		return new ModelMessage(ModelRole.SYSTEM, content);
	}

	public static ModelMessage user(String content) {
		return new ModelMessage(ModelRole.USER, content);
	}

	public static ModelMessage assistant(String content) {
		return new ModelMessage(ModelRole.ASSISTANT, content);
	}

	public static ModelMessage assistant(String content, List<ToolCall> toolCalls) {
		return new ModelMessage(ModelRole.ASSISTANT, content, toolCalls, "", "");
	}

	public static ModelMessage tool(String content) {
		return new ModelMessage(ModelRole.TOOL, content, List.of(), "compat_tool_call", "");
	}

	public static ModelMessage tool(String toolCallId, String toolName, String content) {
		return new ModelMessage(ModelRole.TOOL, content, List.of(), toolCallId, toolName);
	}
}
