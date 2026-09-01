package com.zhumeiyuan.codingagent.agent.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceCommandToolsTests {

	@TempDir
	Path tempDir;

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-28T02:00:00Z"), ZoneOffset.UTC);
	private Path root;
	private WorkspaceCommandTools tools;

	@BeforeEach
	void setUp() throws IOException {
		this.root = this.tempDir.resolve("workspace");
		Files.createDirectories(this.root.resolve("src"));
		Files.writeString(this.root.resolve("src/input.txt"), "hello\n");
		WorkspacePathResolver resolver = new WorkspacePathResolver(this.root);
		this.tools = new WorkspaceCommandTools(resolver, this.clock, name -> switch (name) {
			case "PATH" -> "/usr/bin:/bin";
			case "LANG" -> "C.UTF-8";
			case "TMPDIR" -> "/tmp";
			default -> null;
		});
	}

	@Test
	void runsCommandWithoutShellInWorkspaceDirectory() {
		CommandExecutionResult result = this.tools.runCommand(List.of("/bin/pwd"), "src", 1000);

		assertThat(result.success()).isTrue();
		assertThat(result.message()).isEqualTo("Command completed successfully.");
		assertThat(result.timedOut()).isFalse();
		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.cwd()).isEqualTo("src");
		assertThat(result.stdout()).contains(this.root.resolve("src").toString());
		assertThat(result.stderr()).isEmpty();
	}

	@Test
	void capturesNonZeroExitCodeAsCommandResult() {
		CommandExecutionResult result = this.tools.runCommand(List.of("/usr/bin/false"), ".", 1000);

		assertThat(result.success()).isFalse();
		assertThat(result.message()).isEqualTo("Command exited with a non-zero status.");
		assertThat(result.timedOut()).isFalse();
		assertThat(result.exitCode()).isEqualTo(1);
		assertThat(result.stdout()).isEmpty();
	}

	@Test
	void truncatesStdoutWhenOutputExceedsLimit() {
		CommandExecutionResult result = this.tools.runCommand(List.of("/usr/bin/printf", "%s", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"), ".", 20);

		assertThat(result.stdout()).hasSize(20);
		assertThat(result.stdoutTruncated()).isTrue();
	}

	@Test
	void rejectsWorkingDirectoryOutsideWorkspace() {
		assertThatThrownBy(() -> this.tools.runCommand(List.of("/bin/pwd"), "..", 1000))
				.isInstanceOf(WorkspaceAccessException.class)
				.extracting(ex -> ((WorkspaceAccessException) ex).code())
				.isEqualTo(WorkspaceAccessCode.PATH_ESCAPE);
	}

	@Test
	void rejectsWorkingDirectoryThatIsAFile() {
		assertThatThrownBy(() -> this.tools.runCommand(List.of("/bin/pwd"), "src/input.txt", 1000))
				.isInstanceOf(WorkspaceAccessException.class)
				.extracting(ex -> ((WorkspaceAccessException) ex).code())
				.isEqualTo(WorkspaceAccessCode.PARENT_NOT_DIRECTORY);
	}

	@Test
	void usesMinimalEnvironment() throws IOException {
		CommandExecutionResult result = this.tools.runCommand(List.of("/usr/bin/env"), ".", 2000);

		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.stdout()).contains("CI=true").contains("PWD=" + this.root.toRealPath());
		assertThat(result.stdout())
				.contains("PATH=/opt/homebrew/opt/llvm/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin")
				.contains("TMPDIR=/tmp")
				.doesNotContain("HOME=")
				.doesNotContain("DEEPSEEK_API_KEY=");
	}

	@Test
	void resolvesHomebrewGccAliasWhenAvailable() {
		CommandExecutionResult result = this.tools.runCommand(List.of("g++", "--version"), ".", 4000);

		if (Files.isExecutable(Path.of("/opt/homebrew/bin/g++-15"))) {
			assertThat(result.command()).startsWith("/opt/homebrew/bin/g++-15");
			assertThat(result.exitCode()).isEqualTo(0);
			assertThat(result.stdout()).containsIgnoringCase("gcc");
		}
		else {
			assertThat(result.command()).startsWith("g++");
		}
	}

	@Test
	void rejectsBlankCommandParts() {
		assertThatThrownBy(() -> this.tools.runCommand(List.of("/bin/echo", " "), ".", 1000))
				.isInstanceOf(WorkspaceAccessException.class);
	}

	@Test
	void interruptedCommandDestroysChildProcessTree() throws Exception {
		Assumptions.assumeTrue(processTreeEnumerationAvailable());
		Path childPidFile = this.root.resolve("child.pid");
		Path childOutputFile = this.root.resolve("child-output.txt");
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread commandThread = new Thread(() -> {
			try {
				this.tools.runCommand(List.of("/bin/sh", "-c",
						"while true; do echo alive >> child-output.txt; sleep 0.1; done & echo $! > child.pid; wait"), ".",
						1000);
			} catch (Throwable ex) {
				failure.set(ex);
			}
		});
		commandThread.start();
		try {
			waitForChildPid(childPidFile);

			commandThread.interrupt();
			commandThread.join(Duration.ofSeconds(5));

			assertThat(commandThread.isAlive()).isFalse();
			assertThat(failure.get()).isInstanceOf(WorkspaceAccessException.class);
			assertThat(outputStopsGrowing(childOutputFile, Duration.ofSeconds(2))).isTrue();
		} finally {
			commandThread.interrupt();
			if (Files.exists(childPidFile)) {
				String rawPid = Files.readString(childPidFile).trim();
				if (!rawPid.isBlank()) {
					ProcessHandle.of(Long.parseLong(rawPid)).ifPresent(ProcessHandle::destroyForcibly);
				}
			}
		}
	}

	private long waitForChildPid(Path childPidFile) throws Exception {
		Instant deadline = Instant.now().plusSeconds(5);
		while (Instant.now().isBefore(deadline)) {
			if (Files.exists(childPidFile)) {
				String rawPid = Files.readString(childPidFile).trim();
				if (!rawPid.isBlank()) {
					return Long.parseLong(rawPid);
				}
			}
			Thread.sleep(20);
		}
		throw new AssertionError("Command did not write child pid");
	}

	private boolean processTreeEnumerationAvailable() {
		try {
			ProcessHandle.current().descendants().toList();
			return true;
		} catch (RuntimeException ex) {
			return false;
		}
	}

	private boolean outputStopsGrowing(Path outputFile, Duration timeout) throws Exception {
		Thread.sleep(500);
		long firstSize = Files.exists(outputFile) ? Files.size(outputFile) : 0;
		Instant deadline = Instant.now().plus(timeout);
		while (Instant.now().isBefore(deadline)) {
			Thread.sleep(50);
		}
		long secondSize = Files.exists(outputFile) ? Files.size(outputFile) : 0;
		return firstSize == secondSize;
	}

}
