package com.zhumeiyuan.codingagent.agent.tool.workspace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.tool.ToolArgumentReader;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionErrorCode;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionException;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionResult;
import com.zhumeiyuan.codingagent.agent.execution.WorkspaceChangeJournal;
import com.zhumeiyuan.codingagent.agent.execution.WorkspaceChangeUndoSnapshot;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceAccessCode;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceAccessException;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceReadTools;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceCommandTools;
import com.zhumeiyuan.codingagent.agent.workspace.TextReplacementResult;
import com.zhumeiyuan.codingagent.agent.workspace.WriteFileResult;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceWriteTools;

public final class WorkspaceToolFactory {

	private static final int MAX_LIST_ENTRIES = 500;
	private static final int MAX_SEARCH_MATCHES = 200;
	private static final int MAX_REPLACEMENTS = 200;
	private static final int MAX_COMMAND_PARTS = 64;

	private WorkspaceToolFactory() {
	}

	public static RegisteredTool listFiles(WorkspaceReadTools workspaceReadTools, ObjectMapper objectMapper) {
		String toolName = "list_files";
		return new RegisteredTool(
				new ToolDefinition(toolName, "List files in a directory inside the configured workspace.",
						objectSchema(Map.of(
								"path", stringProperty("Directory path relative to the workspace root. Use '.' for the root."),
								"max_entries", integerProperty("Maximum number of entries to return.", 1, MAX_LIST_ENTRIES)),
								List.of())),
				arguments -> {
					ToolArgumentReader reader = new ToolArgumentReader(toolName, arguments);
					reader.rejectUnexpected(Set.of("path", "max_entries"));
					String path = reader.optionalString("path", ".");
					int maxEntries = reader.optionalPositiveInt("max_entries", 200, MAX_LIST_ENTRIES);
					return workspaceJson(toolName, objectMapper, () -> workspaceReadTools.listFiles(path, maxEntries),
							Map.of("path", path, "maxEntries", maxEntries));
				});
	}

	public static RegisteredTool readFile(WorkspaceReadTools workspaceReadTools, ObjectMapper objectMapper) {
		String toolName = "read_file";
		return new RegisteredTool(
				new ToolDefinition(toolName, "Read a UTF-8 text file inside the configured workspace.",
						objectSchema(Map.of(
								"path", stringProperty("File path relative to the workspace root.")),
								List.of("path"))),
				arguments -> {
					ToolArgumentReader reader = new ToolArgumentReader(toolName, arguments);
					reader.rejectUnexpected(Set.of("path"));
					String path = reader.requiredString("path");
					return workspaceJson(toolName, objectMapper, () -> workspaceReadTools.readFile(path), Map.of("path", path));
				});
	}

	public static RegisteredTool searchText(WorkspaceReadTools workspaceReadTools, ObjectMapper objectMapper) {
		String toolName = "search_text";
		return new RegisteredTool(
				new ToolDefinition(toolName, "Search UTF-8 text files inside the configured workspace.",
						objectSchema(Map.of(
								"query", stringProperty("Text to search for. Matching is case-sensitive substring search."),
								"max_matches", integerProperty("Maximum number of matches to return.", 1, MAX_SEARCH_MATCHES)),
								List.of("query"))),
				arguments -> {
					ToolArgumentReader reader = new ToolArgumentReader(toolName, arguments);
					reader.rejectUnexpected(Set.of("query", "max_matches"));
					String query = reader.requiredString("query");
					int maxMatches = reader.optionalPositiveInt("max_matches", 100, MAX_SEARCH_MATCHES);
					return workspaceJson(toolName, objectMapper, () -> workspaceReadTools.searchText(query, maxMatches),
							Map.of("query", query, "maxMatches", maxMatches));
				});
	}

	public static RegisteredTool writeFile(WorkspaceWriteTools workspaceWriteTools, ObjectMapper objectMapper) {
		String toolName = "write_file";
		return new RegisteredTool(
				new ToolDefinition(toolName,
						"Create or overwrite a UTF-8 text file inside the configured workspace.",
						objectSchema(Map.of(
								"path", stringProperty("File path relative to the workspace root."),
								"content", stringProperty("Full UTF-8 text content to write."),
								"overwrite", booleanProperty("Whether an existing file may be overwritten. Defaults to false."),
								"expected_sha256", stringProperty("Optional SHA-256 hash of the existing file for conflict detection.")),
								List.of("path", "content"))),
				arguments -> {
					ToolArgumentReader reader = new ToolArgumentReader(toolName, arguments);
					reader.rejectUnexpected(Set.of("path", "content", "overwrite", "expected_sha256"));
					String path = reader.requiredString("path");
					String content = reader.requiredText("content");
					boolean overwrite = reader.optionalBoolean("overwrite", false);
					String expectedSha256 = reader.optionalString("expected_sha256", "");
					return workspaceJson(toolName, objectMapper,
							() -> workspaceWriteTools.writeFile(path, content, overwrite, expectedSha256),
							Map.of("path", path, "overwrite", overwrite), WorkspaceToolFactory::writeUndoMetadata);
				});
	}

	public static RegisteredTool replaceText(WorkspaceWriteTools workspaceWriteTools, ObjectMapper objectMapper) {
		return textEditTool("replace_text", "Replace exact text in a UTF-8 file inside the configured workspace.",
				workspaceWriteTools, objectMapper);
	}

	public static RegisteredTool editFile(WorkspaceWriteTools workspaceWriteTools, ObjectMapper objectMapper) {
		return textEditTool("edit_file",
				"Edit a UTF-8 file inside the configured workspace by replacing exact text. This is an alias of replace_text.",
				workspaceWriteTools, objectMapper);
	}

	private static RegisteredTool textEditTool(String toolName, String description, WorkspaceWriteTools workspaceWriteTools,
			ObjectMapper objectMapper) {
		return new RegisteredTool(
				new ToolDefinition(toolName,
						description,
						objectSchema(Map.of(
								"path", stringProperty("File path relative to the workspace root."),
								"old_text", stringProperty("Exact non-empty text to replace."),
								"new_text", stringProperty("Replacement text. It may be empty to delete text."),
								"expected_sha256", stringProperty("Optional SHA-256 hash of the current file for conflict detection."),
								"max_replacements", integerProperty("Maximum number of replacements to perform.", 1,
										MAX_REPLACEMENTS)),
								List.of("path", "old_text", "new_text"))),
				arguments -> {
					ToolArgumentReader reader = new ToolArgumentReader(toolName, arguments);
					reader.rejectUnexpected(Set.of("path", "old_text", "new_text", "expected_sha256", "max_replacements"));
					String path = reader.requiredString("path");
					String oldText = reader.requiredString("old_text");
					String newText = reader.requiredText("new_text");
					String expectedSha256 = reader.optionalString("expected_sha256", "");
					int maxReplacements = reader.optionalPositiveInt("max_replacements", 1, MAX_REPLACEMENTS);
					return workspaceJson(toolName, objectMapper,
								() -> workspaceWriteTools.replaceText(path, oldText, newText, expectedSha256, maxReplacements),
								Map.of("path", path, "maxReplacements", maxReplacements), WorkspaceToolFactory::replaceUndoMetadata);
				});
	}

	public static RegisteredTool runCommand(WorkspaceCommandTools workspaceCommandTools, ObjectMapper objectMapper) {
		String toolName = "run_command";
		return new RegisteredTool(
				new ToolDefinition(toolName,
						"Run an approved command in a directory inside the configured workspace. The command is an argv array and is not executed through a shell.",
						objectSchema(Map.of(
								"command", arrayProperty("Command and arguments as an argv array, for example [\"mvn\",\"test\"]. Shell operators like && or | are not interpreted."),
								"cwd", stringProperty("Working directory relative to the workspace root. Defaults to '.'."),
								"max_output_chars", integerProperty("Maximum characters captured separately from stdout and stderr.", 1,
										workspaceCommandTools.maxOutputChars())),
								List.of("command"))),
				arguments -> {
					ToolArgumentReader reader = new ToolArgumentReader(toolName, arguments);
					reader.rejectUnexpected(Set.of("command", "cwd", "max_output_chars"));
					List<String> command = commandArgument(toolName, reader, objectMapper);
					String cwd = reader.optionalString("cwd", ".");
					int maxOutputChars = reader.optionalPositiveInt("max_output_chars",
							workspaceCommandTools.defaultMaxOutputChars(), workspaceCommandTools.maxOutputChars());
					return workspaceJson(toolName, objectMapper,
							() -> workspaceCommandTools.runCommand(command, cwd, maxOutputChars),
							Map.of("cwd", cwd, "maxOutputChars", maxOutputChars));
				});
	}

	private static List<String> commandArgument(String toolName, ToolArgumentReader reader, ObjectMapper objectMapper) {
		Object rawCommand = reader.raw("command");
		if (rawCommand instanceof String text) {
			String trimmed = text.trim();
			if (!trimmed.startsWith("[")) {
				throw new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, toolName,
						"Argument 'command' must be an argv array, not a shell command string");
			}
			try {
				List<String> parsed = commandArrayFromJson(toolName, objectMapper.readTree(trimmed));
				return validateCommandArray(toolName, parsed);
			} catch (JsonProcessingException ex) {
				throw new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, toolName,
						"Argument 'command' string must contain a JSON string array", ex);
			}
		}
		return reader.requiredStringList("command", MAX_COMMAND_PARTS);
	}

	private static List<String> commandArrayFromJson(String toolName, JsonNode node) {
		if (!node.isArray()) {
			throw new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, toolName,
					"Argument 'command' string must contain a JSON string array");
		}
		List<String> command = new java.util.ArrayList<>();
		for (JsonNode item : node) {
			if (!item.isTextual()) {
				throw new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, toolName,
						"Argument 'command' must contain only non-blank strings");
			}
			command.add(item.asText());
		}
		return command;
	}

	private static List<String> validateCommandArray(String toolName, List<String> command) {
		if (command == null || command.isEmpty()) {
			throw new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, toolName,
					"Argument 'command' must be a non-empty string array");
		}
		if (command.size() > MAX_COMMAND_PARTS) {
			throw new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, toolName,
					"Argument 'command' must contain at most " + MAX_COMMAND_PARTS + " items");
		}
		for (String part : command) {
			if (part == null || part.isBlank()) {
				throw new ToolExecutionException(ToolExecutionErrorCode.INVALID_ARGUMENTS, toolName,
						"Argument 'command' must contain only non-blank strings");
			}
		}
		return List.copyOf(command);
	}

	private static ToolExecutionResult workspaceJson(String toolName, ObjectMapper objectMapper, Supplier<Object> result,
			Map<String, Object> metadata) {
		return workspaceJson(toolName, objectMapper, result, metadata, ignored -> Map.of());
	}

	private static ToolExecutionResult workspaceJson(String toolName, ObjectMapper objectMapper, Supplier<Object> result,
			Map<String, Object> metadata, java.util.function.Function<Object, Map<String, Object>> privateMetadata) {
		try {
			Object value = result.get();
			return ToolExecutionResult.of(objectMapper.writeValueAsString(value), metadata, privateMetadata.apply(value));
		} catch (JsonProcessingException ex) {
			throw new ToolExecutionException(ToolExecutionErrorCode.TOOL_RUNTIME_ERROR, toolName,
					"Cannot serialize tool result", ex);
		} catch (WorkspaceAccessException ex) {
			throw workspaceDenied(toolName, ex);
		}
	}

	private static Map<String, Object> writeUndoMetadata(Object result) {
		WriteFileResult write = (WriteFileResult) result;
		return Map.of(WorkspaceChangeJournal.UNDO_SNAPSHOT_KEY, new WorkspaceChangeUndoSnapshot(write.path(),
				write.created(), write.previousContent(), write.sha256()));
	}

	private static Map<String, Object> replaceUndoMetadata(Object result) {
		TextReplacementResult replacement = (TextReplacementResult) result;
		return Map.of(WorkspaceChangeJournal.UNDO_SNAPSHOT_KEY, new WorkspaceChangeUndoSnapshot(replacement.path(),
				false, replacement.previousContent(), replacement.sha256()));
	}

	private static ToolExecutionException workspaceDenied(String toolName, WorkspaceAccessException ex) {
		return new ToolExecutionException(toolErrorCode(ex.code()), toolName, ex.getMessage(), ex);
	}

	private static ToolExecutionErrorCode toolErrorCode(WorkspaceAccessCode workspaceCode) {
		return switch (workspaceCode) {
			case NOT_FOUND, PARENT_NOT_FOUND -> ToolExecutionErrorCode.WORKSPACE_NOT_FOUND;
			case BLANK_PATH, ABSOLUTE_PATH, PATH_ESCAPE, INVALID_WRITE_TARGET, SYMLINK_ESCAPE ->
				ToolExecutionErrorCode.WORKSPACE_INVALID_PATH;
			case SENSITIVE_PATH -> ToolExecutionErrorCode.WORKSPACE_PERMISSION_DENIED;
			case CONTENT_CONFLICT, FILE_ALREADY_EXISTS -> ToolExecutionErrorCode.WORKSPACE_CONFLICT;
			case TEXT_NOT_FOUND -> ToolExecutionErrorCode.WORKSPACE_EDIT_MISS;
			default -> ToolExecutionErrorCode.WORKSPACE_ACCESS_DENIED;
		};
	}

	private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("additionalProperties", false);
		schema.put("properties", properties);
		schema.put("required", required);
		return schema;
	}

	private static Map<String, Object> stringProperty(String description) {
		return Map.of("type", "string", "description", description);
	}

	private static Map<String, Object> arrayProperty(String description) {
		return Map.of("type", "array", "description", description, "minItems", 1, "maxItems", 64, "items",
				Map.of("type", "string"));
	}

	private static Map<String, Object> integerProperty(String description, int minimum, int maximum) {
		return Map.of("type", "integer", "description", description, "minimum", minimum, "maximum", maximum);
	}

	private static Map<String, Object> booleanProperty(String description) {
		return Map.of("type", "boolean", "description", description);
	}
}
