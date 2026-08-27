package com.zhumeiyuan.codingagent.agent.workspace;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.workspace")
public class WorkspaceProperties {

	private Path root = Path.of("../workspaces/demo");

	public Path getRoot() {
		return this.root;
	}

	public void setRoot(Path root) {
		this.root = root;
	}
}
