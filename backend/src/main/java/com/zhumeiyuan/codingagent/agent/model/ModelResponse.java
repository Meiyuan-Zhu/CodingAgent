package com.zhumeiyuan.codingagent.agent.model;

import java.util.List;
import java.util.Objects;

import com.zhumeiyuan.codingagent.agent.run.ToolCall;

public record ModelResponse(String message, ModelFinishReason finishReason, List<ToolCall> toolCalls) {

	public ModelResponse {
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(finishReason, "finishReason");
		toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls"));
		if (finishReason == ModelFinishReason.TOOL_CALLS && toolCalls.isEmpty()) {
			throw new IllegalArgumentException("TOOL_CALLS response must include at least one tool call");
		}
		if (finishReason != ModelFinishReason.TOOL_CALLS && !toolCalls.isEmpty()) {
			throw new IllegalArgumentException("Only TOOL_CALLS responses may include tool calls");
		}
	}
}
