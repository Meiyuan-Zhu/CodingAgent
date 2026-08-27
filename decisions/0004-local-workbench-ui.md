# ADR-0004: Local Workspace Workbench UI

Date: 2026-08-27

Decision owner: user confirmed the interface should be Codex-like; assistant recorded the product boundary and implemented the first UI shell.

State: accepted

Implementation status: partially implemented. The Vue shell reflects the workbench shape, but workspace-backed agent execution is not implemented yet.

Supersedes: none

## Problem

The project needs a web interface for recording and presentation, but a coding agent should work against a local project directory instead of treating code as uploaded files. Upload-first design would make the product feel like a document processor and would not show the core loop of reading, editing, running commands, and iterating in one workspace.

## Decision

Build the product as a local workspace workbench:

- Browser UI submits tasks, displays run events, shows files/diffs/check results, and later handles approvals.
- Spring Boot backend owns workspace access, model calls, local tool execution, and run storage.
- The initial local workspace will be a backend-configured directory, not a browser file upload.
- The frontend visual direction should be simple, clear, and workbench-like: restrained colors, dense but readable panels, left run list, central task timeline, and right detail tabs.

## Reasons

- This matches the assessment goal: the agent must interact with the user's own program to read/write files and execute commands.
- Browser file upload is a poor fit for multi-step coding tasks because it loses directory context, Git state, command execution, and repeated edits.
- Keeping local file authority in the backend makes the security boundary easier to explain: the frontend is a control surface, not an executor.
- A Codex-like layout supports the demo video because the viewer can see task, tool/event progress, file context, and verification evidence at once.

## Alternatives Considered

- Upload files through the web page: simpler to implement, but weak for real coding workflows and less aligned with the assessment.
- Native desktop UI: closest to a local coding agent, but higher implementation cost and not needed for the required video.
- Marketing-style web page: visually quick, but does not help demonstrate actual agent behavior.

## Costs

- The backend must implement explicit workspace scoping and path safety before file tools become writable.
- The UI must avoid implying capabilities before the backend supports them.
- Running the app locally requires both frontend and backend development servers.

## Evidence

- `frontend/src/App.vue` now uses a workbench layout with run list, task thread, health panel, composer, and detail tabs.
- `frontend/src/style.css` now uses a restrained application layout rather than a landing page.
- `npm run build` passed on 2026-08-27 after the UI shell update.

## Revisit

Revisit if the final demonstration requires importing an external project snapshot. Even then, import should create or select a workspace first, not replace the workspace model with one-off uploads.
