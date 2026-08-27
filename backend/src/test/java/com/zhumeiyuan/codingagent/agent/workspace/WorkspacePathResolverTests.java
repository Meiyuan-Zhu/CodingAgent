package com.zhumeiyuan.codingagent.agent.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspacePathResolverTests {

	@TempDir
	Path tempDir;

	@Test
	void normalizesRelativePathsInsideWorkspace() throws IOException {
		Path root = this.tempDir.resolve("workspace");
		Files.createDirectories(root.resolve("src"));
		Files.writeString(root.resolve("README.md"), "hello");

		WorkspacePathResolver resolver = new WorkspacePathResolver(root);
		ResolvedWorkspacePath resolved = resolver.resolveExisting("src/../README.md");

		assertThat(resolved.displayPath()).isEqualTo("README.md");
		assertThat(resolved.realPath()).isEqualTo(root.resolve("README.md").toRealPath());
	}

	@Test
	void rejectsAbsolutePathsAndPathEscape() throws IOException {
		Path root = this.tempDir.resolve("workspace");
		Path outside = this.tempDir.resolve("outside.txt");
		Files.createDirectories(root);
		Files.writeString(outside, "secret");

		WorkspacePathResolver resolver = new WorkspacePathResolver(root);

		assertWorkspaceError(() -> resolver.resolveExisting(outside.toString()), WorkspaceAccessCode.ABSOLUTE_PATH);
		assertWorkspaceError(() -> resolver.resolveExisting("../outside.txt"), WorkspaceAccessCode.PATH_ESCAPE);
	}

	@Test
	void rejectsSensitiveEnvFiles() throws IOException {
		Path root = this.tempDir.resolve("workspace");
		Files.createDirectories(root.resolve("nested"));
		Files.writeString(root.resolve(".env"), "API_KEY=secret");
		Files.writeString(root.resolve("nested/.env.local"), "TOKEN=secret");

		WorkspacePathResolver resolver = new WorkspacePathResolver(root);

		assertWorkspaceError(() -> resolver.resolveExisting(".env"), WorkspaceAccessCode.SENSITIVE_PATH);
		assertWorkspaceError(() -> resolver.resolveExisting("nested/.env.local"), WorkspaceAccessCode.SENSITIVE_PATH);
	}

	@Test
	void rejectsSymlinkThatResolvesOutsideWorkspace() throws IOException {
		Path root = this.tempDir.resolve("workspace");
		Path outside = this.tempDir.resolve("outside.txt");
		Files.createDirectories(root);
		Files.writeString(outside, "secret");
		Path link = root.resolve("leak.txt");
		try {
			Files.createSymbolicLink(link, outside);
		} catch (UnsupportedOperationException | IOException ex) {
			assumeTrue(false, "Symbolic links are not available in this environment");
		}

		WorkspacePathResolver resolver = new WorkspacePathResolver(root);

		assertWorkspaceError(() -> resolver.resolveExisting("leak.txt"), WorkspaceAccessCode.SYMLINK_ESCAPE);
	}

	private void assertWorkspaceError(Runnable action, WorkspaceAccessCode code) {
		assertThatThrownBy(action::run)
				.isInstanceOf(WorkspaceAccessException.class)
				.extracting(ex -> ((WorkspaceAccessException) ex).code())
				.isEqualTo(code);
	}
}
