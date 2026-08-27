package com.zhumeiyuan.codingagent.agent.workspace;

import java.util.ArrayList;
import java.util.List;

final class WorkspaceUnifiedDiff {

	private static final int MAX_DIFF_LINES = 240;

	private WorkspaceUnifiedDiff() {
	}

	static String create(String displayPath, String previousText, String nextText) {
		List<String> previousLines = lines(previousText);
		List<String> nextLines = lines(nextText);
		StringBuilder diff = new StringBuilder();
		diff.append("--- a/").append(displayPath).append('\n');
		diff.append("+++ b/").append(displayPath).append('\n');
		diff.append("@@ -1,").append(previousLines.size()).append(" +1,").append(nextLines.size()).append(" @@\n");

		int emitted = 0;
		int max = Math.max(previousLines.size(), nextLines.size());
		for (int index = 0; index < max; index++) {
			String previous = index < previousLines.size() ? previousLines.get(index) : null;
			String next = index < nextLines.size() ? nextLines.get(index) : null;
			if (previous != null && previous.equals(next)) {
				emitted = append(diff, ' ', previous, emitted);
			} else {
				if (previous != null) {
					emitted = append(diff, '-', previous, emitted);
				}
				if (next != null) {
					emitted = append(diff, '+', next, emitted);
				}
			}
			if (emitted >= MAX_DIFF_LINES) {
				diff.append("... diff truncated after ").append(MAX_DIFF_LINES).append(" lines\n");
				break;
			}
		}
		return diff.toString();
	}

	private static int append(StringBuilder diff, char prefix, String line, int emitted) {
		if (emitted >= MAX_DIFF_LINES) {
			return emitted;
		}
		diff.append(prefix).append(line).append('\n');
		return emitted + 1;
	}

	private static List<String> lines(String text) {
		if (text.isEmpty()) {
			return List.of();
		}
		String[] split = text.split("\\R", -1);
		List<String> lines = new ArrayList<>(List.of(split));
		if (text.endsWith("\n") || text.endsWith("\r")) {
			lines.remove(lines.size() - 1);
		}
		return List.copyOf(lines);
	}
}
