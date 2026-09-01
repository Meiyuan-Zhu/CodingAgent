package com.zhumeiyuan.codingagent.agent.workspace;

import java.util.List;
import java.util.Objects;

public record FileListing(boolean success, String message, String root, List<ListedWorkspaceFile> files, boolean truncated) {

	public FileListing {
		if (!success) {
			throw new IllegalArgumentException("File listing result must be successful");
		}
		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("File listing message must not be blank");
		}
		if (root == null || root.isBlank()) {
			throw new IllegalArgumentException("Listing root must not be blank");
		}
		files = List.copyOf(Objects.requireNonNull(files, "files"));
	}
}
