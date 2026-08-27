package com.zhumeiyuan.codingagent.agent.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceWriteToolsTests {

	@TempDir
	Path tempDir;

	private Path root;
	private WorkspaceWriteTools tools;

	@BeforeEach
	void setUp() throws IOException {
		this.root = this.tempDir.resolve("workspace");
		Files.createDirectories(this.root.resolve("src"));
		Files.writeString(this.root.resolve("README.md"), "hello agent\nhello workspace\n");
		Files.write(this.root.resolve("binary.dat"), new byte[] { (byte) 0xc3, 0x28 });
		this.tools = new WorkspaceWriteTools(new WorkspacePathResolver(this.root));
	}

	@Test
	void writeFileCreatesNewUtf8File() throws IOException {
		WriteFileResult result = this.tools.writeFile("src/new.txt", "new content\n", false, "");

		assertThat(result.path()).isEqualTo("src/new.txt");
		assertThat(result.created()).isTrue();
		assertThat(result.overwritten()).isFalse();
		assertThat(result.previousSha256()).isNull();
		assertThat(result.sha256()).hasSize(64);
		assertThat(result.unifiedDiff()).contains("--- a/src/new.txt", "+++ b/src/new.txt", "+new content");
		assertThat(Files.readString(this.root.resolve("src/new.txt"))).isEqualTo("new content\n");
	}

	@Test
	void writeFileRejectsExistingFileUnlessOverwriteIsAllowed() {
		assertWorkspaceError(() -> this.tools.writeFile("README.md", "changed\n", false, ""),
				WorkspaceAccessCode.FILE_ALREADY_EXISTS);
	}

	@Test
	void writeFileUsesExpectedHashForConflictDetection() throws IOException {
		WriteFileResult first = this.tools.writeFile("README.md", "changed once\n", true, "");

		assertWorkspaceError(() -> this.tools.writeFile("README.md", "changed twice\n", true, "0".repeat(64)),
				WorkspaceAccessCode.CONTENT_CONFLICT);

		WriteFileResult second = this.tools.writeFile("README.md", "changed twice\n", true, first.sha256());
		assertThat(second.overwritten()).isTrue();
		assertThat(Files.readString(this.root.resolve("README.md"))).isEqualTo("changed twice\n");
	}

	@Test
	void writeFileRejectsSensitivePathAndOversizedContent() {
		assertWorkspaceError(() -> this.tools.writeFile(".env", "API_KEY=secret\n", false, ""),
				WorkspaceAccessCode.SENSITIVE_PATH);
		assertWorkspaceError(() -> this.tools.writeFile("src/large.txt", "x".repeat(200_001), false, ""),
				WorkspaceAccessCode.FILE_TOO_LARGE);
	}

	@Test
	void writeFileRejectsExistingInvalidUtf8File() {
		assertWorkspaceError(() -> this.tools.writeFile("binary.dat", "text\n", true, ""),
				WorkspaceAccessCode.INVALID_TEXT);
	}

	@Test
	void replaceTextEditsExistingUtf8File() throws IOException {
		TextReplacementResult result = this.tools.replaceText("README.md", "hello", "hi", "", 1);

		assertThat(result.path()).isEqualTo("README.md");
		assertThat(result.replacements()).isEqualTo(1);
		assertThat(result.previousSha256()).hasSize(64);
		assertThat(result.sha256()).hasSize(64).isNotEqualTo(result.previousSha256());
		assertThat(result.unifiedDiff()).contains("-hello agent", "+hi agent", " hello workspace");
		assertThat(Files.readString(this.root.resolve("README.md"))).isEqualTo("hi agent\nhello workspace\n");
	}

	@Test
	void replaceTextAllowsDeletingTextWithEmptyReplacement() throws IOException {
		this.tools.replaceText("README.md", "hello ", "", "", 2);

		assertThat(Files.readString(this.root.resolve("README.md"))).isEqualTo("agent\nworkspace\n");
	}

	@Test
	void replaceTextReportsMissingTextAndHashConflict() {
		assertWorkspaceError(() -> this.tools.replaceText("README.md", "missing", "new", "", 1),
				WorkspaceAccessCode.TEXT_NOT_FOUND);
		assertWorkspaceError(() -> this.tools.replaceText("README.md", "hello", "new", "0".repeat(64), 1),
				WorkspaceAccessCode.CONTENT_CONFLICT);
	}

	@Test
	void replaceTextRejectsDirectoryInvalidUtf8AndTooManyReplacements() {
		assertWorkspaceError(() -> this.tools.replaceText("src", "old", "new", "", 1),
				WorkspaceAccessCode.NOT_REGULAR_FILE);
		assertWorkspaceError(() -> this.tools.replaceText("binary.dat", "old", "new", "", 1),
				WorkspaceAccessCode.INVALID_TEXT);
		assertThatThrownBy(() -> this.tools.replaceText("README.md", "old", "new", "", 201))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private void assertWorkspaceError(Runnable action, WorkspaceAccessCode code) {
		assertThatThrownBy(action::run)
				.isInstanceOf(WorkspaceAccessException.class)
				.extracting(ex -> ((WorkspaceAccessException) ex).code())
				.isEqualTo(code);
	}
}
