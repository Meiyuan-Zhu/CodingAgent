# ADR-0006: Workspace Boundary and Read-Only File Tools

Date: 2026-08-27

Decision owner: assistant implemented after the user approved subtask 2.

State: accepted

Implementation status: verified for workspace path boundary and read-only file tools.

Supersedes: none

## Problem

The agent must operate like a local coding assistant, but model-requested file paths are untrusted. Without a workspace boundary, a tool call could read files outside the intended project, follow a symlink to private locations, or include `.env` secrets in model context.

## Decision

Add a backend workspace layer under `com.zhumeiyuan.codingagent.agent.workspace`:

- Configure the development workspace with `agent.workspace.root=../workspaces/demo`.
- Require file tool paths to be relative to the configured workspace.
- Normalize paths before resolving them.
- Reject absolute paths, path traversal outside the workspace, sensitive `.env` paths, missing paths, oversized reads, invalid UTF-8, and symlinks that resolve outside the workspace.
- Provide read-only tools first: `listFiles`, `readFile`, and `searchText`.
- Keep this layer independent of model providers, HTTP controllers, and SSE.

Only `workspaces/demo/` is allowed into Git. Other root-level workspace directories remain ignored.

## Reasons

- A single resolver keeps safety checks consistent for future read, write, edit, and command tools.
- Realpath checking closes the common symlink escape case.
- Rejecting `.env` files reduces the chance of leaking API keys into model context or logs.
- Starting with read-only tools lets us validate boundaries before adding mutating file tools.
- Local demo workspace gives the app a harmless default target without publishing private project files.

## Alternatives Considered

- Trust frontend-provided paths: rejected because the frontend cannot be the authority for local file safety.
- Let each tool validate paths separately: rejected because rules would drift as tools grow.
- Use upload-only files: rejected by ADR-0004 because it does not match real coding-agent workflows.
- Implement write/edit tools in the same step: deferred to keep the first safety layer small and testable.

## Costs

- Current sensitive-file detection is intentionally conservative but narrow; more patterns may be needed later.
- Current search is simple substring search over UTF-8 files, not indexed search.
- Large-file handling rejects reads instead of streaming/truncating content into model context.
- Read-only tools are not yet exposed through HTTP or the model tool registry.

## Evidence

- Workspace code exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/`.
- Tests exist under `backend/src/test/java/com/zhumeiyuan/codingagent/agent/workspace/`.
- Demo files exist under `workspaces/demo/`.
- `mvn test` passed on 2026-08-27 with 21 tests, including workspace boundary and read-tool tests.

## Revisit

Revisit before implementing write/edit tools to decide exact conflict detection, allowed file sizes, binary-file behavior, and whether `.env.example` should remain readable.
