package com.zhumeiyuan.codingagent.agent.model;

public interface StreamingModelClient extends ModelClient {

	ModelResponse completeStreaming(ModelRequest request, ModelStreamListener listener);
}
