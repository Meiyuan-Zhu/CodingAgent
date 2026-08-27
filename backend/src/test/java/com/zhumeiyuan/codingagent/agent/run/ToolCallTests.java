package com.zhumeiyuan.codingagent.agent.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ToolCallTests {

	@Test
	void toolCallArgumentsAreImmutableSnapshot() {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put("path", "src/App.vue");

		ToolCall call = new ToolCall("call-1", "read_file", arguments);
		arguments.put("path", "changed");

		assertThat(call.arguments()).containsEntry("path", "src/App.vue");
		assertThatThrownBy(() -> call.arguments().put("other", "value"))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void toolResultCarriesSuccessOrFailure() {
		Instant completedAt = Instant.parse("2026-08-27T06:00:00Z");

		ToolResult success = ToolResult.success("call-1", "file content", Map.of("bytes", 12), completedAt);
		ToolResult failure = ToolResult.failure("call-2", "file not found", Map.of("code", "NOT_FOUND"),
				completedAt);

		assertThat(success.success()).isTrue();
		assertThat(success.metadata()).containsEntry("bytes", 12);
		assertThat(failure.success()).isFalse();
		assertThat(failure.content()).isEqualTo("file not found");
	}

	@Test
	void blankToolNamesAreRejected() {
		assertThatThrownBy(() -> new ToolCall("call-1", " ", Map.of())).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ToolResult.success("", "content", Map.of(), Instant.now()))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
