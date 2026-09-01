package com.zhumeiyuan.codingagent.agent.execution;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunEventType;
import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.run.RunStatus;
import com.zhumeiyuan.codingagent.agent.run.StopReason;

import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcAgentRunPersistence implements AgentRunPersistence {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcAgentRunPersistence(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public List<PersistedRun> loadRuns() {
		return this.jdbcTemplate.query("""
				SELECT run_id, status, created_at, updated_at, next_sequence, stop_reason, error_message
				FROM agent_runs
				ORDER BY created_at DESC
				""", (rs, rowNum) -> {
			RunId runId = RunId.from(rs.getString("run_id"));
			AgentRun run = new AgentRun(
					runId,
					RunStatus.valueOf(rs.getString("status")),
					Instant.parse(rs.getString("created_at")),
					Instant.parse(rs.getString("updated_at")),
					rs.getLong("next_sequence"),
					enumOrNull(StopReason.class, rs.getString("stop_reason")),
					rs.getString("error_message"));
			return new PersistedRun(run, loadEvents(runId), loadPendingApproval(runId));
		});
	}

	@Override
	public void saveRun(AgentRun run) {
		int updated = this.jdbcTemplate.update("""
				UPDATE agent_runs
				SET status = ?, created_at = ?, updated_at = ?, next_sequence = ?, stop_reason = ?, error_message = ?
				WHERE run_id = ?
				""",
				run.status().name(),
				run.createdAt().toString(),
				run.updatedAt().toString(),
				run.nextSequence(),
				run.stopReason() == null ? null : run.stopReason().name(),
				run.errorMessage(),
				run.id().value());
		if (updated == 0) {
			this.jdbcTemplate.update("""
					INSERT INTO agent_runs
					(run_id, status, created_at, updated_at, next_sequence, stop_reason, error_message)
					VALUES (?, ?, ?, ?, ?, ?, ?)
					""",
					run.id().value(),
					run.status().name(),
					run.createdAt().toString(),
					run.updatedAt().toString(),
					run.nextSequence(),
					run.stopReason() == null ? null : run.stopReason().name(),
					run.errorMessage());
		}
	}

	@Override
	public void insertEvent(RunEvent event) {
		this.jdbcTemplate.update("""
				INSERT INTO agent_run_events
				(run_id, event_sequence, event_id, occurred_at, event_type, payload_json)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				event.runId().value(),
				event.sequence(),
				event.eventId(),
				event.occurredAt().toString(),
				event.type().name(),
				writeJson(event.payload()));
	}

	@Override
	public void savePendingApproval(PendingToolApproval approval) {
		deletePendingApproval(approval.runId());
		this.jdbcTemplate.update("""
				INSERT INTO pending_tool_approvals (run_id, tool_call_id, payload_json)
				VALUES (?, ?, ?)
				""",
				approval.runId().value(),
				approval.toolCall().id(),
				writeJson(approval));
	}

	@Override
	public void deletePendingApproval(RunId runId) {
		this.jdbcTemplate.update("DELETE FROM pending_tool_approvals WHERE run_id = ?", runId.value());
	}

	private List<RunEvent> loadEvents(RunId runId) {
		return this.jdbcTemplate.query("""
				SELECT event_id, event_sequence, occurred_at, event_type, payload_json
				FROM agent_run_events
				WHERE run_id = ?
				ORDER BY event_sequence
				""", (rs, rowNum) -> new RunEvent(
				rs.getString("event_id"),
				runId,
				rs.getLong("event_sequence"),
				Instant.parse(rs.getString("occurred_at")),
				RunEventType.valueOf(rs.getString("event_type")),
				readMap(rs.getString("payload_json"))),
				runId.value());
	}

	private PendingToolApproval loadPendingApproval(RunId runId) {
		List<String> payloads = this.jdbcTemplate.query(
				"SELECT payload_json FROM pending_tool_approvals WHERE run_id = ?",
				(rs, rowNum) -> rs.getString("payload_json"),
				runId.value());
		return payloads.isEmpty() ? null : readJson(payloads.get(0), PendingToolApproval.class);
	}

	private Map<String, Object> readMap(String value) {
		try {
			return this.objectMapper.readValue(value, MAP_TYPE);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Cannot read run event payload JSON", ex);
		}
	}

	private <T> T readJson(String value, Class<T> type) {
		try {
			return this.objectMapper.readValue(value, type);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Cannot read persisted JSON for " + type.getSimpleName(), ex);
		}
	}

	private String writeJson(Object value) {
		try {
			return this.objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Cannot write persisted JSON", ex);
		}
	}

	private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
		return value == null ? null : Enum.valueOf(type, value);
	}
}
