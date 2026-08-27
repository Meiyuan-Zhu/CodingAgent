package com.zhumeiyuan.codingagent.agent.workspace;

import java.util.List;
import java.util.Objects;

public record SearchResult(String query, List<SearchMatch> matches, boolean truncated) {

	public SearchResult {
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException("Search query must not be blank");
		}
		matches = List.copyOf(Objects.requireNonNull(matches, "matches"));
	}
}
