# ADR-0007: Tool Registry Boundary

Date: 2026-08-27

Decision owner: assistant implemented after the user approved the tool-registry subtask.

State: accepted

Implementation status: verified for in-process registry, argument validation, workspace read-tool registration, and Spring bean wiring.

Supersedes: none

## Problem

The agent loop will receive tool calls from a model, but it should not know how each Java service is constructed or how workspace errors are represented internally. Without a registry boundary, later code would likely call `WorkspaceReadTools` directly, scatter argument validation across the loop, and make it harder to add command/write tools safely.

## Decision

Add a backend tool layer under `com.zhumeiyuan.codingagent.agent.tool`:

- `ToolDefinition` describes each tool name, description, and JSON-schema-like input contract.
- `RegisteredTool` binds a definition to a handler.
- `ToolArgumentReader` validates untrusted model-provided arguments at runtime.
- `ToolRegistry` exposes sorted definitions and executes `ToolCall` objects into `ToolResult` objects.
- `ToolExecutionErrorCode` and `ToolExecutionException` normalize known failure types.

Add `com.zhumeiyuan.codingagent.agent.tool.workspace` to register the current read-only workspace tools as:

- `list_files`
- `read_file`
- `search_text`

The registry is an in-process Spring bean. It is not yet exposed through HTTP, SSE, a model adapter, or a full agent loop.

## Reasons

- The agent loop needs a small stable boundary: model tool name plus JSON arguments in, tool result out.
- Runtime argument validation is required because Java types cannot protect data coming from a model response.
- Sorting definitions gives deterministic prompts and tests.
- Mapping unknown tools, invalid arguments, workspace denials, and unexpected runtime failures into `ToolResult` keeps later loop control explicit.
- Keeping workspace adapters separate from `WorkspaceReadTools` preserves the existing workspace safety layer and avoids making it model-aware.

## Alternatives Considered

- Call workspace services directly from the future loop: quicker, but would couple the loop to one tool category and duplicate error handling.
- Add a general plugin system now: too broad for the assessment schedule and current tool set.
- Use provider-specific tool classes now: deferred so the core registry remains independent of OpenAI-compatible, Anthropic-compatible, or other model APIs.
- Return raw Java objects from handlers: rejected because the model-facing boundary needs serialized, recordable content.

## Costs

- The registry adds a small adapter layer before visible UI behavior changes.
- The current schema format is intentionally provider-neutral and may need a translation layer for each real model API.
- Tool result content is JSON text, so callers must parse it if they need structured display.
- Only read-only workspace tools are registered; write/edit/command tools still need their own policies and tests.

## Evidence

- Tool registry code exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/`.
- Workspace tool adapters exist under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/`.
- Tests exist under `backend/src/test/java/com/zhumeiyuan/codingagent/agent/tool/`.
- `mvn test` passed on 2026-08-27 with 39 tests, including registry, argument validation, workspace adapter, and Spring context wiring tests.

## Revisit

Revisit when implementing the model adapter. At that point, add a provider-specific translation from `ToolDefinition` into the chosen model API's tool schema, without leaking provider classes into the registry itself.
