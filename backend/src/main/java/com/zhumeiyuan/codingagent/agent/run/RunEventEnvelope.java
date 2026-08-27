package com.zhumeiyuan.codingagent.agent.run;

import java.util.Objects;

public record RunEventEnvelope(AgentRun run, RunEvent event) {

	public RunEventEnvelope {
		Objects.requireNonNull(run, "run");
		Objects.requireNonNull(event, "event");
		if (!run.id().equals(event.runId())) {
			throw new IllegalArgumentException("Envelope run and event must share the same run id");
		}
		if (run.nextSequence() != event.sequence() + 1) {
			throw new IllegalArgumentException("Envelope run sequence must be one greater than event sequence");
		}
	}
}
