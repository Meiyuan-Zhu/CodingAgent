package com.zhumeiyuan.codingagent.agent.tool.workspace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.tool.ToolArgumentReader;
import com.zhumeiyuan.codingagent.agent.tool.ToolDefinition;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionErrorCode;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionException;
import com.zhumeiyuan.codingagent.agent.tool.ToolExecutionResult;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceAccessException;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceReadTools;

public final class WorkspaceToolFactory {

	private static final int MAX_LIST_ENTRIES = 500;
	private static final int MAX_SEARCH_MATCHES = 200;

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

	private static ToolExecutionResult workspaceJson(String toolName, ObjectMapper objectMapper, Supplier<Object> result,
			Map<String, Object> metadata) {
		try {
			return ToolExecutionResult.of(objectMapper.writeValueAsString(result.get()), metadata);
		} catch (JsonProcessingException ex) {
			throw new ToolExecutionException(ToolExecutionErrorCode.TOOL_RUNTIME_ERROR, toolName,
					"Cannot serialize tool result", ex);
		} catch (WorkspaceAccessException ex) {
			throw workspaceDenied(toolName, ex);
		}
	}

	private static ToolExecutionException workspaceDenied(String toolName, WorkspaceAccessException ex) {
		return new ToolExecutionException(ToolExecutionErrorCode.WORKSPACE_ACCESS_DENIED, toolName,
				ex.getMessage(), ex);
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

	private static Map<String, Object> integerProperty(String description, int minimum, int maximum) {
		return Map.of("type", "integer", "description", description, "minimum", minimum, "maximum", maximum);
	}
}
