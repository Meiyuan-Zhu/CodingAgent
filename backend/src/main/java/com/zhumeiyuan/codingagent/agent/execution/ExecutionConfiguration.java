package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.model.ModelClient;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalPolicy;
import com.zhumeiyuan.codingagent.agent.tool.ToolRegistry;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceWriteTools;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class ExecutionConfiguration {

	@Bean
	AgentRunStore agentRunStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		return new AgentRunStore(new JdbcAgentRunPersistence(jdbcTemplate, objectMapper));
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

	@Bean
	WorkspaceChangeJournal workspaceChangeJournal(WorkspaceWriteTools workspaceWriteTools, JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper) {
		return new WorkspaceChangeJournal(workspaceWriteTools,
				new JdbcWorkspaceChangePersistence(jdbcTemplate, objectMapper));
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
			WorkspaceChangeJournal workspaceChangeJournal,
			@Qualifier("agentToolExecutor") ExecutorService agentToolExecutor, Clock clock) {
		return new MockAgentRunner(store, runEventStream, toolRegistry, toolApprovalPolicy, modelClient, runBudget,
				workspaceChangeJournal, agentToolExecutor, clock);
	}

	@Bean
	AgentRunService agentRunService(AgentRunStore store, RunEventStream runEventStream, MockAgentRunner mockAgentRunner,
			RunTaskManager runTaskManager, WorkspaceChangeJournal workspaceChangeJournal, Clock clock) {
		return new AgentRunService(store, runEventStream, mockAgentRunner, runTaskManager, workspaceChangeJournal, clock);
	}
}
