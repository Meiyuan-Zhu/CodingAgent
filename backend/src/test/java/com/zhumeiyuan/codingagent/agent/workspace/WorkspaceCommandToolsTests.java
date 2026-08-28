package com.zhumeiyuan.codingagent.agent.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
			case "PATH" -> System.getenv("PATH");
			case "LANG" -> "C.UTF-8";
			default -> null;
		});
	}

	@Test
	void runsCommandWithoutShellInWorkspaceDirectory() {
		CommandExecutionResult result = this.tools.runCommand(List.of("/bin/pwd"), "src", 1000);

		assertThat(result.exitCode()).isEqualTo(0);
		assertThat(result.cwd()).isEqualTo("src");
		assertThat(result.stdout()).contains(this.root.resolve("src").toString());
		assertThat(result.stderr()).isEmpty();
	}

	@Test
	void capturesNonZeroExitCodeAsCommandResult() {
		CommandExecutionResult result = this.tools.runCommand(List.of("/usr/bin/false"), ".", 1000);

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
		assertThat(result.stdout()).doesNotContain("HOME=").doesNotContain("DEEPSEEK_API_KEY=");
	}

	@Test
	void rejectsBlankCommandParts() {
		assertThatThrownBy(() -> this.tools.runCommand(List.of("/bin/echo", " "), ".", 1000))
				.isInstanceOf(WorkspaceAccessException.class);
	}
}
