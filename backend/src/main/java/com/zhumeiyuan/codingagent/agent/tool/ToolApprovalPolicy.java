package com.zhumeiyuan.codingagent.agent.tool;

import java.util.Map;
import java.util.Objects;

import com.zhumeiyuan.codingagent.agent.run.ToolCall;

public class ToolApprovalPolicy {

	private static final Map<String, String> APPROVAL_REASONS = Map.of(
			"write_file", "Writing a file changes the workspace and requires user approval.",
			"replace_text", "Editing a file changes the workspace and requires user approval.",
			"edit_file", "Editing a file changes the workspace and requires user approval.",
			"run_command", "Running a local command may change the workspace or host and requires user approval.");

	public ToolApprovalDecision decide(ToolCall call) {
		Objects.requireNonNull(call, "call");
		String reason = APPROVAL_REASONS.get(call.name());
		if (reason != null) {
			return new ToolApprovalDecision(ToolApprovalMode.USER_APPROVAL_REQUIRED, reason);
		}
		return new ToolApprovalDecision(ToolApprovalMode.AUTO_APPROVED,
				"Read-only or unknown tools may proceed to normal registry validation.");
	}
}
