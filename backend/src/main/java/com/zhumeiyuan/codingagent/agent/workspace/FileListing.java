package com.zhumeiyuan.codingagent.agent.workspace;

import java.util.List;
import java.util.Objects;

public record FileListing(String root, List<ListedWorkspaceFile> files, boolean truncated) {

	public FileListing {
		if (root == null || root.isBlank()) {
			throw new IllegalArgumentException("Listing root must not be blank");
		}
		files = List.copyOf(Objects.requireNonNull(files, "files"));
	}
}
