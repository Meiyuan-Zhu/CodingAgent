# ADR-0003: Vue 3 and Spring Boot Framework Baseline

Date: 2026-08-27

Decision owner: user confirms Vue 3 and Spring Boot direction; assistant selected concrete versions and scaffold layout during implementation.

State: accepted

Implementation status: verified for framework build and health endpoint only.

Supersedes: none

## Problem

The project needs an actual runnable web interface and Java backend before implementing the coding agent loop. The baseline should be familiar enough for the user to explain in interview, while leaving room for the self-written agent core required by the assessment.

## Decision

Use a two-application repository:

- `frontend/`: Vue 3, TypeScript, Vite.
- `backend/`: Java 21, Maven, Spring Boot 3.5.16.
- Development mode connects them with a Vite proxy from `/api` to `http://localhost:8080`.
- The first backend endpoint is `GET /api/health`, used only to verify framework wiring.

## Reasons

- Vue 3 matches the user's prior frontend experience and supports a maintainable component structure.
- TypeScript makes frontend API contracts explicit before the agent protocol grows.
- Java 21 is installed locally and is a current LTS runtime.
- Spring Boot 3.5.16 is available in Maven Central and avoids unnecessary Spring Boot 4 migration noise during the assessment window.
- Spring Web and Validation are enough for HTTP APIs and runtime input checks; Spring AI is not used because the agent loop and tool handling must be self-written.
- A Vite dev proxy keeps the frontend free of model credentials and local execution authority.

## Alternatives Considered

- Spring Boot 4.1.1: Spring Initializr metadata listed it as the default stable generation option, but the generated Maven version used `4.1.1.RELEASE`, which did not resolve through the local Maven mirror. Boot 4 also adds major-version migration surface that does not help the assessment core.
- JavaScript frontend: simpler at first, but weaker for maintaining API contracts between Vue and Spring as the event protocol grows.
- Single Spring Boot server rendering pages: fewer processes, but worse fit for the user's Vue experience and for a demo UI with rich run/event state.

## Costs

- Two applications require two dev servers during local development.
- API DTOs must be kept consistent across Java and TypeScript.
- Frontend build validation does not prove backend connectivity unless both dev servers are running.

## Evidence

- `backend/pom.xml` pins Spring Boot 3.5.16 and Java 21.
- `frontend/package.json` pins the generated Vue/Vite dependency set through `package-lock.json`.
- `mvn test` passed on 2026-08-27 with 2 tests, including `/api/health`.
- `npm run build` passed on 2026-08-27.
- With both dev servers running, `curl http://127.0.0.1:5173/api/health` returned the backend health JSON through the Vite proxy.

## Revisit

Revisit if Spring Boot 4 becomes necessary for a specific feature, or if the final model API/event streaming design requires a different backend HTTP stack.
