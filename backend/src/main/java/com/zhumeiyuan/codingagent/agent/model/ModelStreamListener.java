package com.zhumeiyuan.codingagent.agent.model;

@FunctionalInterface
public interface ModelStreamListener {

	void onTextDelta(String delta);
}
