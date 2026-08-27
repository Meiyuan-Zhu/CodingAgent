package com.zhumeiyuan.codingagent.agent.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRunRequest(
		@NotBlank(message = "prompt must not be blank")
		@Size(max = 4000, message = "prompt must not exceed 4000 characters")
		String prompt) {
}
