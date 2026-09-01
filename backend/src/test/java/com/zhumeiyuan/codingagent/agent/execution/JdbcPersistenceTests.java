package com.zhumeiyuan.codingagent.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.model.ModelMessage;
import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.ToolCall;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalDecision;
import com.zhumeiyuan.codingagent.agent.tool.ToolApprovalMode;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceChangeUndoResult;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class JdbcPersistenceTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC);
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void runStoreReloadsRunsEventsAndPendingApproval() {
		JdbcTemplate jdbcTemplate = jdbcTemplate();
		AgentRunStore firstStore = new AgentRunStore(new JdbcAgentRunPersistence(jdbcTemplate, this.objectMapper));
		AgentRun run = firstStore.create(this.clock);
		firstStore.appendEvent(run.id(), RunEventType.RUN_CREATED, Map.of("runId", run.id().value()), this.clock);
		firstStore.appendEvent(run.id(), RunEventType.USER_MESSAGE_ACCEPTED, Map.of("prompt", "persist me"), this.clock);
		firstStore.transition(run.id(), current -> current.start(this.clock).waitForApproval(this.clock));
		firstStore.savePendingApproval(new PendingToolApproval(
				run.id(),
				1,
				new ToolCall("call-1", "write_file", Map.of("path", "x.txt", "content", "x")),
				new ToolApprovalDecision(ToolApprovalMode.USER_APPROVAL_REQUIRED, "test approval"),
				List.of(ModelMessage.system("system"), ModelMessage.user("persist me")),
				0));

		AgentRunStore reloadedStore = new AgentRunStore(new JdbcAgentRunPersistence(jdbcTemplate, this.objectMapper));

		assertThat(reloadedStore.listRuns()).extracting(AgentRun::id).contains(run.id());
		assertThat(reloadedStore.get(run.id()).nextSequence()).isEqualTo(2);
		assertThat(reloadedStore.listEvents(run.id(), -1)).extracting(event -> event.type())
				.containsExactly(RunEventType.RUN_CREATED, RunEventType.USER_MESSAGE_ACCEPTED);
		PendingToolApproval approval = reloadedStore.consumePendingApproval(run.id(), "call-1");
		assertThat(approval.toolCall().name()).isEqualTo("write_file");
		assertThat(approval.messages()).hasSize(2);
	}

	@Test
	void workspaceChangePersistenceReloadsUndoSnapshotAndResult() {
		JdbcTemplate jdbcTemplate = jdbcTemplate();
		JdbcWorkspaceChangePersistence persistence = new JdbcWorkspaceChangePersistence(jdbcTemplate, this.objectMapper);
		AgentRun run = AgentRun.create(this.clock);
		WorkspaceChangeUndoSnapshot snapshot = new WorkspaceChangeUndoSnapshot("src/x.txt", true, null, "abc123");
		WorkspaceChangeUndoResult result = new WorkspaceChangeUndoResult("src/x.txt", true, false, "abc123", null,
				"--- a/src/x.txt\n+++ b/src/x.txt\n");

		persistence.saveChange(run.id(), "call-1", snapshot, WorkspaceChangeUndoState.UNDOABLE, null);
		persistence.saveChange(run.id(), "call-1", snapshot, WorkspaceChangeUndoState.UNDONE, result);

		List<PersistedWorkspaceChange> changes = new JdbcWorkspaceChangePersistence(jdbcTemplate, this.objectMapper)
				.loadChanges();

		assertThat(changes).hasSize(1);
		PersistedWorkspaceChange change = changes.get(0);
		assertThat(change.runId()).isEqualTo(run.id());
		assertThat(change.toolCallId()).isEqualTo("call-1");
		assertThat(change.snapshot()).isEqualTo(snapshot);
		assertThat(change.state()).isEqualTo(WorkspaceChangeUndoState.UNDONE);
		assertThat(change.result()).isEqualTo(result);
	}

	private JdbcTemplate jdbcTemplate() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource());
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS agent_runs (
					run_id VARCHAR(64) PRIMARY KEY,
					status VARCHAR(64) NOT NULL,
					created_at VARCHAR(64) NOT NULL,
					updated_at VARCHAR(64) NOT NULL,
					next_sequence BIGINT NOT NULL,
					stop_reason VARCHAR(64),
					error_message CLOB
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS agent_run_events (
					run_id VARCHAR(64) NOT NULL,
					event_sequence BIGINT NOT NULL,
					event_id VARCHAR(64) NOT NULL,
					occurred_at VARCHAR(64) NOT NULL,
					event_type VARCHAR(64) NOT NULL,
					payload_json CLOB NOT NULL,
					PRIMARY KEY (run_id, event_sequence)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS pending_tool_approvals (
					run_id VARCHAR(64) PRIMARY KEY,
					tool_call_id VARCHAR(256) NOT NULL,
					payload_json CLOB NOT NULL
				)
				""");
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS workspace_change_undo (
					run_id VARCHAR(64) NOT NULL,
					tool_call_id VARCHAR(256) NOT NULL,
					snapshot_json CLOB NOT NULL,
					state VARCHAR(64) NOT NULL,
					result_json CLOB,
					PRIMARY KEY (run_id, tool_call_id)
				)
				""");
		return jdbcTemplate;
	}

	private DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		return dataSource;
	}
}
