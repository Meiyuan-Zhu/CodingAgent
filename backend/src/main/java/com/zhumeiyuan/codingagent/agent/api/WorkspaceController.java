package com.zhumeiyuan.codingagent.agent.api;

import java.util.List;

import com.zhumeiyuan.codingagent.agent.workspace.FileListing;
import com.zhumeiyuan.codingagent.agent.workspace.ListedWorkspaceFile;
import com.zhumeiyuan.codingagent.agent.workspace.ReadFileResult;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceProject;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceProjectService;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceReadTools;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace")
class WorkspaceController {

	private final WorkspaceReadTools readTools;
	private final WorkspaceProjectService projectService;

	WorkspaceController(WorkspaceReadTools readTools, WorkspaceProjectService projectService) {
		this.readTools = readTools;
		this.projectService = projectService;
	}

	@GetMapping("/files")
	WorkspaceFilesResponse listFiles(@RequestParam(name = "path", defaultValue = ".") String path) {
		return WorkspaceFilesResponse.from(this.readTools.listFiles(path, 500));
	}

	@GetMapping("/file")
	WorkspaceFileResponse readFile(@RequestParam("path") String path) {
		return WorkspaceFileResponse.from(this.readTools.readFile(path));
	}

	@GetMapping("/projects")
	List<WorkspaceProjectResponse> listProjects() {
		return this.projectService.listProjects().stream()
				.map(WorkspaceProjectResponse::from)
				.toList();
	}

	@PostMapping("/projects")
	WorkspaceProjectResponse addProject(@Valid @RequestBody AddWorkspaceProjectRequest request) {
		return WorkspaceProjectResponse.from(this.projectService.addProject(request.path(), request.create()));
	}

	@PostMapping("/projects/{projectId}/select")
	WorkspaceProjectResponse selectProject(@PathVariable String projectId) {
		return WorkspaceProjectResponse.from(this.projectService.selectProject(projectId));
	}

	record WorkspaceFilesResponse(String root, List<WorkspaceFileEntryResponse> files, boolean truncated) {

		static WorkspaceFilesResponse from(FileListing listing) {
			return new WorkspaceFilesResponse(listing.root(),
					listing.files().stream().map(WorkspaceFileEntryResponse::from).toList(),
					listing.truncated());
		}
	}

	record WorkspaceFileEntryResponse(String path, String type, long sizeBytes) {

		static WorkspaceFileEntryResponse from(ListedWorkspaceFile file) {
			return new WorkspaceFileEntryResponse(file.path(), file.type().name(), file.sizeBytes());
		}
	}

	record WorkspaceFileResponse(String path, String content, long sizeBytes) {

		static WorkspaceFileResponse from(ReadFileResult file) {
			return new WorkspaceFileResponse(file.path(), file.content(), file.sizeBytes());
		}
	}

	record AddWorkspaceProjectRequest(@NotBlank(message = "Project path must not be blank") String path,
			boolean create) {
	}

	record WorkspaceProjectResponse(String id, String name, String path, String createdAt, boolean active) {

		static WorkspaceProjectResponse from(WorkspaceProject project) {
			return new WorkspaceProjectResponse(project.id(), project.name(), project.path().toString(),
					project.createdAt().toString(), project.active());
		}
	}
}
