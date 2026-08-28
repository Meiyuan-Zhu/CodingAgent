package com.zhumeiyuan.codingagent.agent.model;

@FunctionalInterface
public interface ModelClient {

	ModelResponse complete(ModelRequest request);

	default String providerName() {
		return "unknown";
	}
}
