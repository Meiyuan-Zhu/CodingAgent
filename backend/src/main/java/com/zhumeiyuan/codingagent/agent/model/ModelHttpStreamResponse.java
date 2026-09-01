package com.zhumeiyuan.codingagent.agent.model;

import java.util.Objects;
import java.util.stream.Stream;

public record ModelHttpStreamResponse(int statusCode, Stream<String> lines) implements AutoCloseable {

	public ModelHttpStreamResponse {
		Objects.requireNonNull(lines, "lines");
	}

	@Override
	public void close() {
		this.lines.close();
	}
}
