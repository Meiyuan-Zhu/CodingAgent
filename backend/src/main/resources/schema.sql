CREATE TABLE IF NOT EXISTS agent_runs (
	run_id VARCHAR(64) PRIMARY KEY,
	status VARCHAR(64) NOT NULL,
	created_at VARCHAR(64) NOT NULL,
	updated_at VARCHAR(64) NOT NULL,
	next_sequence BIGINT NOT NULL,
	stop_reason VARCHAR(64),
	error_message CLOB
);

CREATE TABLE IF NOT EXISTS agent_run_events (
	run_id VARCHAR(64) NOT NULL,
	event_sequence BIGINT NOT NULL,
	event_id VARCHAR(64) NOT NULL,
	occurred_at VARCHAR(64) NOT NULL,
	event_type VARCHAR(64) NOT NULL,
	payload_json CLOB NOT NULL,
	PRIMARY KEY (run_id, event_sequence)
);

CREATE TABLE IF NOT EXISTS pending_tool_approvals (
	run_id VARCHAR(64) PRIMARY KEY,
	tool_call_id VARCHAR(256) NOT NULL,
	payload_json CLOB NOT NULL
);

CREATE TABLE IF NOT EXISTS workspace_change_undo (
	run_id VARCHAR(64) NOT NULL,
	tool_call_id VARCHAR(256) NOT NULL,
	snapshot_json CLOB NOT NULL,
	state VARCHAR(64) NOT NULL,
	result_json CLOB,
	PRIMARY KEY (run_id, tool_call_id)
);
