# ADR-0012: Run Cancellation, Timeout, and Background Lifecycle

Date: 2026-08-27

Decision owner: assistant implemented after the user asked to continue with cancellation, timeout, and background task lifecycle management.

State: accepted

Implementation status: verified for cancel API, run task tracking, cooperative runner cancellation checks, tool execution timeout, frontend cancel action, backend tests, frontend build, and local HTTP cancel flow.

Supersedes: none

## Problem

The agent loop can now run multiple model/tool rounds, but it needs operational guardrails. A user must be able to stop a run, and a slow or stuck tool must not block the runner forever. The previous implementation submitted work to an executor without keeping a task handle, so the backend could create runs but could not manage their lifecycle after dispatch.

Cancellation and timeout also need to be visible in run state and events. A UI button alone is not enough; the core must own the state transition and publish a traceable event stream.

## Decision

Add a backend task lifecycle boundary:

- `RunTaskManager` tracks active run tasks by `RunId` and keeps `Future` handles.
- `AgentRunService.createRun` starts work through `RunTaskManager` instead of a bare executor.
- `POST /api/runs/{runId}/cancel` requests cancellation.
- Cancellation transitions non-terminal runs through `RUN_CANCELLING` and then `CANCELLED` with `USER_CANCELLED`.
- Terminal run cancellation is idempotent and does not append new events.
- The run executor and tool executor are separate `ExecutorService` beans and are shut down by Spring with `shutdownNow`.
- `AgentRunner` checks for cancellation before starting, before each model request, after model response, and before/after tool calls.
- Tool execution runs through a separate tool executor and waits up to `RunBudget.toolTimeout`.
- Timed-out tools return a failed `ToolResult` with `TOOL_TIMEOUT` metadata and fail the run with `TIME_LIMIT`.

The Vue workbench now includes a Cancel button and listens for `run_cancelling` SSE events.

## Reasons

- Task handles belong in the backend because only the backend owns the running Java thread and run state.
- Keeping run and tool executors separate prevents a stuck tool from occupying the run-loop thread while the runner waits with a clear timeout.
- Immediate `CANCELLED` state keeps the UI and history from hanging in `CANCELLING` if a background task ignores interruption.
- Runner checkpoint checks make cancellation observable even when the task is between tool calls.
- Timeout and cancellation are explicit stop reasons, which makes demo behavior and interview explanations more precise.

## Alternatives Considered

- Let the frontend close SSE to cancel a run: rejected because closing a stream does not stop backend work.
- Keep `CANCELLING` until the runner confirms interruption: rejected for now because cooperative interruption can be ignored; the user-visible run should not hang forever.
- Run tools synchronously in the runner thread and rely on tool code to be fast: rejected because future command execution and file operations need a timeout boundary.
- Add process-level killing now: deferred until the shell command tool exists. Java thread interruption is enough for current in-process tools.

## Costs

- Java `Future.cancel(true)` uses interruption; it cannot forcibly stop arbitrary Java code that ignores interrupts.
- A run may be marked `CANCELLED` before a misbehaving background operation has fully unwound. The runner suppresses later non-terminal events, but future shell tools must explicitly terminate child processes.
- Tool timeout currently wraps `ToolRegistry.execute`; individual tools do not yet receive a cancellation token.
- Default executor sizes are fixed in code and not yet externally configurable.

## Evidence

- `RunTaskManager` exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/`.
- `AgentRunService` exposes `cancelRun` and dispatches runs through `RunTaskManager`.
- `RunController` exposes `POST /api/runs/{runId}/cancel`.
- `AgentRunner` checks cancellation and applies tool timeout via `RunBudget.toolTimeout`.
- Vue workbench calls the cancel endpoint and listens to `run_cancelling` SSE events.
- `mvn test` passed on 2026-08-27 with 94 tests.
- `npm run build` passed on 2026-08-27.
- Local HTTP run `844f0b97-1a46-4955-bf44-2bc47e8b2802` ended as `CANCELLED` with `USER_CANCELLED`, with one `RUN_CANCELLING` and one `RUN_FINISHED` event.
- Implementation commit: `e9fdc0c feat: add run cancellation and tool timeouts`.

## Revisit

Shell command execution was added later as `run_command`; process-tree cleanup for interrupted commands is covered by [ADR-0032](0032-command-process-tree-cleanup.md). Revisit again if command execution needs interactive processes, long-running background jobs, stronger OS isolation, or timeout-specific exit metadata beyond the current recoverable observation.
