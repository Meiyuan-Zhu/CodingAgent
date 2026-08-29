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

- Background: white and very light blue-gray surfaces.
- Text: near-black body text with muted gray secondary text.
- Accent: green for healthy/successful state, amber for permission-needed state, red for failures, blue only for selected review affordances.
- Shape: rounded but controlled. Composer and empty-state cards are softer; action rows are flatter.
- Elevation: only the composer and empty-state intro use visible shadow. Operational rows should stay quiet.

## Layout

- Left sidebar: project identity and run list, compact and stable.
- Center: chat/task narrative. User prompt appears as a right-aligned bubble; assistant progress appears as natural language plus small action rows.
- Right panel: two user-facing surfaces only: 审查 and 文件. 审查 owns diffs and command/tool evidence; 文件 owns discovered files and readable previews.
- The right panel may collapse and resize. There is no persistent bottom terminal until backend terminal semantics exist.

## Interaction principles

- Tool events are translated into user-understandable actions such as “正在运行 …”, “已读取 …”, “已编辑 …”. Raw JSON is secondary and only appears behind inspectable details.
- Permission requests explain the risk in task language and provide clear Approve/Reject actions.
- File changes produce a compact change summary card with an 审查 action. Undo is not shown until the backend implements undo.
- Streaming and running states should look alive without producing noisy metadata cards.
- Error states remain visible in the conversation and do not erase the run history.

## Accessibility and localization

Primary user-facing labels on the workbench use concise Chinese where the user compares against Codex Chinese UI: 审查、文件、正在思考、正在运行、已运行、已编辑. Buttons use native button semantics and visible focus states.

## Markdown rendering and final answer behavior

Assistant narrative content supports a small safe Markdown subset rendered as Vue nodes, not raw HTML. During an active run, progress messages can appear as lightweight narrative. After the run finishes, intermediate assistant narration is collapsed and only the final assistant answer remains in the main transcript, with tool/action evidence preserved as action rows and review cards.

## Composer behavior

Return sends the task. Command-Return and Control-Return also send for keyboard familiarity. After a task is accepted, the composer clears so the submitted request is not duplicated in the input area. If run creation fails, the draft is restored for recovery.

## Streaming behavior

Current streaming is UI-level progressive reveal driven by existing run SSE events. Backend model calls still return complete assistant messages per round; provider token-level streaming is a later backend protocol change. The UI should still feel live by showing thinking state, action rows, and animated assistant text when a model message arrives during an active run.
