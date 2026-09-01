# ADR-0025：Agent 文件变更撤销

- 日期：2026-08-29
- 状态：accepted
- 决策依据/确认来源：用户明确提出需要类似 Codex 的 Agent 修改撤销能力；既有 ADR-0009、ADR-0013、ADR-0014 已确立 workspace 写入、审批和 diff 展示边界。
- 实现状态：已实现并验证
- 取代/被取代：无

## 问题与约束

Agent 经用户批准后可以写入或替换 workspace 文件。为了贴近 Codex-like 审查体验，用户需要在变更完成后撤销该次 Agent 修改。

撤销本身也是文件变更，不能由模型自行触发，不能绕过 workspace 路径边界，也不能在文件已被用户或后续工具改动后盲目覆盖新内容。撤销所需的旧文件内容不应进入模型观察、SSE 事件 payload 或前端事件日志。

## 备选方案

1. 依赖 Git checkout 撤销。实现简单，但要求 workspace 必须是 Git 仓库，并可能影响用户未暂存改动，和当前 demo workspace 边界不匹配。
2. 把旧内容放进工具结果 JSON。实现简单，但旧内容会回传给模型和前端事件回看，扩大日志暴露面。
3. 在后端记录每次成功文件工具变更的私有撤销快照，由用户 API 触发撤销。需要新增内存 journal 和 API，但能保持边界清楚。

## 决定与理由

采用方案 3。

`write_file` 和 `replace_text` 成功后，工具结果通过后端私有 metadata 携带撤销快照，`WorkspaceChangeJournal` 按 `runId + toolCallId` 记录旧内容、目标路径、是否为新建文件和 Agent 修改后的 hash。模型和前端只看到公开工具结果、diff、`undoable` 标记和撤销完成事件。

撤销由用户调用 `POST /api/runs/{runId}/changes/{toolCallId}/undo` 触发，不作为模型工具暴露。执行撤销前校验当前文件 hash 必须等于 Agent 修改后的 hash；新建文件撤销为删除文件，替换/覆盖撤销为恢复旧 UTF-8 内容。撤销成功后追加 `CHANGE_UNDONE` 事件，前端据此更新变更卡和审查面板。

## 代价与限制

- 撤销记录目前保存在后端进程内存，后端重启后不能撤销旧 run 的变更。
- 当前撤销粒度是单个 `write_file` 或 `replace_text` 工具调用，不是跨多个工具调用的批量回滚。
- 如果用户或后续工具已改动同一文件，撤销会因 hash 冲突失败，需要人工审查。

## 实现与验证证据

- 代码位置：[WorkspaceChangeJournal](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/WorkspaceChangeJournal.java)、[WorkspaceWriteTools](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceWriteTools.java)、[RunController](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/api/RunController.java)、[App.vue](../frontend/src/App.vue)、[ChangeSummaryCard.vue](../frontend/src/components/ChangeSummaryCard.vue)、[InspectorPane.vue](../frontend/src/components/InspectorPane.vue)。
- 验证记录：[UNDO-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚未提交。

## 何时重新考虑

如果需要重启后仍可撤销、批量撤销一个 run 的所有变更、或支持多 workspace/多用户并发，需要把撤销快照持久化并设计更完整的变更事务与权限模型。
