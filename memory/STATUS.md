# 当前开发状态

更新日期：2026-08-27（北京时间）。

**当前阶段：工程骨架、Agent 运行协议领域模型、本地 workspace 边界、读写文件工具、工具注册表、HTTP run API、SSE 事件流、mock runner、模型适配边界、响应解析器、多轮 Agent loop、运行预算、取消 API、工具超时、后台任务生命周期、审批策略和 diff/变更展示已建立并验证；尚未接入真实模型 API、命令工具或完整 approve/resume 工作流。**

## 已确认

- Vue 3 前端，Java + Spring Boot 后端，前后端分离。见 [ADR-0001](../decisions/0001-frontend-backend-stack.md)。
- 当前框架基线：Vue 3 + TypeScript + Vite，Java 21 + Maven + Spring Boot 3.5.16。见 [ADR-0003](../decisions/0003-framework-baseline.md)。
- 产品形态：Web 界面作为本地 workspace 工作台，不采用上传文件作为主要工作流；界面方向简洁、清晰、Codex-like。见 [ADR-0004](../decisions/0004-local-workbench-ui.md)。
- Agent 运行协议领域模型已建立，核心仍未接入模型、工具执行或 API。见 [ADR-0005](../decisions/0005-run-protocol-domain.md)。
- Workspace 边界和只读文件工具已建立：相对路径、realpath、敏感路径、UTF-8 和大小限制均有测试。见 [ADR-0006](../decisions/0006-workspace-boundary-read-tools.md)。
- 工具注册表已建立：`list_files`、`read_file`、`search_text` 具备工具定义、参数校验、统一执行入口和失败归一化。见 [ADR-0007](../decisions/0007-tool-registry.md)。
- HTTP run API、SSE 事件流和 mock runner 已建立：前端 Run 按钮可创建任务、订阅事件并展示工具执行结果。见 [ADR-0008](../decisions/0008-run-api-sse-mock-runner.md)。
- Workspace 写入与文本编辑工具已建立：`write_file`、`replace_text` 具备路径边界、UTF-8/大小限制、hash 冲突检测和注册表接入。见 [ADR-0009](../decisions/0009-workspace-write-edit-tools.md)。
- 模型适配边界与响应解析器已建立：runner 通过 `ModelClient` 获取响应，`ModelResponseParser` 校验 JSON 输出并将解析失败归为 `MODEL_PARSE_ERROR`。见 [ADR-0010](../decisions/0010-model-boundary-response-parser.md)。
- 多轮 Agent loop 与运行预算已建立：模型工具调用结果会回填到下一轮请求，轮次、工具调用数和上下文消息窗口有明确限制。见 [ADR-0011](../decisions/0011-agent-loop-budget.md)。
- Run 取消、工具超时和后台任务生命周期已建立：后端保留 run task 句柄，支持 cancel endpoint，runner 检查取消状态，工具执行有 timeout。见 [ADR-0012](../decisions/0012-run-cancellation-timeout-lifecycle.md)。
- 审批策略和 diff/变更展示已建立：可变更工具在后端需要审批；当前阶段未实现 approve/resume API，因此安全拒绝且不执行；写入/替换工具返回 unified diff，前端 Diff 面板可展示。见 [ADR-0013](../decisions/0013-approval-policy-diff-display.md)。
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
- 实现 `ModelClient`、模型请求/响应类型、`ModelResponseParser` 和 `HeuristicMockModelClient`；mock runner 已通过模型边界调用工具。
- 实现 `RunBudget`，mock runner 已支持多轮模型/工具循环、工具观察回填、轮次上限、工具调用上限和上下文消息窗口。
- 实现 `RunTaskManager`、`POST /api/runs/{runId}/cancel`、工具执行超时和前端 Cancel 按钮。
- 实现 `ToolApprovalPolicy`、审批事件拦截、写入/替换 unified diff 元数据和前端 Diff 面板。

## 功能状态

状态含义：未开始 / 进行中 / 已实现未验证 / 已验证 / 阻塞。只有存在实现和对应有效证据才能写已验证。

| 模块 | 当前状态 | 实现位置 | 验证证据 | 决策/说明 |
| --- | --- | --- | --- | --- |
| 前后端工程骨架及独立启动 | 已验证 | [frontend](../frontend)、[backend](../backend) | [APP-001](VERIFICATION.md) | ADR-0001、ADR-0003；验证覆盖构建和后端健康接口 |
| 任务接口与 Web 页面 | 已验证 | [frontend/src/App.vue](../frontend/src/App.vue)、[frontend/src/api/runs.ts](../frontend/src/api/runs.ts)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/api](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/api) | [RUNAPI-001](VERIFICATION.md)、[UI-002](VERIFICATION.md)、[LIFE-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md) | 可创建/cancel mock run、显示事件和审批拦截；仍非真实模型任务界面 |
| 模型适配器及 Agent 循环 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/model](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/model)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution) | [CORE-001](VERIFICATION.md)、[RUNAPI-001](VERIFICATION.md)、[MODEL-001](VERIFICATION.md)、[LOOP-001](VERIFICATION.md)、[LIFE-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md) | 模型边界、多轮工具循环、预算、取消、工具超时和可变更工具审批拦截已验证；尚未实现真实模型 API |
| 文件工具、搜索与编辑 | 已验证 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace)、[workspaces/demo](../workspaces/demo) | [WORKSPACE-001](VERIFICATION.md)、[WRITE-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md) | list/read/search/write/replace 已验证；write/replace 会返回 unified diff；命令工具未实现 |
| 工具注册表与执行入口 | 已验证 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool) | [TOOLREG-001](VERIFICATION.md)、[WRITE-001](VERIFICATION.md)、[MODEL-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md) | `list_files`、`read_file`、`search_text`、`write_file`、`replace_text` 已注册；可变更工具经过审批策略；真实模型适配器未接入 |
| 命令执行、审批、取消 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolApprovalPolicy.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolApprovalPolicy.java)、[frontend/src/App.vue](../frontend/src/App.vue) | [LIFE-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md) | Run 取消、工具 timeout 和可变更工具审批拦截已验证；完整 approve/resume、命令执行和进程级取消未实现 |
| 对话上下文及运行预算 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunBudget.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunBudget.java)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java) | [LOOP-001](VERIFICATION.md) | 轮次、工具调用和消息窗口已验证；真实 token 计数和 provider-specific 裁剪未实现 |
| SSE、工具卡片、Diff、输出 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunEventStream.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunEventStream.java)、[frontend/src/App.vue](../frontend/src/App.vue) | [RUNAPI-001](VERIFICATION.md)、[UI-002](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md) | SSE 基础事件流、审批事件订阅和 Diff 面板构建已验证；工具卡片和 rich output 仍待增强 |
| 运行记录与历史回看 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunStore.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunStore.java) | [RUNAPI-001](VERIFICATION.md) | 进程内事件回看已验证；重启后持久化未实现 |
| 真实模型任务及回归测试 | 未开始 | 尚无 | 尚无 | 与模拟模型测试分开 |
| 提交说明、视频与面试材料 | 未开始 | 尚无 | 尚无 | 依据真实能力编写 |

## 下一步

1. 实现完整 approve/reject/resume 工作流：前端审批按钮、后端审批接口、run 从 `WAITING_FOR_APPROVAL` 恢复执行。
2. 接入第一个真实模型 API adapter，但保持密钥只通过本地环境变量提供，不写进聊天、源码或文档。
3. 实现命令执行工具：在 workspace 边界内执行审批后的命令，捕获 stdout/stderr/exit code，并补充进程级取消。
4. 增强前端工具卡片和 rich output 展示，让录屏能清楚呈现工具调用、审批和文件变更。
5. 确认公开仓库的账户、名称及可公开文件；首次公开推送前复核题目 PDF、日志、密钥和演示材料。

## 风险与待定项

- 尚未做真实模型连通性验证，不能声称可调用真实模型。
- 公开远程仓库尚未建立；当前只有本地 Git 历史。
- 演示案例尚未最终确定，排期属于建议。
- 当前有已验证的 mock run 前后端闭环、写入/编辑工具、多轮 loop、取消、工具超时、审批拦截策略和 diff 元数据/展示构建，但没有完整 approve/resume、命令执行、进程级取消或真实模型能力。
