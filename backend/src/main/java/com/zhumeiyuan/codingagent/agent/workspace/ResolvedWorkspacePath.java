package com.zhumeiyuan.codingagent.agent.workspace;

import java.nio.file.Path;
import java.util.Objects;

public record ResolvedWorkspacePath(Path relativePath, Path realPath) {

	public ResolvedWorkspacePath {
		Objects.requireNonNull(relativePath, "relativePath");
		Objects.requireNonNull(realPath, "realPath");
	}

	public String displayPath() {
		String path = this.relativePath.toString();
		return path.isBlank() ? "." : path.replace('\\', '/');
	}
}
