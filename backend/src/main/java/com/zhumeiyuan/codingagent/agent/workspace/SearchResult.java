package com.zhumeiyuan.codingagent.agent.workspace;

import java.util.List;
import java.util.Objects;

public record SearchResult(boolean success, String message, String query, List<SearchMatch> matches, boolean truncated) {

	public SearchResult {
		if (!success) {
			throw new IllegalArgumentException("Search result must be successful");
		}
		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("Search result message must not be blank");
		}
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException("Search query must not be blank");
		}
		matches = List.copyOf(Objects.requireNonNull(matches, "matches"));
	}
}
