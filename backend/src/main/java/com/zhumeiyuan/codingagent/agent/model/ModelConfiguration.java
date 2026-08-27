package com.zhumeiyuan.codingagent.agent.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ModelConfiguration {

	@Bean
	ModelResponseParser modelResponseParser(ObjectMapper objectMapper) {
		return new ModelResponseParser(objectMapper);
	}

	@Bean
	ModelClient modelClient(ModelResponseParser parser, ObjectMapper objectMapper) {
		return new HeuristicMockModelClient(parser, objectMapper);
	}
}
