package com.zhumeiyuan.codingagent.agent.workspace;

import java.util.List;
import java.util.Objects;

public record CommandExecutionResult(List<String> command, String cwd, int exitCode, String stdout, String stderr,
		boolean stdoutTruncated, boolean stderrTruncated, long durationMillis) {

	public CommandExecutionResult {
		command = List.copyOf(Objects.requireNonNull(command, "command"));
		Objects.requireNonNull(cwd, "cwd");
		Objects.requireNonNull(stdout, "stdout");
		Objects.requireNonNull(stderr, "stderr");
	}
}
