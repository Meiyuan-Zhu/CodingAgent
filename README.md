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

To try DeepSeek V4 Flash, configure `DEEPSEEK_API_KEY` in your shell environment, then start the backend with:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--agent.model.provider=openai-compatible --agent.model.name=deepseek-v4-flash"
```

The key must stay in your shell or local untracked environment. Do not commit it.

The frontend development server proxies `/api` requests to `http://localhost:8080`.

The current workbench can create a run, stream backend events through SSE, execute workspace tools through the backend tool registry, request approval for mutating tools and local commands, show resulting diffs, and render command stdout/stderr/exit code in tool cards. The default model provider is still `mock` for safe local development. A DeepSeek/OpenAI-compatible adapter is implemented but real DeepSeek runs require an API key and explicit approval to send task context to the external model service.

Currently registered model-facing tools:

- `list_files`
- `read_file`
- `search_text`
- `write_file`
- `replace_text`
- `run_command`

Useful backend endpoints:

- `POST /api/runs`
- `GET /api/runs/{runId}`
- `GET /api/runs/{runId}/events`
- `GET /api/runs/{runId}/events/stream`
- `POST /api/runs/{runId}/cancel`
- `POST /api/runs/{runId}/approvals/{toolCallId}/approve`
- `POST /api/runs/{runId}/approvals/{toolCallId}/reject`

## Checks

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```
