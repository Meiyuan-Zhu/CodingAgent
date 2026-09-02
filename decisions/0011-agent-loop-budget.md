# ADR-0011: Multi-Round Agent Loop and Run Budget

Date: 2026-08-27

Decision owner: assistant implemented after the user asked whether a real model should be connected now and approved continuing according to the staged plan.

State: accepted

Implementation status: verified for multi-round model/tool loop, round limit, tool-call limit, context message window, length handling, HTTP run flow, and frontend build.

Supersedes: none

## Problem

After the model boundary was introduced, the runner still made only one model request and executed one batch of tool calls. That shape is enough for a mock demo, but it is not a credible coding agent loop. A coding agent must observe tool results, send them back into the next model request, and continue until the model stops or a guardrail ends the run.

Connecting a real model before these guardrails would make failures harder to diagnose. A bad run could be caused by provider configuration, model output, missing context, an infinite tool loop, or missing budget handling. The project needs explicit control before adding a nondeterministic provider.

## Decision

Add `RunBudget` and upgrade `AgentRunner` into a bounded multi-round loop:

1. Start the run and emit the configured budget.
2. Build model context with a system message and the user prompt.
3. Request the model for each round.
4. If the model returns `STOP`, mark the run as succeeded.
5. If the model returns `LENGTH`, fail with `TOKEN_BUDGET_LIMIT`.
6. If the model returns tool calls, check the total tool-call budget before executing them.
7. Execute each tool through `ToolRegistry`, append a tool observation message, then continue to the next round.
8. If the model keeps asking for tools after the maximum number of rounds, fail with `ROUND_LIMIT`.
9. If the model asks for more tool calls than allowed, fail with `TOOL_CALL_LIMIT`.

The default budget is intentionally small and explainable:

- `maxRounds = 4`
- `maxToolCalls = 12`
- `maxContextMessages = 30`

When the context grows beyond the message window, the runner keeps the system prompt and the most recent messages. This is not a final token-budget algorithm, but it establishes where context truncation belongs.

## Reasons

- Multi-round loop is core agent behavior and should be deterministic-testable before real provider integration.
- Round and tool-call limits prevent runaway loops during local demos and future real-model runs.
- Tool observations as messages make the loop easy to explain: model decides, tool acts, result returns to model.
- `RunBudget` is a small domain object rather than scattered constants, so later configuration can be added without hunting through runner logic.
- Keeping the provider as `HeuristicMockModelClient` for now avoids putting API keys or network instability into core-loop tests.

## Alternatives Considered

- Connect a real model immediately: rejected for this stage because control flow and budget failures should be isolated before provider behavior is introduced.
- Add only a round loop without budgets: rejected because an agent loop without guardrails is unsafe and hard to demo reliably.
- Implement full token counting now: deferred because provider-specific tokenizers and prompt formatting are not selected yet. Message-window trimming is enough to define the boundary.
- Persist full run context to disk now: deferred because the current run store is still in-memory; persistence should be designed together with run history.

## Costs

- At this stage the runner still used the heuristic mock model provider by default; it has since been renamed to the provider-agnostic `AgentRunner`.
- Message-count trimming is coarser than token-count trimming.
- There is no cancellation or timeout yet, so long-running tools still need a separate lifecycle policy.
- The current tool observation format is plain text; a future provider adapter may benefit from structured native tool result messages.

## Evidence

- `RunBudget` exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/`.
- `AgentRunner` now performs multiple model rounds and appends tool observations to the next request.
- `StopReason` includes `TOOL_CALL_LIMIT` for explicit tool budget failures.
- Tests cover multi-round success, model parse failure, tool failure, length failure, round limit, tool-call limit, and context-window trimming.
- `mvn test` passed on 2026-08-27 with 85 tests.
- `npm run build` passed on 2026-08-27.
- Local HTTP run on port 18080 produced 11 events, 2 `MODEL_REQUESTED` events, 1 `TOOL_CALL_FINISHED` event, and final status `SUCCEEDED` with `COMPLETED`.
- Implementation commit: `2b06ac8 feat: add bounded agent loop`.

## Revisit

Revisit when cancellation, timeouts, real command execution, or a real model provider are added. Those features may require configurable budgets and changing tool observations to provider-native structures.
