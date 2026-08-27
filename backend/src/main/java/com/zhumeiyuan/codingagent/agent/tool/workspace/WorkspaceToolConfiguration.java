package com.zhumeiyuan.codingagent.agent.tool.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.tool.RegisteredTool;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceReadTools;

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
}
