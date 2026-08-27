package com.zhumeiyuan.codingagent.agent.tool;

public class ToolExecutionException extends RuntimeException {

	private final ToolExecutionErrorCode code;
	private final String toolName;

	public ToolExecutionException(ToolExecutionErrorCode code, String toolName, String message) {
		super(message);
		this.code = code;
		this.toolName = toolName;
	}

	public ToolExecutionException(ToolExecutionErrorCode code, String toolName, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.toolName = toolName;
	}

	public ToolExecutionErrorCode code() {
		return this.code;
	}

	public String toolName() {
		return this.toolName;
	}
}
