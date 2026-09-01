package com.zhumeiyuan.codingagent.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.zhumeiyuan.codingagent.agent.run.ToolCall;

import org.junit.jupiter.api.Test;

class ToolApprovalPolicyTests {

	private final ToolApprovalPolicy policy = new ToolApprovalPolicy();

	@Test
	void autoApprovesReadOnlyTools() {
		ToolApprovalDecision decision = this.policy.decide(new ToolCall("call-1", "read_file", Map.of("path", "README.md")));

		assertThat(decision.mode()).isEqualTo(ToolApprovalMode.AUTO_APPROVED);
		assertThat(decision.requiresUserApproval()).isFalse();
	}

	@Test
	void requiresApprovalForWorkspaceMutationsAndCommands() {
		assertThat(this.policy.decide(new ToolCall("call-1", "write_file", Map.of())).requiresUserApproval()).isTrue();
		assertThat(this.policy.decide(new ToolCall("call-2", "replace_text", Map.of())).requiresUserApproval()).isTrue();
		assertThat(this.policy.decide(new ToolCall("call-3", "edit_file", Map.of())).requiresUserApproval()).isTrue();
		assertThat(this.policy.decide(new ToolCall("call-4", "run_command", Map.of())).requiresUserApproval()).isTrue();
	}
}
