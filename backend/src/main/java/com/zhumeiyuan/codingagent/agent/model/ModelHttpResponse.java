package com.zhumeiyuan.codingagent.agent.model;

import java.util.Objects;

public record ModelHttpResponse(int statusCode, String body) {

	public ModelHttpResponse {
		Objects.requireNonNull(body, "body");
	}
}
