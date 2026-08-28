package com.zhumeiyuan.codingagent.agent.model;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record ModelHttpRequest(URI uri, Map<String, String> headers, String body, Duration timeout) {

	public ModelHttpRequest {
		Objects.requireNonNull(uri, "uri");
		headers = Map.copyOf(Objects.requireNonNull(headers, "headers"));
		Objects.requireNonNull(body, "body");
		Objects.requireNonNull(timeout, "timeout");
	}
}
