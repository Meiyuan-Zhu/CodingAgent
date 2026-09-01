package com.zhumeiyuan.codingagent.agent.workspace;

import java.util.List;
import java.util.Objects;

public record CommandExecutionResult(boolean success, String message, List<String> command, String cwd, int exitCode,
		String stdout, String stderr, boolean stdoutTruncated, boolean stderrTruncated, boolean timedOut,
		long durationMillis) {

	public CommandExecutionResult {
		Objects.requireNonNull(message, "message");
		command = List.copyOf(Objects.requireNonNull(command, "command"));
		Objects.requireNonNull(cwd, "cwd");
		Objects.requireNonNull(stdout, "stdout");
		Objects.requireNonNull(stderr, "stderr");
	}
}
