# ADR-0010: Model Boundary and Response Parser

Date: 2026-08-27

Decision owner: assistant implemented after the user asked to continue feature work with maintainable code and traceable decisions.

State: accepted

Implementation status: verified for provider-neutral model request/response types, JSON response parsing, mock model client wiring, parser failure handling, and Spring bean loading.

Supersedes: none

## Problem

The agent loop cannot call a provider directly from controller or runner code without creating hard-to-test coupling. The project also needs to prove that model responses are treated as external input: they may be malformed, may request invalid tools, or may stop without tools. Java types alone do not validate a model response that arrives as text.

The immediate need is not a real API key integration. The need is a stable seam where a real model provider can later be connected while the core loop remains testable with a controllable substitute.

## Decision

Introduce a provider-neutral model package under `agent.model`:

- `ModelClient` is the boundary used by the runner.
- `ModelRequest` carries ordered messages and available `ToolDefinition` values.
- `ModelMessage`, `ModelRole`, `ModelResponse`, and `ModelFinishReason` express the core protocol without provider-specific DTOs.
- `ModelResponseParser` parses a small JSON contract from raw model text into `ModelResponse`.
- `HeuristicMockModelClient` keeps the current demo deterministic, but it now emits raw JSON and goes through the same parser path as a future provider adapter.

The accepted intermediate response contract is:

```json
{
  "message": "short assistant message",
  "finish_reason": "stop | tool_calls | length",
  "tool_calls": [
    {
      "id": "call-1",
      "name": "list_files",
      "arguments": { "path": "." }
    }
  ]
}
```

The parser validates blank/non-JSON responses, non-object roots, message length, unknown finish reasons, tool call array shape, maximum tool call count, required call IDs/names, duplicate call IDs, and object-shaped arguments. A parse failure ends the run with `MODEL_PARSE_ERROR`.

## Reasons

- A small `ModelClient` seam keeps provider HTTP details out of the runner, controller, SSE, and Vue code.
- Parsing raw JSON through one class makes the model boundary auditable and easy to test.
- Provider-neutral request/response types leave room for several API providers without forcing the rest of the application to know their SDK DTOs.
- Keeping the deterministic mock client allows local demos and CI-style tests without storing model keys.
- Explicit `MODEL_PARSE_ERROR` gives a clearer interview story than a generic runtime failure.

## Alternatives Considered

- Call a real model API immediately from `MockAgentRunner`: rejected because it would mix network/config concerns with core loop behavior and make tests require secrets.
- Let each provider adapter parse its own output directly into tool execution: rejected because validation rules would drift and parser failures would be harder to observe.
- Use a model API client's native tool-calling DTOs as the internal domain model: deferred because native tool calling may be useful later, but internal core state should remain provider-neutral.
- Parse free-form Markdown tool calls: rejected for this stage because JSON is easier to validate and explain.

## Costs

- The current mock client is still heuristic and does not demonstrate real model reasoning.
- The runner still performs only one model request and one batch of tool calls; it does not yet implement a full multi-round observe-think-act loop.
- The JSON response contract may need an adapter layer if the chosen provider returns a different native tool-call shape.
- Tool call authorization, execution budget, cancellation, and approval policy remain separate work.

## Evidence

- Model boundary code exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/`.
- `MockAgentRunner` now depends on `ModelClient` and handles `ModelParseException` as `MODEL_PARSE_ERROR`.
- Tests exist under `backend/src/test/java/com/zhumeiyuan/codingagent/agent/model/` and `backend/src/test/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunnerTests.java`.
- `mvn test` passed on 2026-08-27 with 78 tests.
- Implementation commit: `a58e31b feat: add model response parsing boundary`.

## Revisit

Revisit when selecting and implementing the first real model provider adapter. At that point decide whether to use provider-native tool calling, prompt-constrained JSON, or a hybrid adapter that normalizes native responses into `ModelResponse`.
