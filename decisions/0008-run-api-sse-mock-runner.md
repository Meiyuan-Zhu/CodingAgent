# ADR-0008: Run API, SSE Event Stream, and Mock Runner

Date: 2026-08-27

Decision owner: assistant implemented after the user approved continuing from the tool registry to the runnable task flow.

State: accepted

Implementation status: verified for HTTP run creation/status/event replay, SSE replay format, mock runner execution, and Vue integration.

Supersedes: none

## Problem

The frontend workbench needs a real backend interaction loop before adding a real model provider. The project also needs an event protocol that can later carry model messages, tool calls, approvals, and terminal state without coupling the UI to the internal Java classes that execute tools.

## Decision

Add a backend execution and API layer:

- `AgentRunStore` keeps in-memory runs and ordered events.
- `AgentRunService` validates prompts, creates runs, emits initial events, and starts the runner.
- `MockAgentRunner` simulates model planning and executes one read-only workspace tool through `ToolRegistry`.
- `RunEventStream` provides SSE replay and live publishing using Spring MVC `SseEmitter`.
- `RunController` exposes:
  - `POST /api/runs`
  - `GET /api/runs/{runId}`
  - `GET /api/runs/{runId}/events`
  - `GET /api/runs/{runId}/events/stream`

Update the Vue workbench so the Run button creates a backend run and streams events into the timeline.

This is explicitly a mock runner. It verifies the run lifecycle, event stream, API shape, frontend integration, and tool registry execution path, but it is not a real model integration.

## Reasons

- Starting with a mock runner gives a deterministic full-stack test before model API cost, latency, and parsing failure enter the system.
- SSE matches the product shape: the UI should receive incremental events rather than polling a final result only.
- Event replay is required because the frontend may subscribe after the first events have already been emitted.
- In-memory storage is enough for the first vertical slice and avoids locking in a database schema too early.
- The runner uses `ToolRegistry` instead of direct workspace calls, proving the registry boundary is useful before real model integration.

## Alternatives Considered

- Poll-only API: simpler, but it would not match the intended Codex-like event experience.
- WebSocket: more flexible, but unnecessary before bidirectional control, cancellation, or approval interactions are implemented.
- Real model integration immediately: deferred because the event/tool loop should be testable without API keys.
- Persist runs to disk now: deferred until replay/history retention requirements are clearer.

## Costs

- Current run storage is process-local; restarting the backend loses run history.
- `MockAgentRunner` uses heuristic prompt matching and executes only one read-only tool.
- SSE subscriber tracking is intentionally simple and not yet tuned for many concurrent users.
- The frontend displays event payloads mostly as compact text; rich tool cards and diffs remain future work.

## Evidence

- Execution code exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/`.
- API code exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/api/`.
- Frontend integration exists in `frontend/src/App.vue` and `frontend/src/api/runs.ts`.
- `mvn test` passed on 2026-08-27 with 48 tests.
- `npm run build` passed on 2026-08-27.
- Local HTTP verification created a run, observed `SUCCEEDED`, replayed 10 events, and confirmed `list_files` executed through the registry.
- Local SSE verification returned `id/event/data` records and included `event:run_finished`.
- In-app browser verification opened the Vue workbench, clicked Run, displayed 10 streamed events, showed `Tool finished: list_files` and `Run finished`, and reported no console warnings/errors.

## Revisit

Revisit when implementing the real model adapter, cancellation, approval-required tool calls, persisted history, and richer frontend event rendering.
