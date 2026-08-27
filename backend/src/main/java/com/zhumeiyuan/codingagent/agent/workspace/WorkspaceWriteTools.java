package com.zhumeiyuan.codingagent.agent.workspace;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public class WorkspaceWriteTools {

	private static final long MAX_WRITE_BYTES = 200_000;
	private static final int MAX_REPLACEMENTS = 200;

	private final WorkspacePathResolver resolver;

	public WorkspaceWriteTools(WorkspacePathResolver resolver) {
		this.resolver = Objects.requireNonNull(resolver, "resolver");
	}

	public WriteFileResult writeFile(String requestedPath, String content, boolean overwrite, String expectedSha256) {
		Objects.requireNonNull(content, "content");
		byte[] nextBytes = encodeWithinLimit(content, requestedPath);
		ResolvedWorkspacePath target = this.resolver.resolveForWrite(requestedPath);
		Path path = target.realPath();
		boolean existed = Files.exists(path, LinkOption.NOFOLLOW_LINKS);

		String previousHash = null;
		String previousText = "";
		long previousSize = 0;
		if (existed) {
			if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.NOT_REGULAR_FILE, requestedPath,
						"Workspace path is not a regular file");
			}
			if (!overwrite) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.FILE_ALREADY_EXISTS, requestedPath,
						"Workspace file already exists");
			}
			byte[] previousBytes = readBytes(path, requestedPath);
			previousText = decodeUtf8(previousBytes, requestedPath);
			previousHash = sha256(previousBytes);
			previousSize = previousBytes.length;
			rejectHashMismatch(requestedPath, previousHash, expectedSha256);
		} else if (expectedSha256 != null && !expectedSha256.isBlank()) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.CONTENT_CONFLICT, requestedPath,
					"Workspace file does not exist, so expected_sha256 cannot match");
		}

		writeBytes(path, nextBytes, requestedPath);
		String nextText = decodeUtf8(nextBytes, requestedPath);
		return new WriteFileResult(target.displayPath(), !existed, existed, previousHash, sha256(nextBytes),
				previousSize, nextBytes.length, WorkspaceUnifiedDiff.create(target.displayPath(), previousText, nextText));
	}

	public TextReplacementResult replaceText(String requestedPath, String oldText, String newText, String expectedSha256,
			int maxReplacements) {
		if (oldText == null || oldText.isEmpty()) {
			throw new IllegalArgumentException("oldText must not be empty");
		}
		Objects.requireNonNull(newText, "newText");
		if (maxReplacements <= 0 || maxReplacements > MAX_REPLACEMENTS) {
			throw new IllegalArgumentException("maxReplacements must be between 1 and " + MAX_REPLACEMENTS);
		}

		ResolvedWorkspacePath target = this.resolver.resolveExisting(requestedPath);
		Path path = target.realPath();
		if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.NOT_REGULAR_FILE, requestedPath,
					"Workspace path is not a regular file");
		}

		byte[] previousBytes = readBytes(path, requestedPath);
		String previousText = decodeUtf8(previousBytes, requestedPath);
		String previousHash = sha256(previousBytes);
		rejectHashMismatch(requestedPath, previousHash, expectedSha256);

		Replacement replacement = replaceLimited(previousText, oldText, newText, maxReplacements);
		if (replacement.count() == 0) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.TEXT_NOT_FOUND, requestedPath,
					"Text to replace was not found in workspace file");
		}
		byte[] nextBytes = encodeWithinLimit(replacement.text(), requestedPath);
		writeBytes(path, nextBytes, requestedPath);

		return new TextReplacementResult(target.displayPath(), replacement.count(), previousHash, sha256(nextBytes),
				nextBytes.length, WorkspaceUnifiedDiff.create(target.displayPath(), previousText, replacement.text()));
	}

	private Replacement replaceLimited(String source, String oldText, String newText, int maxReplacements) {
		StringBuilder builder = new StringBuilder(source.length());
		int index = 0;
		int count = 0;
		while (count < maxReplacements) {
			int found = source.indexOf(oldText, index);
			if (found < 0) {
				break;
			}
			builder.append(source, index, found);
			builder.append(newText);
			index = found + oldText.length();
			count++;
		}
		if (count == 0) {
			return new Replacement(source, 0);
		}
		builder.append(source.substring(index));
		return new Replacement(builder.toString(), count);
	}

	private void rejectHashMismatch(String requestedPath, String actualSha256, String expectedSha256) {
		if (expectedSha256 != null && !expectedSha256.isBlank() && !actualSha256.equalsIgnoreCase(expectedSha256)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.CONTENT_CONFLICT, requestedPath,
					"Workspace file content changed since it was read");
		}
	}

	private byte[] encodeWithinLimit(String content, String requestedPath) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_WRITE_BYTES) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.FILE_TOO_LARGE, requestedPath,
					"Workspace file is too large to write");
		}
		return bytes;
	}

	private byte[] readBytes(Path path, String requestedPath) {
		try {
			return Files.readAllBytes(path);
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, requestedPath, "Cannot read workspace file",
					ex);
		}
	}

	private void writeBytes(Path path, byte[] bytes, String requestedPath) {
		try {
			Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE);
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, requestedPath, "Cannot write workspace file",
					ex);
		}
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

	private String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	private record Replacement(String text, int count) {
	}
}
