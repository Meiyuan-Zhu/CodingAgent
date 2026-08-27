package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.concurrent.Executor;

import com.zhumeiyuan.codingagent.agent.model.ModelClient;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
class ExecutionConfiguration {

	@Bean
	AgentRunStore agentRunStore() {
		return new AgentRunStore();
	}

	@Bean
	RunEventStream runEventStream() {
		return new RunEventStream();
	}

	@Bean
	RunBudget runBudget() {
		return RunBudget.defaults();
	}

	@Bean
	MockAgentRunner mockAgentRunner(AgentRunStore store, RunEventStream runEventStream, ToolRegistry toolRegistry,
			ModelClient modelClient, RunBudget runBudget, Clock clock) {
		return new MockAgentRunner(store, runEventStream, toolRegistry, modelClient, runBudget, clock);
	}

	@Bean
	Executor agentRunExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("agent-run-");
		executor.setConcurrencyLimit(2);
		return executor;
	}

	@Bean
	AgentRunService agentRunService(AgentRunStore store, RunEventStream runEventStream, MockAgentRunner mockAgentRunner,
			Executor agentRunExecutor, Clock clock) {
		return new AgentRunService(store, runEventStream, mockAgentRunner, agentRunExecutor, clock);
	}
}
