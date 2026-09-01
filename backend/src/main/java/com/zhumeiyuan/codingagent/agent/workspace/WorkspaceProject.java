package com.zhumeiyuan.codingagent.agent.workspace;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record WorkspaceProject(
		String id,
		String name,
		Path path,
		Instant createdAt,
		boolean active) {

	public WorkspaceProject {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("Project id must not be blank");
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Project name must not be blank");
		}
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(createdAt, "createdAt");
	}
}
