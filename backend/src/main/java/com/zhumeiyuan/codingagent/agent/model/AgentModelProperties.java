package com.zhumeiyuan.codingagent.agent.model;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.model")
public class AgentModelProperties {

	private Provider provider = Provider.MOCK;

	private String baseUrl = "https://api.deepseek.com";

	private String name = "deepseek-v4-flash";

	private String apiKeyEnv = "DEEPSEEK_API_KEY";

	private Duration timeout = Duration.ofSeconds(60);

	private double temperature = 0.2;

	public Provider getProvider() {
		return this.provider;
	}

	public void setProvider(Provider provider) {
		this.provider = provider == null ? Provider.MOCK : provider;
	}

	public String getBaseUrl() {
		return this.baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getApiKeyEnv() {
		return this.apiKeyEnv;
	}

	public void setApiKeyEnv(String apiKeyEnv) {
		this.apiKeyEnv = apiKeyEnv;
	}

	public Duration getTimeout() {
		return this.timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	public double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public enum Provider {
		MOCK,
		OPENAI_COMPATIBLE
	}
}
