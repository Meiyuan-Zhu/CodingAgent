package com.zhumeiyuan.codingagent.agent.model;

public class ModelClientException extends RuntimeException {

	public ModelClientException(String message) {
		super(message);
	}

	public ModelClientException(String message, Throwable cause) {
		super(message, cause);
	}
}
