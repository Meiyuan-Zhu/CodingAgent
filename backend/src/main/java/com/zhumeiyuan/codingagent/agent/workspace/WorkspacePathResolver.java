package com.zhumeiyuan.codingagent.agent.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

public class WorkspacePathResolver {

	private static final Set<String> SENSITIVE_NAMES = Set.of(".env", ".env.local", ".env.development",
			".env.production", ".env.test");

	private final Path configuredRoot;
	private final Path realRoot;

	public WorkspacePathResolver(Path configuredRoot) {
		this.configuredRoot = Objects.requireNonNull(configuredRoot, "configuredRoot").toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.configuredRoot);
			this.realRoot = this.configuredRoot.toRealPath();
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, this.configuredRoot.toString(),
					"Cannot initialize workspace root", ex);
		}
	}

	public Path realRoot() {
		return this.realRoot;
	}

	public ResolvedWorkspacePath resolveExisting(String requestedPath) {
		Path relativePath = normalizeRelativePath(requestedPath);
		rejectSensitivePath(relativePath, requestedPath);

		Path candidate = this.configuredRoot.resolve(relativePath).normalize();
		rejectEscape(candidate, requestedPath);

		if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.NOT_FOUND, requestedPath, "Workspace path not found");
		}

		try {
			Path realPath = candidate.toRealPath();
			if (!realPath.startsWith(this.realRoot)) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.SYMLINK_ESCAPE, requestedPath,
						"Workspace path resolves outside the workspace root");
			}
			return new ResolvedWorkspacePath(this.realRoot.relativize(realPath), realPath);
		} catch (WorkspaceAccessException ex) {
			throw ex;
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, requestedPath, "Cannot resolve workspace path",
					ex);
		}
	}

	public ResolvedWorkspacePath resolveForWrite(String requestedPath) {
		Path relativePath = normalizeRelativePath(requestedPath);
		rejectSensitivePath(relativePath, requestedPath);
		if (relativePath.getFileName() == null) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.INVALID_WRITE_TARGET, requestedPath,
					"Workspace write target must be a file path");
		}

		Path candidate = this.configuredRoot.resolve(relativePath).normalize();
		rejectEscape(candidate, requestedPath);

		if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
			return resolveExisting(requestedPath);
		}

		Path parent = candidate.getParent();
		if (parent == null || !parent.startsWith(this.configuredRoot)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.PATH_ESCAPE, requestedPath,
					"Workspace path escapes the workspace root");
		}
		if (!Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.PARENT_NOT_FOUND, requestedPath,
					"Workspace parent directory not found");
		}

		try {
			Path realParent = parent.toRealPath();
			if (!realParent.startsWith(this.realRoot)) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.SYMLINK_ESCAPE, requestedPath,
						"Workspace parent resolves outside the workspace root");
			}
			if (!Files.isDirectory(realParent, LinkOption.NOFOLLOW_LINKS)) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.PARENT_NOT_DIRECTORY, requestedPath,
						"Workspace parent is not a directory");
			}
			Path realTarget = realParent.resolve(candidate.getFileName()).normalize();
			if (!realTarget.startsWith(this.realRoot)) {
				throw new WorkspaceAccessException(WorkspaceAccessCode.PATH_ESCAPE, requestedPath,
						"Workspace path escapes the workspace root");
			}
			return new ResolvedWorkspacePath(this.realRoot.relativize(realTarget), realTarget);
		} catch (WorkspaceAccessException ex) {
			throw ex;
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, requestedPath,
					"Cannot resolve workspace write target", ex);
		}
	}

	public boolean isSensitiveRelativePath(Path relativePath) {
		for (Path segment : relativePath.normalize()) {
			if (SENSITIVE_NAMES.contains(segment.toString())) {
				return true;
			}
		}
		return false;
	}

	public Path toDisplayRelativePath(Path realPath) {
		Path normalizedRealPath = realPath.toAbsolutePath().normalize();
		if (!normalizedRealPath.startsWith(this.realRoot)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.PATH_ESCAPE, normalizedRealPath.toString(),
					"Path is outside the workspace root");
		}
		return this.realRoot.relativize(normalizedRealPath);
	}

	private Path normalizeRelativePath(String requestedPath) {
		if (requestedPath == null || requestedPath.isBlank()) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.BLANK_PATH, requestedPath, "Workspace path is blank");
		}
		if (requestedPath.indexOf('\0') >= 0) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.PATH_ESCAPE, requestedPath,
					"Workspace path contains a NUL byte");
		}

		Path rawPath = Path.of(requestedPath);
		if (rawPath.isAbsolute()) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.ABSOLUTE_PATH, requestedPath,
					"Workspace path must be relative");
		}
		return rawPath.normalize();
	}

	private void rejectEscape(Path candidate, String requestedPath) {
		if (!candidate.startsWith(this.configuredRoot)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.PATH_ESCAPE, requestedPath,
					"Workspace path escapes the workspace root");
		}
	}

	private void rejectSensitivePath(Path relativePath, String requestedPath) {
		if (isSensitiveRelativePath(relativePath)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.SENSITIVE_PATH, requestedPath,
					"Workspace path is blocked because it may contain secrets");
		}
	}
}
