package com.zhumeiyuan.codingagent.agent.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceReadToolsTests {

	@TempDir
	Path tempDir;

	private Path root;
	private WorkspaceReadTools tools;

	@BeforeEach
	void setUp() throws IOException {
		this.root = this.tempDir.resolve("workspace");
		Files.createDirectories(this.root.resolve("src"));
		Files.writeString(this.root.resolve("README.md"), "demo workspace\nhello agent\n");
		Files.writeString(this.root.resolve("src/App.java"), "class App { String name = \"agent\"; }\n");
		Files.writeString(this.root.resolve(".env"), "API_KEY=secret\n");
		this.tools = new WorkspaceReadTools(new WorkspacePathResolver(this.root));
	}

	@Test
	void listFilesReturnsSafeEntriesAndHidesEnvFiles() {
		FileListing listing = this.tools.listFiles(".", 10);

		assertThat(listing.root()).isEqualTo(".");
		assertThat(listing.files()).extracting(ListedWorkspaceFile::path).contains("README.md", "src")
				.doesNotContain(".env");
	}

	@Test
	void readFileReturnsUtf8Text() {
		ReadFileResult result = this.tools.readFile("README.md");

		assertThat(result.path()).isEqualTo("README.md");
		assertThat(result.content()).contains("hello agent");
		assertThat(result.sizeBytes()).isPositive();
	}

	@Test
	void readFileRejectsDirectoryAndInvalidUtf8() throws IOException {
		byte[] invalidUtf8 = HexFormat.of().parseHex("c328");
		Files.write(this.root.resolve("binary.dat"), invalidUtf8);

		assertWorkspaceError(() -> this.tools.readFile("src"), WorkspaceAccessCode.NOT_REGULAR_FILE);
		assertWorkspaceError(() -> this.tools.readFile("binary.dat"), WorkspaceAccessCode.INVALID_TEXT);
	}

	@Test
	void searchTextFindsMatchesAndSkipsSensitiveFiles() {
		SearchResult result = this.tools.searchText("agent", 10);

		assertThat(result.matches()).extracting(SearchMatch::path).contains("README.md", "src/App.java")
				.doesNotContain(".env");
		assertThat(result.truncated()).isFalse();
	}

	@Test
	void searchTextReportsTruncationAtLimit() {
		SearchResult result = this.tools.searchText("agent", 1);

		assertThat(result.matches()).hasSize(1);
		assertThat(result.truncated()).isTrue();
	}

	private void assertWorkspaceError(Runnable action, WorkspaceAccessCode code) {
		assertThatThrownBy(action::run)
				.isInstanceOf(WorkspaceAccessException.class)
				.extracting(ex -> ((WorkspaceAccessException) ex).code())
				.isEqualTo(code);
	}
}
