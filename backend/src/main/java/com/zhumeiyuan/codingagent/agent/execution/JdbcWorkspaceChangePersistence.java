package com.zhumeiyuan.codingagent.agent.execution;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceChangeUndoResult;

import org.springframework.jdbc.core.JdbcTemplate;

final class JdbcWorkspaceChangePersistence implements WorkspaceChangePersistence {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcWorkspaceChangePersistence(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public List<PersistedWorkspaceChange> loadChanges() {
		return this.jdbcTemplate.query("""
				SELECT run_id, tool_call_id, snapshot_json, state, result_json
				FROM workspace_change_undo
				""", (rs, rowNum) -> new PersistedWorkspaceChange(
				RunId.from(rs.getString("run_id")),
				rs.getString("tool_call_id"),
				readJson(rs.getString("snapshot_json"), WorkspaceChangeUndoSnapshot.class),
				WorkspaceChangeUndoState.valueOf(rs.getString("state")),
				readNullableJson(rs.getString("result_json"), WorkspaceChangeUndoResult.class)));
	}

	@Override
	public void saveChange(RunId runId, String toolCallId, WorkspaceChangeUndoSnapshot snapshot,
			WorkspaceChangeUndoState state, WorkspaceChangeUndoResult result) {
		int updated = this.jdbcTemplate.update("""
				UPDATE workspace_change_undo
				SET snapshot_json = ?, state = ?, result_json = ?
				WHERE run_id = ? AND tool_call_id = ?
				""",
				writeJson(snapshot),
				state.name(),
				result == null ? null : writeJson(result),
				runId.value(),
				toolCallId);
		if (updated == 0) {
			this.jdbcTemplate.update("""
					INSERT INTO workspace_change_undo
					(run_id, tool_call_id, snapshot_json, state, result_json)
					VALUES (?, ?, ?, ?, ?)
					""",
					runId.value(),
					toolCallId,
					writeJson(snapshot),
					state.name(),
					result == null ? null : writeJson(result));
		}
	}

	private <T> T readNullableJson(String value, Class<T> type) {
		return value == null ? null : readJson(value, type);
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
			throw new IllegalStateException("Cannot write persisted workspace change JSON", ex);
		}
	}
}
