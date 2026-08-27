# Coding Agent

Personal coding agent project for the recommendation assessment. The repository is organized as a separated Vue 3 frontend and Spring Boot backend.

## Applications

- Frontend: `frontend/`, Vue 3 + TypeScript + Vite.
- Backend: `backend/`, Java 21 + Spring Boot + Maven.
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

## Checks

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```
