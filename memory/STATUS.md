# 当前开发状态

更新日期：2026-08-27（北京时间）。

**当前阶段：工程骨架、Agent 运行协议领域模型、本地 workspace 边界、读写文件工具、工具注册表、HTTP run API、SSE 事件流和 mock runner 前后端闭环已建立并验证；尚未接入真实模型、命令工具、取消或审批策略。**

## 已确认

- Vue 3 前端，Java + Spring Boot 后端，前后端分离。见 [ADR-0001](../decisions/0001-frontend-backend-stack.md)。
- 当前框架基线：Vue 3 + TypeScript + Vite，Java 21 + Maven + Spring Boot 3.5.16。见 [ADR-0003](../decisions/0003-framework-baseline.md)。
- 产品形态：Web 界面作为本地 workspace 工作台，不采用上传文件作为主要工作流；界面方向简洁、清晰、Codex-like。见 [ADR-0004](../decisions/0004-local-workbench-ui.md)。
- Agent 运行协议领域模型已建立，核心仍未接入模型、工具执行或 API。见 [ADR-0005](../decisions/0005-run-protocol-domain.md)。
- Workspace 边界和只读文件工具已建立：相对路径、realpath、敏感路径、UTF-8 和大小限制均有测试。见 [ADR-0006](../decisions/0006-workspace-boundary-read-tools.md)。
- 工具注册表已建立：`list_files`、`read_file`、`search_text` 具备工具定义、参数校验、统一执行入口和失败归一化。见 [ADR-0007](../decisions/0007-tool-registry.md)。
- HTTP run API、SSE 事件流和 mock runner 已建立：前端 Run 按钮可创建任务、订阅事件并展示工具执行结果。见 [ADR-0008](../decisions/0008-run-api-sse-mock-runner.md)。
- Workspace 写入与文本编辑工具已建立：`write_file`、`replace_text` 具备路径边界、UTF-8/大小限制、hash 冲突检测和注册表接入。见 [ADR-0009](../decisions/0009-workspace-write-edit-tools.md)。
- Agent 关键逻辑自行实现，项目不使用 Agent 框架/SDK 或 Spring AI。
- decisions/ 记录决策，memory/ 记录开发状态与文档。见 [ADR-0002](../decisions/0002-development-records.md)。
- 正式截止：北京时间 2026-09-03 00:00；此后不推送新提交。

## 已完成的准备工作

- 读取题目 PDF 的全部 2 页，整理要求与提交限制。
- 建立并迁移总体方案、技术选型决策。
- 建立开发约定、状态、阶段日志和验证记录。
- 已执行文档核验：见 [DOC-001](VERIFICATION.md)。这不构成应用功能测试。
- 初始化本地 Git 仓库，分支为 `main`。
- 建立 `frontend/` Vue 3 项目和 `backend/` Spring Boot 项目。
- 建立后端 `GET /api/health` 与前端健康状态展示，用于验证前后端开发连接基础。
- 已执行应用骨架验证：见 [APP-001](VERIFICATION.md)。
- 建立 `workspaces/demo/` 示例 workspace。
- 实现后端 `WorkspacePathResolver` 与只读工具 `listFiles`、`readFile`、`searchText`。
- 实现后端 `ToolRegistry`，将只读 workspace 工具注册为模型后续可调用的工具入口。
- 实现 `POST /api/runs`、run 状态查询、事件回看和 SSE 订阅；Vue 工作台已接入 mock run 流程。
- 实现 `WorkspaceWriteTools`，将 `write_file` 和 `replace_text` 接入工具注册表。

## 功能状态

状态含义：未开始 / 进行中 / 已实现未验证 / 已验证 / 阻塞。只有存在实现和对应有效证据才能写已验证。

| 模块 | 当前状态 | 实现位置 | 验证证据 | 决策/说明 |
| --- | --- | --- | --- | --- |
| 前后端工程骨架及独立启动 | 已验证 | [frontend](../frontend)、[backend](../backend) | [APP-001](VERIFICATION.md) | ADR-0001、ADR-0003；验证覆盖构建和后端健康接口 |
| 任务接口与 Web 页面 | 已验证 | [frontend/src/App.vue](../frontend/src/App.vue)、[frontend/src/api/runs.ts](../frontend/src/api/runs.ts)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/api](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/api) | [RUNAPI-001](VERIFICATION.md)、[UI-002](VERIFICATION.md) | 可创建 mock run 并显示事件；仍非真实模型任务界面 |
| 模型适配器及 Agent 循环 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/run](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/run)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution) | [CORE-001](VERIFICATION.md)、[RUNAPI-001](VERIFICATION.md) | 已有 mock runner 垂直切片；尚未实现真实模型适配器、响应解析、预算或多轮循环 |
| 文件工具、搜索与编辑 | 已验证 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace)、[workspaces/demo](../workspaces/demo) | [WORKSPACE-001](VERIFICATION.md)、[WRITE-001](VERIFICATION.md) | list/read/search/write/replace 已验证；尚未实现 diff 渲染、审批策略或命令工具 |
| 工具注册表与执行入口 | 已验证 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool) | [TOOLREG-001](VERIFICATION.md)、[WRITE-001](VERIFICATION.md) | `list_files`、`read_file`、`search_text`、`write_file`、`replace_text` 已注册；尚未接入真实模型适配器 |
| 命令执行、审批、取消 | 未开始 | 尚无 | 尚无 | 需要真实进程与权限测试 |
| 对话上下文及运行预算 | 未开始 | 尚无 | 尚无 | 裁剪与终止规则待定 |
| SSE、工具卡片、Diff、输出 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunEventStream.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunEventStream.java)、[frontend/src/App.vue](../frontend/src/App.vue) | [RUNAPI-001](VERIFICATION.md)、[UI-002](VERIFICATION.md) | SSE 基础事件流已验证；工具卡片、Diff 和 rich output 未实现 |
| 运行记录与历史回看 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunStore.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunStore.java) | [RUNAPI-001](VERIFICATION.md) | 进程内事件回看已验证；重启后持久化未实现 |
| 真实模型任务及回归测试 | 未开始 | 尚无 | 尚无 | 与模拟模型测试分开 |
| 提交说明、视频与面试材料 | 未开始 | 尚无 | 尚无 | 依据真实能力编写 |

## 下一步

1. 建立真实模型适配边界和响应解析器，先用 fixture/mock 响应测试，再接真实 API。
2. 增加运行预算、轮次上限、取消入口和失败终止规则。
3. 设计写入/命令工具的审批策略，并让前端展示 diff 或变更摘要。
4. 确认可用模型 API、工具调用支持和测试预算；密钥由用户在本地环境中配置，不写进聊天或文档。
5. 确认公开仓库的账户、名称及可公开文件；首次公开推送前复核题目 PDF、日志、密钥和演示材料。

## 风险与待定项

- 尚未做真实模型连通性验证，不能声称可调用真实模型。
- 公开远程仓库尚未建立；当前只有本地 Git 历史。
- 演示案例尚未最终确定，排期属于建议。
- 当前有已验证的 mock run 前后端闭环和写入/编辑工具，但没有已验证的命令执行、审批策略或真实模型能力。
