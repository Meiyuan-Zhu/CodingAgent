package com.zhumeiyuan.codingagent.agent.api;

import java.util.List;

import com.zhumeiyuan.codingagent.agent.execution.AgentRunService;
import com.zhumeiyuan.codingagent.agent.execution.RunEventStream;
import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunId;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/runs")
class RunController {

	private final AgentRunService agentRunService;
	private final RunEventStream runEventStream;

	RunController(AgentRunService agentRunService, RunEventStream runEventStream) {
		this.agentRunService = agentRunService;
		this.runEventStream = runEventStream;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	RunResponse createRun(@Valid @RequestBody CreateRunRequest request) {
		return RunResponse.from(this.agentRunService.createRun(request.prompt()));
	}

	@GetMapping
	List<RunResponse> listRuns() {
		return this.agentRunService.listRuns().stream()
				.map(RunResponse::from)
				.toList();
	}

	@GetMapping("/{runId}")
	RunResponse getRun(@PathVariable String runId) {
		return RunResponse.from(this.agentRunService.getRun(RunId.from(runId)));
	}

	@PostMapping("/{runId}/cancel")
	RunResponse cancelRun(@PathVariable String runId) {
		return RunResponse.from(this.agentRunService.cancelRun(RunId.from(runId)));
	}

	@PostMapping("/{runId}/approvals/{toolCallId}/approve")
	RunResponse approveToolCall(@PathVariable String runId, @PathVariable String toolCallId) {
		return RunResponse.from(this.agentRunService.approveToolCall(RunId.from(runId), toolCallId));
	}

	@PostMapping("/{runId}/approvals/{toolCallId}/reject")
	RunResponse rejectToolCall(@PathVariable String runId, @PathVariable String toolCallId) {
		return RunResponse.from(this.agentRunService.rejectToolCall(RunId.from(runId), toolCallId));
	}

	@PostMapping("/{runId}/changes/{toolCallId}/undo")
	UndoWorkspaceChangeResponse undoWorkspaceChange(@PathVariable String runId, @PathVariable String toolCallId) {
		return UndoWorkspaceChangeResponse.from(
				this.agentRunService.undoWorkspaceChange(RunId.from(runId), toolCallId));
	}

	@GetMapping("/{runId}/events")
	List<RunEvent> listRunEvents(@PathVariable String runId,
			@RequestParam(name = "after", defaultValue = "-1") long afterSequence) {
		return this.agentRunService.listEvents(RunId.from(runId), afterSequence);
	}

	@GetMapping(path = "/{runId}/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter streamRunEvents(@PathVariable String runId,
			@RequestParam(name = "after", defaultValue = "-1") long afterSequence) {
		RunId id = RunId.from(runId);
		List<RunEvent> replay = this.agentRunService.listEvents(id, afterSequence);
		boolean terminal = this.agentRunService.getRun(id).status().isTerminal();
		return this.runEventStream.subscribe(id, replay, terminal);
	}
}
