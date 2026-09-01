package com.zhumeiyuan.codingagent.agent.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class WorkspaceCommandTools {

	private static final int MAX_COMMAND_PARTS = 64;
	private static final int MAX_COMMAND_PART_CHARS = 500;
	private static final int DEFAULT_MAX_OUTPUT_CHARS = 12_000;
	private static final int MAX_OUTPUT_CHARS = 20_000;
	private static final String DEFAULT_PATH = "/opt/homebrew/opt/llvm/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin";
	private static final Map<String, String> MACOS_COMPILER_ALIASES = Map.of(
			"g++", "/opt/homebrew/bin/g++-15",
			"gcc", "/opt/homebrew/bin/gcc-15");

	private final WorkspacePathResolver resolver;
	private final Clock clock;
	private final Function<String, String> environment;

	public WorkspaceCommandTools(WorkspacePathResolver resolver, Clock clock) {
		this(resolver, clock, System::getenv);
	}

	WorkspaceCommandTools(WorkspacePathResolver resolver, Clock clock, Function<String, String> environment) {
		this.resolver = Objects.requireNonNull(resolver, "resolver");
		this.clock = Objects.requireNonNull(clock, "clock");
		this.environment = Objects.requireNonNull(environment, "environment");
	}

	public CommandExecutionResult runCommand(List<String> command, String cwd, int maxOutputChars) {
		List<String> safeCommand = resolveCommandAliases(validateCommand(command));
		int outputLimit = outputLimit(maxOutputChars);
		ResolvedWorkspacePath resolvedCwd = this.resolver.resolveExisting(cwd == null || cwd.isBlank() ? "." : cwd);
		Path workingDirectory = resolvedCwd.realPath();
		if (!Files.isDirectory(workingDirectory, LinkOption.NOFOLLOW_LINKS)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.PARENT_NOT_DIRECTORY, cwd,
					"Command working directory must be a directory");
		}

		Instant started = this.clock.instant();
		Process process = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(safeCommand);
			builder.directory(workingDirectory.toFile());
			configureEnvironment(builder.environment(), workingDirectory);
			process = builder.start();
			BoundedText stdout = new BoundedText(outputLimit);
			BoundedText stderr = new BoundedText(outputLimit);
			Thread stdoutThread = drainAsync(process.getInputStream(), stdout);
			Thread stderrThread = drainAsync(process.getErrorStream(), stderr);
			int exitCode = waitForProcess(process);
			join(stdoutThread);
			join(stderrThread);
			return new CommandExecutionResult(exitCode == 0, commandMessage(exitCode), safeCommand,
					resolvedCwd.displayPath(), exitCode, stdout.content(), stderr.content(), stdout.truncated(),
					stderr.truncated(), false, Duration.between(started, this.clock.instant()).toMillis());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			if (process != null) {
				destroyProcessTree(process);
			}
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, String.join(" ", safeCommand),
					"Command execution was interrupted", ex);
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, String.join(" ", safeCommand),
					"Cannot start command", ex);
		}
	}

	public int defaultMaxOutputChars() {
		return DEFAULT_MAX_OUTPUT_CHARS;
	}

	public int maxOutputChars() {
		return MAX_OUTPUT_CHARS;
	}

	private List<String> validateCommand(List<String> command) {
		if (command == null || command.isEmpty()) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.BLANK_PATH, "command", "Command must contain at least one part");
		}
		if (command.size() > MAX_COMMAND_PARTS) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, "command", "Command has too many parts");
		}
		for (String part : command) {
			if (part == null || part.isBlank()) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.BLANK_PATH, "command", "Command parts must not be blank");
			}
			if (part.indexOf('\0') >= 0 || part.length() > MAX_COMMAND_PART_CHARS) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.PATH_ESCAPE, "command",
						"Command parts contain blocked characters or are too long");
			}
		}
		return List.copyOf(command);
	}

	private int outputLimit(int maxOutputChars) {
		if (maxOutputChars <= 0) {
			return DEFAULT_MAX_OUTPUT_CHARS;
		}
		return Math.min(maxOutputChars, MAX_OUTPUT_CHARS);
	}

	private String commandMessage(int exitCode) {
		return exitCode == 0 ? "Command completed successfully." : "Command exited with a non-zero status.";
	}

	private int waitForProcess(Process process) throws InterruptedException {
		while (!process.waitFor(100, java.util.concurrent.TimeUnit.MILLISECONDS)) {
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedException("Command execution was interrupted");
			}
		}
		return process.exitValue();
	}

	private void destroyProcessTree(Process process) {
		ProcessHandle handle = process.toHandle();
		List<ProcessHandle> descendants = descendants(handle);
		for (int index = descendants.size() - 1; index >= 0; index--) {
			destroyAndWait(descendants.get(index));
		}
		destroyAndWait(handle);
		descendants(handle).forEach(this::destroyAndWait);
	}

	private List<ProcessHandle> descendants(ProcessHandle handle) {
		try {
			return handle.descendants().toList();
		} catch (RuntimeException ex) {
			return List.of();
		}
	}

	private void destroyAndWait(ProcessHandle handle) {
		handle.destroyForcibly();
		try {
			handle.onExit().get(Duration.ofMillis(500).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ignored) {
			// Best effort: the caller is already handling interruption/timeout and will return an observation.
		}
	}

	private void configureEnvironment(Map<String, String> environment, Path workingDirectory) {
		environment.clear();
		environment.put("PATH", commandPath());
		environment.put("LANG", valueOrDefault(this.environment.apply("LANG"), "C.UTF-8"));
		String lcAll = this.environment.apply("LC_ALL");
		if (lcAll != null && !lcAll.isBlank()) {
			environment.put("LC_ALL", lcAll);
		}
		String tmpDir = this.environment.apply("TMPDIR");
		if (tmpDir != null && !tmpDir.isBlank()) {
			environment.put("TMPDIR", tmpDir);
		}
		environment.put("CI", "true");
		environment.put("PWD", workingDirectory.toString());
	}

	private List<String> resolveCommandAliases(List<String> command) {
		String alias = MACOS_COMPILER_ALIASES.get(command.getFirst());
		if (alias == null || !Files.isExecutable(Path.of(alias))) {
			return command;
		}
		List<String> resolved = new ArrayList<>(command);
		resolved.set(0, alias);
		return List.copyOf(resolved);
	}

	private String commandPath() {
		Set<String> entries = new LinkedHashSet<>();
		addPathEntries(entries, DEFAULT_PATH);
		addPathEntries(entries, this.environment.apply("PATH"));
		return String.join(":", entries);
	}

	private void addPathEntries(Set<String> entries, String path) {
		if (path == null || path.isBlank()) {
			return;
		}
		for (String entry : path.split(":")) {
			if (!entry.isBlank()) {
				entries.add(entry);
			}
		}
	}

	private String valueOrDefault(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private Thread drainAsync(InputStream inputStream, BoundedText output) {
		return Thread.startVirtualThread(() -> {
			try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				char[] buffer = new char[1024];
				int read;
				while ((read = reader.read(buffer)) >= 0) {
					output.append(buffer, read);
				}
			} catch (IOException ignored) {
				output.append("\n[output stream closed]\n");
			}
		});
	}

	private void join(Thread thread) throws InterruptedException {
		thread.join(Duration.ofSeconds(2));
	}

	private static class BoundedText {

		private final int limit;
		private final StringBuilder builder = new StringBuilder();
		private boolean truncated;

		BoundedText(int limit) {
			this.limit = limit;
		}

		synchronized void append(char[] chars, int length) {
			if (this.builder.length() >= this.limit) {
				this.truncated = true;
				return;
			}
			int accepted = Math.min(length, this.limit - this.builder.length());
			this.builder.append(chars, 0, accepted);
			if (accepted < length) {
				this.truncated = true;
			}
		}

		synchronized void append(String text) {
			append(text.toCharArray(), text.length());
		}

		synchronized String content() {
			return this.builder.toString();
		}

		synchronized boolean truncated() {
			return this.truncated;
		}
	}
}
