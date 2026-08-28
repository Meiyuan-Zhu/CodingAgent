package com.zhumeiyuan.codingagent.agent.workspace;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WorkspaceProperties.class)
class WorkspaceConfiguration {

	@Bean
	WorkspacePathResolver workspacePathResolver(WorkspaceProperties properties) {
		return new WorkspacePathResolver(properties.getRoot());
	}

	@Bean
	WorkspaceReadTools workspaceReadTools(WorkspacePathResolver resolver) {
		return new WorkspaceReadTools(resolver);
	}

	@Bean
	WorkspaceWriteTools workspaceWriteTools(WorkspacePathResolver resolver) {
		return new WorkspaceWriteTools(resolver);
	}

	@Bean
	WorkspaceCommandTools workspaceCommandTools(WorkspacePathResolver resolver, java.time.Clock clock) {
		return new WorkspaceCommandTools(resolver, clock);
	}
}
