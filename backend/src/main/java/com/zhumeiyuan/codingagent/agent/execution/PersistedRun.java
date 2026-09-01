package com.zhumeiyuan.codingagent.agent.execution;

import java.util.List;
import java.util.Objects;

import com.zhumeiyuan.codingagent.agent.run.AgentRun;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;

record PersistedRun(AgentRun run, List<RunEvent> events, PendingToolApproval pendingApproval) {

	PersistedRun {
		Objects.requireNonNull(run, "run");
		events = List.copyOf(Objects.requireNonNull(events, "events"));
	}
}
