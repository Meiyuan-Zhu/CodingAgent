package com.zhumeiyuan.codingagent.agent.tool;

import java.time.Clock;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ToolConfiguration {

	@Bean
	Clock agentClock() {
		return Clock.systemUTC();
	}

	@Bean
	ToolRegistry toolRegistry(List<RegisteredTool> registeredTools, Clock clock) {
		return new ToolRegistry(registeredTools, clock);
	}
}
