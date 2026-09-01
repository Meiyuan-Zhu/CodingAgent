package com.zhumeiyuan.codingagent.agent.model;

@FunctionalInterface
public interface ModelHttpTransport {

	ModelHttpResponse send(ModelHttpRequest request);

	default ModelHttpStreamResponse stream(ModelHttpRequest request) {
		ModelHttpResponse response = send(request);
		return new ModelHttpStreamResponse(response.statusCode(), response.body().lines());
	}
}
