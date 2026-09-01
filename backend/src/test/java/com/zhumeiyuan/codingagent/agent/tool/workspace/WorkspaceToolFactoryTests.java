package com.zhumeiyuan.codingagent.agent.tool.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.execution.WorkspaceChangeJournal;
import com.zhumeiyuan.codingagent.agent.execution.WorkspaceChangeUndoSnapshot;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionErrorCode;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspacePathResolver;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceCommandTools;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceReadTools;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceWriteTools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceToolFactoryTests {

	@TempDir
	Path tempDir;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Clock clock = Clock.fixed(Instant.parse("2026-08-27T08:00:00Z"), ZoneOffset.UTC);
	private ToolRegistry registry;

	@BeforeEach
	void setUp() throws IOException {
		Path root = this.tempDir.resolve("workspace");
		Files.createDirectories(root.resolve("src"));
		Files.writeString(root.resolve("README.md"), "hello agent\n");
		Files.writeString(root.resolve("src/App.java"), "class App { String name = \"agent\"; }\n");
		Files.writeString(root.resolve(".env"), "API_KEY=secret\n");
		WorkspacePathResolver resolver = new WorkspacePathResolver(root);
		WorkspaceReadTools workspaceReadTools = new WorkspaceReadTools(resolver);
		WorkspaceWriteTools workspaceWriteTools = new WorkspaceWriteTools(resolver);
		WorkspaceCommandTools workspaceCommandTools = new WorkspaceCommandTools(resolver, this.clock);
		this.registry = new ToolRegistry(List.of(
				WorkspaceToolFactory.listFiles(workspaceReadTools, this.objectMapper),
				WorkspaceToolFactory.readFile(workspaceReadTools, this.objectMapper),
					WorkspaceToolFactory.searchText(workspaceReadTools, this.objectMapper),
					WorkspaceToolFactory.writeFile(workspaceWriteTools, this.objectMapper),
					WorkspaceToolFactory.replaceText(workspaceWriteTools, this.objectMapper),
					WorkspaceToolFactory.editFile(workspaceWriteTools, this.objectMapper),
					WorkspaceToolFactory.runCommand(workspaceCommandTools, this.objectMapper)),
					this.clock);
	}

	@Test
	void registersWorkspaceReadToolDefinitions() {
		List<ToolDefinition> definitions = this.registry.definitions();

			assertThat(definitions).extracting(ToolDefinition::name)
					.containsExactly("edit_file", "list_files", "read_file", "replace_text", "run_command", "search_text",
							"write_file");
			ToolDefinition readFile = definitions.stream()
					.filter(definition -> definition.name().equals("read_file"))
					.findFirst()
					.orElseThrow();
			assertThat(readFile.inputSchema()).containsEntry("additionalProperties", false);
			assertThat(readFile.inputSchema()).containsEntry("required", List.of("path"));
	}

	@Test
	void listFilesReturnsJsonAndHidesSensitiveEntries() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "list_files", Map.of("path", ".")));

		assertThat(result.success()).isTrue();
		assertThat(result.metadata()).containsEntry("toolName", "list_files").containsEntry("path", ".");
			JsonNode json = this.objectMapper.readTree(result.content());
			assertThat(json.get("success").asBoolean()).isTrue();
			assertThat(json.get("message").asText()).isNotBlank();
			assertThat(json.get("root").asText()).isEqualTo(".");
		assertThat(json.get("files").toString()).contains("README.md", "src").doesNotContain(".env");
	}

	@Test
	void readFileReturnsJsonContent() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "read_file", Map.of("path", "README.md")));

		assertThat(result.success()).isTrue();
			JsonNode json = this.objectMapper.readTree(result.content());
			assertThat(json.get("success").asBoolean()).isTrue();
			assertThat(json.get("message").asText()).isNotBlank();
			assertThat(json.get("path").asText()).isEqualTo("README.md");
		assertThat(json.get("content").asText()).contains("hello agent");
	}

	@Test
	void searchTextReturnsJsonMatches() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "search_text",
				Map.of("query", "agent", "max_matches", 2)));

		assertThat(result.success()).isTrue();
		assertThat(result.metadata()).containsEntry("maxMatches", 2);
			JsonNode json = this.objectMapper.readTree(result.content());
			assertThat(json.get("success").asBoolean()).isTrue();
			assertThat(json.get("message").asText()).isNotBlank();
			assertThat(json.get("matches")).hasSize(2);
		assertThat(json.get("truncated").asBoolean()).isTrue();
	}

	@Test
	void invalidArgumentsReturnFailure() {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "search_text",
				Map.of("query", "agent", "max_matches", 999)));

		assertThat(result.success()).isFalse();
		assertThat(result.metadata()).containsEntry("errorCode", ToolExecutionErrorCode.INVALID_ARGUMENTS.name());
	}

	@Test
	void workspaceAccessDeniedReturnsFailure() {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "read_file", Map.of("path", "../secret.txt")));

		assertThat(result.success()).isFalse();
		assertThat(result.content()).contains("escapes the workspace root");
		assertThat(result.metadata()).containsEntry("errorCode", ToolExecutionErrorCode.WORKSPACE_INVALID_PATH.name());
	}

	@Test
	void missingWorkspacePathReturnsRecoverableNotFoundFailure() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "read_file", Map.of("path", "missing.py")));

		assertThat(result.success()).isFalse();
		assertThat(result.metadata()).containsEntry("errorCode", ToolExecutionErrorCode.WORKSPACE_NOT_FOUND.name());
		JsonNode json = this.objectMapper.readTree(result.content());
		assertThat(json.get("recoverable").asBoolean()).isTrue();
		assertThat(json.get("recoveryHint").asText()).contains("list_files");
	}

	@Test
	void writeFileToolCreatesFileAndReturnsHashMetadata() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "write_file",
				Map.of("path", "src/New.java", "content", "class New {}\n")));

		assertThat(result.success()).isTrue();
		assertThat(result.metadata()).containsEntry("toolName", "write_file").containsEntry("overwrite", false);
			JsonNode json = this.objectMapper.readTree(result.content());
			assertThat(json.get("success").asBoolean()).isTrue();
			assertThat(json.get("message").asText()).isNotBlank();
			assertThat(json.get("path").asText()).isEqualTo("src/New.java");
		assertThat(json.get("created").asBoolean()).isTrue();
		assertThat(json.get("sha256").asText()).hasSize(64);
		assertThat(json.get("unifiedDiff").asText()).contains("+class New {}");
		assertThat(json.has("previousContent")).isFalse();
		assertThat(result.privateMetadata().get(WorkspaceChangeJournal.UNDO_SNAPSHOT_KEY))
				.isInstanceOf(WorkspaceChangeUndoSnapshot.class);
	}

	@Test
	void replaceTextToolEditsFileAndCanUseEmptyReplacement() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "replace_text",
				Map.of("path", "README.md", "old_text", "hello ", "new_text", "")));

		assertThat(result.success()).isTrue();
			JsonNode json = this.objectMapper.readTree(result.content());
			assertThat(json.get("success").asBoolean()).isTrue();
			assertThat(json.get("message").asText()).isNotBlank();
			assertThat(json.get("path").asText()).isEqualTo("README.md");
		assertThat(json.get("replacements").asInt()).isEqualTo(1);
		assertThat(json.get("unifiedDiff").asText()).contains("-hello agent", "+agent");
		assertThat(json.has("previousContent")).isFalse();
		assertThat(result.privateMetadata().get(WorkspaceChangeJournal.UNDO_SNAPSHOT_KEY))
				.isInstanceOf(WorkspaceChangeUndoSnapshot.class);
	}

	@Test
	void editFileAliasesReplaceTextAndReturnsUndoMetadata() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "edit_file",
				Map.of("path", "README.md", "old_text", "hello", "new_text", "hi")));

		assertThat(result.success()).isTrue();
		assertThat(result.metadata()).containsEntry("toolName", "edit_file").containsEntry("path", "README.md");
		JsonNode json = this.objectMapper.readTree(result.content());
		assertThat(json.get("success").asBoolean()).isTrue();
		assertThat(json.get("path").asText()).isEqualTo("README.md");
		assertThat(json.get("replacements").asInt()).isEqualTo(1);
		assertThat(json.get("unifiedDiff").asText()).contains("-hello agent", "+hi agent");
		assertThat(result.privateMetadata().get(WorkspaceChangeJournal.UNDO_SNAPSHOT_KEY))
				.isInstanceOf(WorkspaceChangeUndoSnapshot.class);
	}

	@Test
	void runCommandToolReturnsExitCodeAndCapturedOutput() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "run_command",
				Map.of("command", List.of("/bin/echo", "hello"), "cwd", ".", "max_output_chars", 1000)));

		assertThat(result.success()).isTrue();
		assertThat(result.metadata()).containsEntry("toolName", "run_command").containsEntry("cwd", ".");
			JsonNode json = this.objectMapper.readTree(result.content());
			assertThat(json.get("success").asBoolean()).isTrue();
			assertThat(json.get("message").asText()).isEqualTo("Command completed successfully.");
			assertThat(json.get("timedOut").asBoolean()).isFalse();
			assertThat(json.get("exitCode").asInt()).isEqualTo(0);
			assertThat(json.get("stdout").asText()).contains("hello");
	}

	@Test
	void runCommandToolAcceptsJsonStringArrayFromModel() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "run_command",
				Map.of("command", "[\"/bin/echo\",\"hello\"]", "cwd", ".", "max_output_chars", 1000)));

		assertThat(result.success()).isTrue();
		JsonNode json = this.objectMapper.readTree(result.content());
		assertThat(json.get("command").toString()).isEqualTo("[\"/bin/echo\",\"hello\"]");
		assertThat(json.get("stdout").asText()).contains("hello");
	}

	@Test
	void runCommandToolStillRejectsShellCommandString() throws Exception {
		ToolResult result = this.registry.execute(new ToolCall("call-1", "run_command",
				Map.of("command", "which g++", "cwd", ".")));

		assertThat(result.success()).isFalse();
		assertThat(result.metadata()).containsEntry("errorCode", ToolExecutionErrorCode.INVALID_ARGUMENTS.name());
		JsonNode json = this.objectMapper.readTree(result.content());
		assertThat(json.get("message").asText()).contains("argv array");
	}

	@Test
	void writeConflictAndMissingTextUseSpecificToolErrorCodes() {
		ToolResult conflict = this.registry.execute(new ToolCall("call-1", "write_file",
				Map.of("path", "README.md", "content", "changed", "overwrite", false)));
		ToolResult editMiss = this.registry.execute(new ToolCall("call-2", "replace_text",
				Map.of("path", "README.md", "old_text", "missing", "new_text", "changed")));

		assertThat(conflict.success()).isFalse();
		assertThat(conflict.metadata()).containsEntry("errorCode", ToolExecutionErrorCode.WORKSPACE_CONFLICT.name());
		assertThat(editMiss.success()).isFalse();
		assertThat(editMiss.metadata()).containsEntry("errorCode", ToolExecutionErrorCode.WORKSPACE_EDIT_MISS.name());
	}
}
