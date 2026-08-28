package com.zhumeiyuan.codingagent.agent.execution;

import java.util.List;
import java.util.Objects;

import com.zhumeiyuan.codingagent.agent.model.ModelMessage;
import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalDecision;

public record PendingToolApproval(
		RunId runId,
		int round,
		ToolCall toolCall,
		ToolApprovalDecision decision,
		List<ModelMessage> messages,
		int toolCallsUsed) {

	public PendingToolApproval {
		Objects.requireNonNull(runId, "runId");
		if (round < 1) {
			throw new IllegalArgumentException("Approval round must be positive");
		}
		Objects.requireNonNull(toolCall, "toolCall");
		Objects.requireNonNull(decision, "decision");
		messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
		if (messages.isEmpty()) {
			throw new IllegalArgumentException("Approval continuation messages must not be empty");
		}
		if (toolCallsUsed < 0) {
			throw new IllegalArgumentException("Tool calls used must not be negative");
		}
	}
}
