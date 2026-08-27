package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.zhumeiyuan.codingagent.agent.model.ModelClient;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalPolicy;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
	ToolApprovalPolicy toolApprovalPolicy() {
		return new ToolApprovalPolicy();
	}

	@Bean(destroyMethod = "shutdownNow")
	ExecutorService agentRunExecutor() {
		return Executors.newFixedThreadPool(2, runnable -> {
			Thread thread = new Thread(runnable);
			thread.setName("agent-run-" + thread.threadId());
			return thread;
		});
	}

	@Bean(destroyMethod = "shutdownNow")
	ExecutorService agentToolExecutor() {
		return Executors.newFixedThreadPool(2, runnable -> {
			Thread thread = new Thread(runnable);
			thread.setName("agent-tool-" + thread.threadId());
			return thread;
		});
	}

	@Bean
	RunTaskManager runTaskManager(@Qualifier("agentRunExecutor") ExecutorService agentRunExecutor) {
		return new RunTaskManager(agentRunExecutor);
	}

	@Bean
	MockAgentRunner mockAgentRunner(AgentRunStore store, RunEventStream runEventStream, ToolRegistry toolRegistry,
			ToolApprovalPolicy toolApprovalPolicy, ModelClient modelClient, RunBudget runBudget,
			@Qualifier("agentToolExecutor") ExecutorService agentToolExecutor, Clock clock) {
		return new MockAgentRunner(store, runEventStream, toolRegistry, toolApprovalPolicy, modelClient, runBudget,
				agentToolExecutor, clock);
	}

	@Bean
	AgentRunService agentRunService(AgentRunStore store, RunEventStream runEventStream, MockAgentRunner mockAgentRunner,
			RunTaskManager runTaskManager, Clock clock) {
		return new AgentRunService(store, runEventStream, mockAgentRunner, runTaskManager, clock);
	}
}
