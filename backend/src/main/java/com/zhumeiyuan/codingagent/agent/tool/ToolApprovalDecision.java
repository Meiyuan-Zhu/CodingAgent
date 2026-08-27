package com.zhumeiyuan.codingagent.agent.tool;

import java.util.Objects;

public record ToolApprovalDecision(ToolApprovalMode mode, String reason) {

	public ToolApprovalDecision {
		Objects.requireNonNull(mode, "mode");
		if (reason == null || reason.isBlank()) {
			throw new IllegalArgumentException("Tool approval reason must not be blank");
		}
	}

	public boolean requiresUserApproval() {
		return this.mode == ToolApprovalMode.USER_APPROVAL_REQUIRED;
	}
}
