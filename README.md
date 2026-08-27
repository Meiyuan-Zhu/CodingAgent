# Coding Agent

Personal coding agent project for the recommendation assessment. The repository is organized as a separated Vue 3 frontend and Spring Boot backend.

## Applications

- Frontend: `frontend/`, Vue 3 + TypeScript + Vite.
- Backend: `backend/`, Java 21 + Spring Boot + Maven.
- Demo workspace: `workspaces/demo/`.
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

The frontend development server proxies `/api` requests to `http://localhost:8080`.

The current workbench can create a mock run, stream backend events through SSE, and execute read-only workspace tools through the backend tool registry. This is a verified local loop, not a real model integration yet.

Useful backend endpoints:

- `POST /api/runs`
- `GET /api/runs/{runId}`
- `GET /api/runs/{runId}/events`
- `GET /api/runs/{runId}/events/stream`

## Checks

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```
