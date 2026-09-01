package com.zhumeiyuan.codingagent.agent.api;

import java.util.List;

import com.zhumeiyuan.codingagent.agent.workspace.FileListing;
import com.zhumeiyuan.codingagent.agent.workspace.ListedWorkspaceFile;
import com.zhumeiyuan.codingagent.agent.workspace.ReadFileResult;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceReadTools;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace")
class WorkspaceController {

	private final WorkspaceReadTools readTools;

	WorkspaceController(WorkspaceReadTools readTools) {
		this.readTools = readTools;
	}

	@GetMapping("/files")
	WorkspaceFilesResponse listFiles(@RequestParam(name = "path", defaultValue = ".") String path) {
		return WorkspaceFilesResponse.from(this.readTools.listFiles(path, 500));
	}

	@GetMapping("/file")
	WorkspaceFileResponse readFile(@RequestParam("path") String path) {
		return WorkspaceFileResponse.from(this.readTools.readFile(path));
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
}
