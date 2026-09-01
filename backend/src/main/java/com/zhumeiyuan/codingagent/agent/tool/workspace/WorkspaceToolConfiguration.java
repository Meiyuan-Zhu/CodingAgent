package com.zhumeiyuan.codingagent.agent.tool.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceCommandTools;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceReadTools;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceWriteTools;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class WorkspaceToolConfiguration {

	@Bean
	RegisteredTool listFilesTool(WorkspaceReadTools workspaceReadTools, ObjectMapper objectMapper) {
		return WorkspaceToolFactory.listFiles(workspaceReadTools, objectMapper);
	}

	@Bean
	RegisteredTool readFileTool(WorkspaceReadTools workspaceReadTools, ObjectMapper objectMapper) {
		return WorkspaceToolFactory.readFile(workspaceReadTools, objectMapper);
	}

	@Bean
	RegisteredTool searchTextTool(WorkspaceReadTools workspaceReadTools, ObjectMapper objectMapper) {
		return WorkspaceToolFactory.searchText(workspaceReadTools, objectMapper);
	}

	@Bean
	RegisteredTool writeFileTool(WorkspaceWriteTools workspaceWriteTools, ObjectMapper objectMapper) {
		return WorkspaceToolFactory.writeFile(workspaceWriteTools, objectMapper);
	}

	@Bean
	RegisteredTool replaceTextTool(WorkspaceWriteTools workspaceWriteTools, ObjectMapper objectMapper) {
		return WorkspaceToolFactory.replaceText(workspaceWriteTools, objectMapper);
	}

	@Bean
	RegisteredTool editFileTool(WorkspaceWriteTools workspaceWriteTools, ObjectMapper objectMapper) {
		return WorkspaceToolFactory.editFile(workspaceWriteTools, objectMapper);
	}

	@Bean
	RegisteredTool runCommandTool(WorkspaceCommandTools workspaceCommandTools, ObjectMapper objectMapper) {
		return WorkspaceToolFactory.runCommand(workspaceCommandTools, objectMapper);
	}
}
