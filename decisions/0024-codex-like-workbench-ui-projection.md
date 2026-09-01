# ADR-0024: Codex-like 工作台 UI 与事件投影层

## 状态

accepted

## 背景

早期前端主要用于验证后端事件流，页面直接展示大量 run event payload 和工具 JSON。该形态适合开发调试，但不适合作为 coding agent 的用户界面，也不利于录屏说明“模型提出动作、用户审批、工具执行、结果回填”的人机协作过程。

用户明确希望界面参考 Codex：左侧显示 project / history，中间是对话区，右侧显示文件、diff 等上下文，底部显示 terminal。审批应表现为权限审批，而不是普通事件日志。

## 决策

前端采用 Codex-like 工作台信息架构：

- 左侧 `ProjectSidebar` 展示当前 project 和本会话 run history。
- 中间 `ChatTimeline` 展示用户消息、assistant 消息、工具卡片和权限审批卡片。
- 右侧 `InspectorPane` 根据用户选择展示文件、工具详情、diff、命令和 checks。
- 底部 `BottomTerminal` 展示最新命令的 command、cwd、stdout、stderr、exit code 和 duration。
- 输入区 `ComposerBox` 固定在对话区底部，强调本地 workspace 和审批边界。
- 新增 `run/timeline.ts`，把后端审计事件转换成 UI-friendly timeline、inspector、terminal 和 approval view；Vue 组件不直接解释原始后端 payload。

后端事件协议在本阶段保持不变。前端的“流式”先基于已有 SSE 事件渐进渲染消息和工具状态，不在 ADR-0024 阶段引入 provider token streaming；该能力后来由 [ADR-0033](0033-provider-token-streaming.md) 补充。

## 理由

后端事件需要可追踪、可复核；前端界面需要可理解、可操作。把审计事件和 UI 投影分开，可以同时满足开发记录和人机交互：

- 面试时能解释完整事件链路。
- 录屏时用户能清楚看到审批对象和执行结果。
- 后续接 token streaming、文件预览、历史持久化时不需要重写后端协议。

## 替代方案

1. 继续直接渲染原始事件 JSON。实现最少，但人机可用性差，录屏呈现不清楚。
2. 后端直接返回 UI 专用 DTO。前端简单，但会把展示语义推入后端，削弱事件审计边界。
3. 一次性实现完整 IDE 级文件树和 terminal。体验最好，但超出当前阶段，风险较高。

## 影响

- `App.vue` 从大组件改为编排层。
- 新增多个 Vue 组件，按交互职责拆分。
- 新增前端事件投影层 `run/timeline.ts`。
- 保留现有 run API、SSE、审批 API 和工具事件结构。
- 当前 project/run history 仍是轻量前端会话态；后续如需跨重启历史，需要单独实现持久化。

## 验证

- `npm run build`：2026-08-28 21:02 CST 通过。
- 浏览器打开 `http://localhost:5174/` 进行目视检查：左侧 project/runs、中间对话区、右侧 Inspector、底部 Terminal 均可见。
- 浏览器触发一次 run 至命令审批点：中间显示 assistant 消息、工具卡片和 `run_command` 权限审批卡；右侧显示已发现文件；底部 Terminal 显示待审批命令。未批准该命令。
