package com.zhumeiyuan.codingagent.agent.model;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class JavaHttpModelTransport implements ModelHttpTransport {

	private final HttpClient httpClient;

	public JavaHttpModelTransport(HttpClient httpClient) {
		this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
	}

	@Override
	public ModelHttpResponse send(ModelHttpRequest request) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
				.timeout(request.timeout())
				.POST(HttpRequest.BodyPublishers.ofString(request.body()));
		request.headers().forEach(builder::header);
		try {
			HttpResponse<String> response = this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
			return new ModelHttpResponse(response.statusCode(), response.body());
		} catch (IOException ex) {
			throw new ModelClientException("Model HTTP request failed", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ModelClientException("Model HTTP request was interrupted", ex);
		}
	}

	@Override
	public ModelHttpStreamResponse stream(ModelHttpRequest request) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
				.timeout(request.timeout())
				.POST(HttpRequest.BodyPublishers.ofString(request.body()));
		request.headers().forEach(builder::header);
		try {
			HttpResponse<java.util.stream.Stream<String>> response =
					this.httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofLines());
			return new ModelHttpStreamResponse(response.statusCode(), response.body());
		} catch (IOException ex) {
			throw new ModelClientException("Model HTTP streaming request failed", ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ModelClientException("Model HTTP streaming request was interrupted", ex);
		}
	}
}
