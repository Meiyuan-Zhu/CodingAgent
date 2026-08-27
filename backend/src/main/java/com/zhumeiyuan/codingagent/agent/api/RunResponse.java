package com.zhumeiyuan.codingagent.agent.api;

import java.time.Instant;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunStatus;
import com.zhumeiyuan.codingagent.agent.run.StopReason;

public record RunResponse(
		String id,
		RunStatus status,
		Instant createdAt,
		Instant updatedAt,
		long nextSequence,
		StopReason stopReason,
		String errorMessage) {

	public static RunResponse from(AgentRun run) {
		return new RunResponse(run.id().value(), run.status(), run.createdAt(), run.updatedAt(), run.nextSequence(),
				run.stopReason(), run.errorMessage());
	}
}
