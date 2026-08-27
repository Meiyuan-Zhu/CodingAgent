package com.zhumeiyuan.codingagent.agent.workspace;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.workspace")
public class WorkspaceProperties {

	private String root = "../workspaces/demo";

	public Path getRoot() {
		return Path.of(this.root);
	}

	public void setRoot(String root) {
		this.root = root;
	}
}
