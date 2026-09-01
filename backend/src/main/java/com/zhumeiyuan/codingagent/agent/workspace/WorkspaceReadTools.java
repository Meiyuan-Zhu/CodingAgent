package com.zhumeiyuan.codingagent.agent.workspace;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class WorkspaceReadTools {

	private static final int DEFAULT_MAX_ENTRIES = 200;
	private static final int DEFAULT_MAX_MATCHES = 100;
	private static final long MAX_READ_BYTES = 200_000;
	private static final long MAX_SEARCH_FILE_BYTES = 300_000;

	private final WorkspacePathResolver resolver;

	public WorkspaceReadTools(WorkspacePathResolver resolver) {
		this.resolver = Objects.requireNonNull(resolver, "resolver");
	}

	public FileListing listFiles(String requestedDirectory) {
		return listFiles(requestedDirectory, DEFAULT_MAX_ENTRIES);
	}

	public FileListing listFiles(String requestedDirectory, int maxEntries) {
		if (maxEntries <= 0) {
			throw new IllegalArgumentException("maxEntries must be positive");
		}

		ResolvedWorkspacePath directory = this.resolver.resolveExisting(requestedDirectory);
		if (!Files.isDirectory(directory.realPath(), LinkOption.NOFOLLOW_LINKS)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.NOT_DIRECTORY, requestedDirectory,
					"Workspace path is not a directory");
		}

		List<ListedWorkspaceFile> files = new ArrayList<>();
		boolean truncated = false;
		try (Stream<Path> stream = Files.list(directory.realPath())) {
			List<Path> paths = stream.sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
			for (Path path : paths) {
				Path relative = this.resolver.toDisplayRelativePath(path);
				if (this.resolver.isSensitiveRelativePath(relative)) {
					continue;
				}
				if (files.size() >= maxEntries) {
					truncated = true;
					break;
				}
				files.add(toListedFile(path, relative));
			}
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, requestedDirectory,
					"Cannot list workspace directory", ex);
		}

		return new FileListing(true, "Listed workspace directory.", directory.displayPath(), files, truncated);
	}

	public ReadFileResult readFile(String requestedPath) {
		ResolvedWorkspacePath file = this.resolver.resolveExisting(requestedPath);
		if (!Files.isRegularFile(file.realPath(), LinkOption.NOFOLLOW_LINKS)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.NOT_REGULAR_FILE, requestedPath,
					"Workspace path is not a regular file");
		}

		try {
			long size = Files.size(file.realPath());
			if (size > MAX_READ_BYTES) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.FILE_TOO_LARGE, requestedPath,
						"Workspace file is too large to read into the model context");
			}
			byte[] bytes = Files.readAllBytes(file.realPath());
				return new ReadFileResult(true, "Read workspace file.", file.displayPath(), decodeUtf8(bytes, requestedPath),
						size);
		} catch (WorkspaceAccessException ex) {
			throw ex;
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, requestedPath, "Cannot read workspace file",
					ex);
		}
	}

	public SearchResult searchText(String query) {
		return searchText(query, DEFAULT_MAX_MATCHES);
	}

	public SearchResult searchText(String query, int maxMatches) {
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException("Search query must not be blank");
		}
		if (maxMatches <= 0) {
			throw new IllegalArgumentException("maxMatches must be positive");
		}

		List<SearchMatch> matches = new ArrayList<>();
		boolean truncated = false;
		try (Stream<Path> stream = Files.walk(this.resolver.realRoot())) {
			List<Path> paths = stream.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
					.sorted(Comparator.comparing(path -> this.resolver.toDisplayRelativePath(path).toString())).toList();
			for (Path path : paths) {
				Path relative = this.resolver.toDisplayRelativePath(path);
				if (this.resolver.isSensitiveRelativePath(relative) || tooLargeForSearch(path)) {
					continue;
				}
				truncated = collectMatches(path, relative, query, maxMatches, matches);
				if (truncated) {
					break;
				}
			}
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, ".", "Cannot search workspace", ex);
		}
			return new SearchResult(true, "Searched workspace text.", query, matches, truncated);
	}

	private ListedWorkspaceFile toListedFile(Path path, Path relative) throws IOException {
		WorkspaceFileType type;
		if (Files.isSymbolicLink(path)) {
			type = WorkspaceFileType.SYMLINK;
		} else if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			type = WorkspaceFileType.DIRECTORY;
		} else {
			type = WorkspaceFileType.FILE;
		}
		long size = type == WorkspaceFileType.FILE ? Files.size(path) : 0;
		return new ListedWorkspaceFile(relative.toString().replace('\\', '/'), type, size);
	}

	private boolean tooLargeForSearch(Path path) throws IOException {
		return Files.size(path) > MAX_SEARCH_FILE_BYTES;
	}

	private boolean collectMatches(Path path, Path relative, String query, int maxMatches, List<SearchMatch> matches)
			throws IOException {
		String content;
		try {
			content = decodeUtf8(Files.readAllBytes(path), relative.toString());
		} catch (WorkspaceAccessException ex) {
			if (ex.code() == WorkspaceAccessCode.INVALID_TEXT) {
				return false;
			}
			throw ex;
		}

		String[] lines = content.split("\\R", -1);
		for (int index = 0; index < lines.length; index++) {
			if (lines[index].contains(query)) {
				matches.add(new SearchMatch(relative.toString().replace('\\', '/'), index + 1, lines[index]));
				if (matches.size() >= maxMatches) {
					return true;
				}
			}
		}
		return false;
	}

	private String decodeUtf8(byte[] bytes, String requestedPath) {
		try {
			return StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes))
					.toString();
		} catch (CharacterCodingException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.INVALID_TEXT, requestedPath,
					"Workspace file is not valid UTF-8 text", ex);
		}
	}
}
