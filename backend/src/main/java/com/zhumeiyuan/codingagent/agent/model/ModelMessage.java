package com.zhumeiyuan.codingagent.agent.model;

public record ModelMessage(ModelRole role, String content) {

	public ModelMessage {
		if (role == null) {
			throw new IllegalArgumentException("Model message role must not be null");
		}
		if (content == null) {
			throw new IllegalArgumentException("Model message content must not be null");
		}
	}

	public static ModelMessage system(String content) {
		return new ModelMessage(ModelRole.SYSTEM, content);
	}

	public static ModelMessage user(String content) {
		return new ModelMessage(ModelRole.USER, content);
	}

	public static ModelMessage assistant(String content) {
		return new ModelMessage(ModelRole.ASSISTANT, content);
	}

	public static ModelMessage tool(String content) {
		return new ModelMessage(ModelRole.TOOL, content);
	}
}
