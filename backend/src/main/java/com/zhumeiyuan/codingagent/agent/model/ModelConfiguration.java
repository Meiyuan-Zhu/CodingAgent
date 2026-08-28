package com.zhumeiyuan.codingagent.agent.model;

import java.net.http.HttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentModelProperties.class)
class ModelConfiguration {

	@Bean
	ModelResponseParser modelResponseParser(ObjectMapper objectMapper) {
		return new ModelResponseParser(objectMapper);
	}

	@Bean
	ModelHttpTransport modelHttpTransport() {
		return new JavaHttpModelTransport(HttpClient.newHttpClient());
	}

	@Bean
	ModelClient modelClient(AgentModelProperties properties, ModelResponseParser parser, ObjectMapper objectMapper,
			ModelHttpTransport transport) {
		return switch (properties.getProvider()) {
			case MOCK -> new HeuristicMockModelClient(parser, objectMapper);
			case OPENAI_COMPATIBLE -> new OpenAiCompatibleModelClient(properties, parser, objectMapper, transport,
					System::getenv);
		};
	}
}
