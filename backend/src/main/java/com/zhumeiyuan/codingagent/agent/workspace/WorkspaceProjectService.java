package com.zhumeiyuan.codingagent.agent.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

public class WorkspaceProjectService {

	private final JdbcTemplate jdbcTemplate;
	private final WorkspacePathResolver resolver;
	private final Clock clock;

	public WorkspaceProjectService(JdbcTemplate jdbcTemplate, WorkspacePathResolver resolver, Clock clock) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
		this.resolver = Objects.requireNonNull(resolver, "resolver");
		this.clock = Objects.requireNonNull(clock, "clock");
		ensureDefaultProject();
	}

	public List<WorkspaceProject> listProjects() {
		return this.jdbcTemplate.query("""
				SELECT project_id, name, path, created_at, active
				FROM workspace_projects
				ORDER BY created_at DESC
				""", (rs, rowNum) -> new WorkspaceProject(
				rs.getString("project_id"),
				rs.getString("name"),
				Path.of(rs.getString("path")),
				Instant.parse(rs.getString("created_at")),
				rs.getBoolean("active")));
	}

	public WorkspaceProject activeProject() {
		List<WorkspaceProject> active = listProjects().stream().filter(WorkspaceProject::active).toList();
		if (!active.isEmpty()) {
			return active.get(0);
		}
		return ensureDefaultProject();
	}

	public WorkspaceProject addProject(String requestedPath, boolean createDirectory) {
		Path path = normalizeProjectPath(requestedPath);
		if (createDirectory) {
			createDirectory(path);
		}
		requireUsableDirectory(path);
		String normalizedPath = realPath(path).toString();
		List<WorkspaceProject> existing = listProjects().stream()
				.filter(project -> project.path().toString().equals(normalizedPath))
				.toList();
		if (!existing.isEmpty()) {
			return selectProject(existing.get(0).id());
		}
		String id = UUID.randomUUID().toString();
		Instant now = this.clock.instant();
		WorkspaceProject project = new WorkspaceProject(id, nameFor(path), Path.of(normalizedPath), now, false);
		this.jdbcTemplate.update("""
				INSERT INTO workspace_projects (project_id, name, path, created_at, active)
				VALUES (?, ?, ?, ?, ?)
				""", project.id(), project.name(), project.path().toString(), project.createdAt().toString(), false);
		return selectProject(project.id());
	}

	public WorkspaceProject selectProject(String projectId) {
		WorkspaceProject project = listProjects().stream()
				.filter(item -> item.id().equals(projectId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown project: " + projectId));
		requireUsableDirectory(project.path());
		this.jdbcTemplate.update("UPDATE workspace_projects SET active = FALSE");
		this.jdbcTemplate.update("UPDATE workspace_projects SET active = TRUE WHERE project_id = ?", project.id());
		this.resolver.switchRoot(project.path());
		return new WorkspaceProject(project.id(), project.name(), project.path(), project.createdAt(), true);
	}

	private WorkspaceProject ensureDefaultProject() {
		List<WorkspaceProject> projects = listProjects();
		if (!projects.isEmpty()) {
			WorkspaceProject active = projects.stream().filter(WorkspaceProject::active).findFirst().orElse(projects.get(0));
			if (!active.active()) {
				this.jdbcTemplate.update("UPDATE workspace_projects SET active = FALSE");
				this.jdbcTemplate.update("UPDATE workspace_projects SET active = TRUE WHERE project_id = ?", active.id());
			}
			this.resolver.switchRoot(active.path());
			return new WorkspaceProject(active.id(), active.name(), active.path(), active.createdAt(), true);
		}
		Path root = realPath(this.resolver.realRoot());
		WorkspaceProject project = new WorkspaceProject(UUID.randomUUID().toString(), nameFor(root), root,
				this.clock.instant(), true);
		this.jdbcTemplate.update("""
				INSERT INTO workspace_projects (project_id, name, path, created_at, active)
				VALUES (?, ?, ?, ?, ?)
				""", project.id(), project.name(), project.path().toString(), project.createdAt().toString(), true);
		return project;
	}

	private Path normalizeProjectPath(String requestedPath) {
		if (requestedPath == null || requestedPath.isBlank()) {
			throw new IllegalArgumentException("Project path must not be blank");
		}
		if (requestedPath.indexOf('\0') >= 0) {
			throw new IllegalArgumentException("Project path contains a NUL byte");
		}
		Path path = Path.of(requestedPath).toAbsolutePath().normalize();
		Path fileName = path.getFileName();
		if (fileName != null && fileName.toString().startsWith(".")) {
			throw new IllegalArgumentException("Hidden directories are not accepted as projects");
		}
		return path;
	}

	private void createDirectory(Path path) {
		try {
			Files.createDirectories(path);
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, path.toString(),
					"Cannot create project directory", ex);
		}
	}

	private void requireUsableDirectory(Path path) {
		if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.NOT_FOUND, path.toString(),
					"Project directory does not exist");
		}
		if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.NOT_DIRECTORY, path.toString(),
					"Project path is not a directory");
		}
	}

	private Path realPath(Path path) {
		try {
			return path.toRealPath();
		} catch (IOException ex) {
			throw new WorkspaceAccessException(WorkspaceAccessCode.IO_ERROR, path.toString(),
					"Cannot resolve project directory", ex);
		}
	}

	private String nameFor(Path path) {
		Path fileName = path.getFileName();
		return fileName == null ? path.toString() : fileName.toString();
	}
}
