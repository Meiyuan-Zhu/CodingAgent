package com.zhumeiyuan.codingagent.agent.workspace;

public class WorkspaceAccessException extends RuntimeException {

	private final WorkspaceAccessCode code;
	private final String requestedPath;

	public WorkspaceAccessException(WorkspaceAccessCode code, String requestedPath, String message) {
		super(message);
		this.code = code;
		this.requestedPath = requestedPath;
	}

	public WorkspaceAccessException(WorkspaceAccessCode code, String requestedPath, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.requestedPath = requestedPath;
	}

	public WorkspaceAccessCode code() {
		return this.code;
	}

	public String requestedPath() {
		return this.requestedPath;
	}
}
