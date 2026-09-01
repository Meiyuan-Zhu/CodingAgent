# Coding Agent

Personal coding agent project for the recommendation assessment. The repository is organized as a separated Vue 3 frontend and Spring Boot backend.

## Applications

- Frontend: `frontend/`, Vue 3 + TypeScript + Vite.
- Backend: `backend/`, Java 21 + Spring Boot + Maven.
- Demo workspace: `workspaces/demo/`, a tiny Python pricing project with an intentional failing test for agent demos.
- Development records: `memory/`.
- Architecture decisions: `decisions/`.

## Local Run

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

Start the frontend in another terminal:

```bash
cd frontend
npm run dev
```

Configure `DEEPSEEK_API_KEY` in your shell environment, then start the backend:

```bash
cd backend
mvn spring-boot:run
```

The default provider is DeepSeek V4 Flash through the OpenAI-compatible native tools protocol. The key must stay in your shell or local untracked environment. Do not commit it. Tests override the provider to `mock` so local verification stays offline and deterministic.

The frontend development server proxies `/api` requests to `http://localhost:8080`.

The current workbench can create a run, stream backend events through SSE, execute workspace tools through the backend tool registry, request approval for mutating tools and local commands, show resulting diffs, undo approved file changes, and render command stdout/stderr/exit code in tool cards. With the OpenAI-compatible native tools protocol, assistant text is streamed from the provider as token-level `MODEL_MESSAGE_DELTA` events while the final full message is still persisted for replay. Real DeepSeek runs require an API key and explicit approval to send task context to the external model service.

Command execution uses argv arrays rather than shell strings, runs from a workspace-bounded cwd, captures bounded stdout/stderr, and cleans up the spawned process tree on interruption or tool timeout when the host OS permits descendant enumeration.

## Agent Runtime

The backend implements the core agent loop itself rather than using an agent framework:

```text
User task -> LLM -> tool call -> local execution -> observation -> LLM -> ... -> final answer
```

Runtime termination is split into three cases:

- normal completion: the model returns a final answer without more tool calls
- system termination: round limit, tool call limit, token/length limit, or user cancellation stops the run
- unrecoverable failure: provider/API errors, invalid provider responses, or internal runtime exceptions fail the run

Tool execution failures are treated as recoverable observations. A failed tool result is appended back to the model context with `success=false`, allowing the model to adjust paths, arguments, edits, or commands on the next round. Budgets still bound repeated failed attempts.

Tool observations are JSON-first. Successful tools include `success=true` and a short `message`; failed tools include `success=false`, `failureKind=RECOVERABLE_TOOL_ERROR`, `recoverable=true`, `message`, `toolName`, `errorCode`, `recoveryHint`, and `timedOut`. Command results also include `exitCode`, `stdout`, `stderr`, truncation flags, and duration.

Recoverable tool errors include missing files, invalid workspace paths, sensitive or denied paths, edit misses, content conflicts, invalid arguments, unknown tools, command failures, and tool timeouts. The runner feeds these observations back into the next LLM request instead of ending the run immediately.

Context trimming is pair-aware. When the message history exceeds the configured window, the runner keeps the system prompt and original user task, then adds recent messages without splitting an assistant tool call from its corresponding tool result.

The system prompt is intentionally short and policy-focused. It tells the model to operate through tools, inspect before editing, keep changes focused, verify after code changes when possible, treat tool failures as feedback, avoid repeated failed actions, and summarize changes plus verification when done.

## Local Persistence

The backend uses H2 file mode with Spring JDBC for local persistence. Runtime data is stored under `backend/data/` and is intentionally ignored by Git.

Persisted state includes:

- run status and timestamps
- run event history for replay and review
- pending approval continuation state
- workspace change undo snapshots and undo state

This persistence is local development storage, not a multi-user production database. Runs that were actively executing when the backend process stopped are not automatically resumed after restart.

Currently registered model-facing tools:

- `list_files`
- `read_file`
- `search_text`
- `write_file`
- `edit_file`
- `replace_text`
- `run_command`

Useful backend endpoints:

- `POST /api/runs`
- `GET /api/runs`
- `GET /api/runs/{runId}`
- `GET /api/runs/{runId}/events`
- `GET /api/runs/{runId}/events/stream`
- `POST /api/runs/{runId}/cancel`
- `POST /api/runs/{runId}/approvals/{toolCallId}/approve`
- `POST /api/runs/{runId}/approvals/{toolCallId}/reject`
- `POST /api/runs/{runId}/changes/{toolCallId}/undo`

## Checks

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```
