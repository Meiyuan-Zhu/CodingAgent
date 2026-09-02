# CodingAgent Design Context

## Register

CodingAgent is a local developer tool, not a marketing surface. The UI should feel like a quiet workbench for inspecting an agent run: conversational in the center, project/run memory on the left, and concrete review artifacts on the right.

## North Star

Reference Codex desktop's coding task screen: sparse chrome, centered task narrative, lightweight command/action rows, compact change cards, and a right review pane for diff/file inspection.

## Anti-references

Avoid raw event logs, dashboard cards full of backend metadata, fake terminal panels, colorful SaaS gradients, and buttons for capabilities not backed by the backend. The product should not look like a generic admin template.

## Token ownership

Runtime CSS variables and component styles in `frontend/src/style.css` are currently the canonical token implementation. This document mirrors the accepted direction and explains intent; it does not generate tokens.

## Visual vocabulary

- Background: white and very light neutral gray surfaces with a barely visible technical grid under the main shell. The texture should feel precise and quiet, not decorative.
- Text: near-black body text with muted gray secondary text.
- Accent: green for healthy/successful state, amber for permission-needed state, red for failures, blue/violet for selected review affordances, active work, and Markdown emphasis. Violet should be a controlled signal layer, not a full-page purple theme.
- Shape: rounded but controlled. Composer and empty-state cards are softer; action rows are flatter.
- Elevation: the composer, active change card, and inspector diff block may use visible but soft shadow. Operational rows should stay quiet.
- Iconography: use the shared line-style UI icon component for tool rows, tabs, run/stop, and undo affordances instead of one-off text glyphs.
- Motion: restrained micro-interactions only: hover lift, live thinking pulse, and progressive assistant text. All motion must respect reduced-motion settings.
- Typography: prefer a Geist-like sans-serif stack. Use compact, confident weights; avoid oversized marketing headings inside the workbench.

## Layout

- Left sidebar: project identity and run list, compact and stable, with enough width for Chinese task titles to scan cleanly.
- Center: chat/task narrative with a readable Codex-like measure. User prompt appears as a right-aligned bubble; assistant progress appears as natural language plus small action rows.
- Right panel: two user-facing surfaces only: 审查 and 文件. 审查 owns the list of modified files and their diffs; 文件 owns discovered files and readable previews.
- The right panel may collapse and resize. There is no persistent bottom terminal until backend terminal semantics exist.

## Interaction principles

- Tool events are translated into user-understandable actions such as “正在运行 …”, “已读取 …”, “已编辑 …”. Raw JSON is secondary and only appears behind inspectable details.
- Permission requests explain the risk in task language and provide clear Approve/Reject actions.
- File changes produce a compact change summary card with 审查 and, when the backend marks the change undoable, 撤销 actions. Once undone, counts and review chips should no longer present that change as pending review.
- Streaming and running states should look alive without producing noisy metadata cards.
- The timeline should follow active work when the user is at the bottom, but preserve the user's scroll position while they are reading earlier output.
- Error states remain visible in the conversation and do not erase the run history.

## Accessibility and localization

Primary user-facing labels on the workbench use concise Chinese where the user compares against Codex Chinese UI: 审查、文件、正在思考、正在运行、已运行、已编辑. Buttons use native button semantics and visible focus states.

## Markdown rendering and final answer behavior

Assistant narrative content supports a small safe Markdown subset rendered as Vue nodes, not raw HTML. It includes headings, ordered and unordered lists, blockquotes, horizontal rules, simple tables, fenced code, inline code, emphasis, strong text, and safe links. Markdown styling may use restrained violet accents to improve scanability without turning the workspace into a colorful document editor. During an active run, progress messages can appear as lightweight narrative. After the run finishes, intermediate assistant narration is collapsed and only the final assistant answer remains in the main transcript, with tool/action evidence preserved as action rows and review cards.

## Composer behavior

Return sends the task unless the user is composing IME text or holding Shift for a newline. Command-Return and Control-Return also send for keyboard familiarity. After a task is accepted, the composer clears so the submitted request is not duplicated in the input area. If run creation fails, the draft is restored for recovery.

## Streaming behavior

Current streaming uses provider token-level deltas when the backend runs an OpenAI-compatible native-tools model. The backend emits `MODEL_MESSAGE_DELTA` events as text arrives, then persists the final full assistant message for replay. UI-level progressive reveal remains a fallback for non-streaming model paths.
