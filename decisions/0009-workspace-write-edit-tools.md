# ADR-0009: Workspace Write and Text Edit Tools

Date: 2026-08-27

Decision owner: assistant implemented after the user approved continuing from mock run flow to write/edit tools.

State: accepted

Implementation status: verified for workspace write boundary, full-file writes, exact text replacement, hash conflict detection, registry wiring, and Spring bean loading.

Supersedes: none

## Problem

The agent needs to modify local project files, not only read them. Mutating tools are riskier than read-only tools because a model may operate on stale context, target the wrong path, overwrite human changes, or write secret files. The write boundary must therefore be stricter and easier to explain than ordinary Java file writes.

## Decision

Extend the workspace layer with:

- `WorkspacePathResolver.resolveForWrite`, which allows missing target files only when the parent directory already exists inside the configured workspace.
- `WorkspaceWriteTools.writeFile`, which creates or overwrites full UTF-8 text files.
- `WorkspaceWriteTools.replaceText`, which performs exact text replacement in existing UTF-8 files.
- Result records with change summaries and SHA-256 hashes.

Register two model-facing tools:

- `write_file`
- `replace_text`

Both tools reject absolute paths, path traversal, sensitive `.env` paths, symlink escapes, invalid UTF-8, and oversized content. Existing-file writes require `overwrite=true`. Both tools accept optional `expected_sha256` so future agent loops can avoid writing over files that changed after being read.

The mock runner is not changed to write by default, because repeated demo runs should not dirty the Git working tree.

## Reasons

- The same resolver should own read and write boundaries so safety rules do not drift.
- Whole-file write is simple and useful for creating new files.
- Exact text replacement is safer for small edits than asking the model to resend an entire file.
- SHA-256 conflict detection provides a clear interview story: read, remember hash, compare before write, then reject stale edits.
- Avoiding default mock writes keeps the current UI demo repeatable and clean.

## Alternatives Considered

- Let write tools create parent directories automatically: deferred because silent directory creation increases the blast radius of a bad tool call.
- Accept line-number patches first: useful later, but exact text replacement is simpler to validate and easier to test now.
- Expose write tools directly as HTTP endpoints: rejected for now because mutating workspace tools should be invoked through the agent/tool policy path, not arbitrary frontend calls.
- Make mock runner perform writes for demo impact: rejected for now because it would dirty tracked demo files during routine UI testing.

## Costs

- Exact replacement can fail when the target text changes slightly; the model will need to read again and retry.
- `write_file` can still overwrite a whole file when explicitly allowed; future approval/budget policy should decide when that is acceptable.
- Current change summary is metadata and hashes, not a rendered unified diff.
- Parent directories must already exist.

## Evidence

- Write/edit workspace code exists under `backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/`.
- Tool adapters exist in `backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/WorkspaceToolFactory.java`.
- Tests exist under `backend/src/test/java/com/zhumeiyuan/codingagent/agent/workspace/` and `backend/src/test/java/com/zhumeiyuan/codingagent/agent/tool/`.
- `mvn test` passed on 2026-08-27 with 64 tests.

## Revisit

Revisit when implementing the real agent loop and approval policy. At that point decide which write operations require explicit user approval, how to render diffs in the UI, and whether to add structured patch tools.
