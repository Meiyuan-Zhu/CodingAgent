package com.zhumeiyuan.codingagent.agent.model;

@FunctionalInterface
public interface ModelHttpTransport {

	ModelHttpResponse send(ModelHttpRequest request);
}
