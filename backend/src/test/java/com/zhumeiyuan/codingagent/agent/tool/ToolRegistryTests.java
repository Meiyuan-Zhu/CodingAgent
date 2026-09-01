package com.zhumeiyuan.codingagent.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;

import org.junit.jupiter.api.Test;

class ToolRegistryTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T07:30:00Z"), ZoneOffset.UTC);
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void exposesDefinitionsInStableNameOrder() {
		ToolRegistry registry = new ToolRegistry(List.of(
				tool("search_text", arguments -> ToolExecutionResult.of("{}", Map.of())),
				tool("list_files", arguments -> ToolExecutionResult.of("{}", Map.of()))),
				this.clock);

		assertThat(registry.definitions()).extracting(ToolDefinition::name).containsExactly("list_files", "search_text");
	}

	@Test
	void rejectsDuplicateToolNames() {
		RegisteredTool first = tool("read_file", arguments -> ToolExecutionResult.of("{}", Map.of()));
		RegisteredTool second = tool("read_file", arguments -> ToolExecutionResult.of("{}", Map.of()));

		assertThatThrownBy(() -> new ToolRegistry(List.of(first, second), this.clock))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicate tool name");
	}

	@Test
	void executesKnownToolAndAddsBaseMetadata() {
		ToolRegistry registry = new ToolRegistry(List.of(
				tool("read_file", arguments -> ToolExecutionResult.of("content", Map.of("path", arguments.get("path"))))),
				this.clock);

		ToolResult result = registry.execute(new ToolCall("call-1", "read_file", Map.of("path", "README.md")));

		assertThat(result.success()).isTrue();
		assertThat(result.content()).isEqualTo("content");
		assertThat(result.completedAt()).isEqualTo(this.clock.instant());
		assertThat(result.metadata()).containsEntry("toolName", "read_file").containsEntry("path", "README.md");
	}

	@Test
	void unknownToolReturnsFailureResult() {
		ToolRegistry registry = new ToolRegistry(List.of(), this.clock);

		ToolResult result = registry.execute(new ToolCall("call-1", "missing_tool", Map.of()));

		assertThat(result.success()).isFalse();
		assertThat(result.content()).contains("Unknown tool");
		assertThat(result.metadata()).containsEntry("toolName", "missing_tool")
				.containsEntry("errorCode", ToolExecutionErrorCode.UNKNOWN_TOOL.name());
	}

	@Test
	void toolExecutionExceptionReturnsStructuredFailureResult() throws Exception {
		ToolRegistry registry = new ToolRegistry(List.of(
				tool("read_file", arguments -> {
					throw new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, "read_file", "bad args");
				})),
				this.clock);

		ToolResult result = registry.execute(new ToolCall("call-1", "read_file", Map.of()));

		assertThat(result.success()).isFalse();
		assertThat(this.objectMapper.readTree(result.content()).get("success").asBoolean()).isFalse();
		assertThat(this.objectMapper.readTree(result.content()).get("message").asText()).isEqualTo("bad args");
		assertThat(this.objectMapper.readTree(result.content()).get("errorCode").asText())
				.isEqualTo(ToolExecutionErrorCode.INVALID_ARGUMENTS.name());
		assertThat(this.objectMapper.readTree(result.content()).get("failureKind").asText())
				.isEqualTo("RECOVERABLE_TOOL_ERROR");
		assertThat(this.objectMapper.readTree(result.content()).get("recoverable").asBoolean()).isTrue();
		assertThat(this.objectMapper.readTree(result.content()).get("recoveryHint").asText()).contains("schema");
		assertThat(result.metadata()).containsEntry("toolName", "read_file")
				.containsEntry("errorCode", ToolExecutionErrorCode.INVALID_ARGUMENTS.name());
	}

	@Test
	void unexpectedRuntimeExceptionReturnsStructuredFailureResultWithoutStackTrace() throws Exception {
		ToolRegistry registry = new ToolRegistry(List.of(
				tool("read_file", arguments -> {
					throw new IllegalStateException("secret internal details");
				})),
				this.clock);

		ToolResult result = registry.execute(new ToolCall("call-1", "read_file", Map.of("path", "README.md")));

		assertThat(result.success()).isFalse();
		assertThat(result.content()).contains("Tool failed unexpectedly")
				.doesNotContain("secret internal details");
		assertThat(this.objectMapper.readTree(result.content()).get("success").asBoolean()).isFalse();
		assertThat(result.metadata()).containsEntry("errorCode", ToolExecutionErrorCode.TOOL_RUNTIME_ERROR.name());
	}

	private RegisteredTool tool(String name, ToolHandler handler) {
		return new RegisteredTool(new ToolDefinition(name, "Test tool", Map.of("type", "object")), handler);
	}
}
