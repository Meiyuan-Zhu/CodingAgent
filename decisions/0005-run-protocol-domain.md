# ADR-0005: Agent Run Protocol Domain

Date: 2026-08-27

Decision owner: assistant proposed and implemented the first backend core subtask after the user asked to proceed one subtask at a time.

State: accepted

Implementation status: verified for domain model and state-transition unit tests only.

Supersedes: none

## Problem

The assessment requires a self-written agent loop with conversation/context management, tool definitions and execution, response parsing, termination, and error handling. Before wiring HTTP APIs, model calls, tools, or SSE, the backend needs stable run protocol objects so every later layer can share the same state, event, and stop-reason vocabulary.

## Decision

Create a backend domain package `com.zhumeiyuan.codingagent.agent.run` containing:

- `RunId` for stable run identity.
- `RunStatus` for lifecycle states.
- `StopReason` for explicit terminal reasons.
- `RunEventType` and `RunEvent` for ordered run timeline events.
- `ToolCall` and `ToolResult` for model-requested local tool work.
- `AgentRun` for state transitions.
- `RunEventEnvelope` for pairing an emitted event with the run sequence update.

The domain stays independent of Spring MVC, SSE, Vue, persistence, and any model provider.

## Reasons

- The agent loop should not depend on Controller classes or frontend event rendering.
- Explicit terminal reasons make failures explainable in the UI and in interview discussion.
- Ordered events provide the base for SSE streaming and JSONL history later.
- Immutable snapshots for event payloads and tool arguments prevent later mutations from changing recorded facts.
- Unit-testable state transitions give a small safety net before adding model/tool side effects.

## Alternatives Considered

- Let Controllers define response DTOs first: quicker for UI, but risks coupling the core loop to HTTP.
- Store events as loose maps only: flexible, but easy to lose terminal reason and sequence invariants.
- Add a database schema now: unnecessary before run storage requirements are clear.

## Costs

- The first implementation adds types before visible user-facing behavior changes.
- `Map<String, Object>` payloads still need stricter typed event payloads or schema validation when externalized through API/SSE.
- Current tests cover lifecycle invariants, not concurrency, persistence, SSE replay, or real tool execution.

## Evidence

- Domain code exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/run/`.
- Unit tests exist under `backend/src/test/java/com/zhumeiyuan/codingagent/agent/run/`.
- `mvn test` passed on 2026-08-27 with 12 tests, including 10 run-protocol tests and 2 existing Spring Boot tests.

## Revisit

Revisit when SSE and persisted run history are implemented. If payload maps become hard to validate or render safely, introduce typed event payload records and explicit serialization DTOs.
