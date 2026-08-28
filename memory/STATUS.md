# 当前开发状态

更新日期：2026-08-28（北京时间）。

**当前阶段：工程骨架、Agent 运行协议领域模型、本地 workspace 边界、读写文件工具、工具注册表、HTTP run API、SSE 事件流、mock runner、模型适配边界、响应解析器、多轮 Agent loop、运行预算、取消 API、工具超时、后台任务生命周期、审批策略、完整 approve/reject/resume 工作流、diff/变更展示、前端工具卡片/命令输出展示、OpenAI-compatible DeepSeek V4 Flash 适配器、原生 tool calling 协议和 workspace 命令执行工具已建立；DeepSeek V4 Flash 原生 tool calling 真实只读端到端 run 已验证；真实模型命令 run 已通过 deepseek-chat 强指令 demo 验证；V4 Flash 自主/半自主 demo 未通过。**

## 已确认

- Vue 3 前端，Java + Spring Boot 后端，前后端分离。见 [ADR-0001](../decisions/0001-frontend-backend-stack.md)。
- 当前框架基线：Vue 3 + TypeScript + Vite，Java 21 + Maven + Spring Boot 3.5.16。见 [ADR-0003](../decisions/0003-framework-baseline.md)。
- 产品形态：Web 界面作为本地 workspace 工作台，不采用上传文件作为主要工作流；界面方向简洁、清晰、Codex-like。见 [ADR-0004](../decisions/0004-local-workbench-ui.md)。
- Agent 运行协议领域模型已建立。见 [ADR-0005](../decisions/0005-run-protocol-domain.md)。
- Workspace 边界和只读文件工具已建立：相对路径、realpath、敏感路径、UTF-8 和大小限制均有测试。见 [ADR-0006](../decisions/0006-workspace-boundary-read-tools.md)。
- 工具注册表已建立：`list_files`、`read_file`、`search_text` 具备工具定义、参数校验、统一执行入口和失败归一化。见 [ADR-0007](../decisions/0007-tool-registry.md)。
- HTTP run API、SSE 事件流和 mock runner 已建立：前端 Run 按钮可创建任务、订阅事件并展示工具执行结果。见 [ADR-0008](../decisions/0008-run-api-sse-mock-runner.md)。
- Workspace 写入与文本编辑工具已建立：`write_file`、`replace_text` 具备路径边界、UTF-8/大小限制、hash 冲突检测和注册表接入。见 [ADR-0009](../decisions/0009-workspace-write-edit-tools.md)。
- 模型适配边界与响应解析器已建立：runner 通过 `ModelClient` 获取响应，`ModelResponseParser` 校验 JSON 输出并将解析失败归为 `MODEL_PARSE_ERROR`。见 [ADR-0010](../decisions/0010-model-boundary-response-parser.md)。
- 多轮 Agent loop 与运行预算已建立：模型工具调用动作和结果都会回填到下一轮请求，轮次、工具调用数和上下文消息窗口有明确限制；真实模型 demo 默认预算和单工具轮次策略见 [ADR-0017](../decisions/0017-real-model-demo-loop-budget.md)，工具动作 transcript 见 [ADR-0019](../decisions/0019-agent-transcript-tool-call-context.md)。
- Run 取消、工具超时和后台任务生命周期已建立：后端保留 run task 句柄，支持 cancel endpoint，runner 检查取消状态，工具执行有 timeout。见 [ADR-0012](../decisions/0012-run-cancellation-timeout-lifecycle.md)。
- 审批策略和 diff/变更展示已建立：可变更工具在后端需要审批；写入/替换工具返回 unified diff，前端 Diff 面板可展示。见 [ADR-0013](../decisions/0013-approval-policy-diff-display.md)。
- 完整 approve/reject/resume 工作流已建立：可变更工具会让 run 进入 `WAITING_FOR_APPROVAL`，前端可批准或拒绝；批准后同一 run 恢复执行工具并继续 Agent loop。见 [ADR-0014](../decisions/0014-approval-resume-workflow.md)。
- OpenAI-compatible 模型适配器已建立：默认仍为 mock，可通过配置切换到 DeepSeek V4 Flash，并从 `DEEPSEEK_API_KEY` 环境变量读取密钥；已完成一次授权后的真实只读端到端 run 和一次原生 tool calling 真实只读 run；provider 空 content 带提醒单次重试见 [ADR-0018](../decisions/0018-empty-model-content-retry.md)；原生 tool calling 协议见 [ADR-0023](../decisions/0023-native-tool-calling-protocol.md)。
- Workspace 命令执行工具已建立：`run_command` 使用 argv 数组而非 shell 字符串，cwd 限制在 workspace 内，输出有截断，并且执行前需要用户审批。见 [ADR-0016](../decisions/0016-workspace-command-tool.md)。
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
- 建立并升级 `workspaces/demo/` 示例 workspace：当前包含一个带故意失败测试的 Python pricing demo，用于真实模型修复演示。
- 实现后端 `WorkspacePathResolver` 与只读工具 `listFiles`、`readFile`、`searchText`。
- 实现后端 `ToolRegistry`，将只读 workspace 工具注册为模型后续可调用的工具入口。
- 实现 `POST /api/runs`、run 状态查询、事件回看和 SSE 订阅；Vue 工作台已接入 mock run 流程。
- 实现 `WorkspaceWriteTools`，将 `write_file` 和 `replace_text` 接入工具注册表。
- 实现 `ModelClient`、模型请求/响应类型、`ModelResponseParser` 和 `HeuristicMockModelClient`；mock runner 已通过模型边界调用工具。
- 实现 `RunBudget`，mock runner 已支持多轮模型/工具循环、工具观察回填、轮次上限、工具调用上限和上下文消息窗口。
- 实现 `RunTaskManager`、`POST /api/runs/{runId}/cancel`、工具执行超时和前端 Cancel 按钮。
- 实现 `ToolApprovalPolicy`、审批事件拦截、写入/替换 unified diff 元数据和前端 Diff 面板。
- 实现 `PendingToolApproval`、approve/reject API、审批后恢复执行和前端审批按钮。
- 实现 `run_command` 命令执行工具，并接入工具注册表、审批策略和 mock run 审批闭环。
- 增强前端工具卡片和命令输出展示：命令审批、执行状态、stdout/stderr、exit code 和 duration 可在录屏中直接呈现。
- 构造真实 demo 编程任务：`workspaces/demo` 现在包含一个无第三方依赖的 Python pricing 项目，基线测试故意失败，供 Agent 修复并运行测试。

## 功能状态

状态含义：未开始 / 进行中 / 已实现未验证 / 已验证 / 阻塞。只有存在实现和对应有效证据才能写已验证。

| 模块 | 当前状态 | 实现位置 | 验证证据 | 决策/说明 |
| --- | --- | --- | --- | --- |
| 前后端工程骨架及独立启动 | 已验证 | [frontend](../frontend)、[backend](../backend) | [APP-001](VERIFICATION.md) | ADR-0001、ADR-0003；验证覆盖构建和后端健康接口 |
| 任务接口与 Web 页面 | 已验证 | [frontend/src/App.vue](../frontend/src/App.vue)、[frontend/src/run/toolCards.ts](../frontend/src/run/toolCards.ts)、[frontend/src/api/runs.ts](../frontend/src/api/runs.ts)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/api](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/api) | [RUNAPI-001](VERIFICATION.md)、[UI-002](VERIFICATION.md)、[LIFE-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md)、[APPROVAL-001](VERIFICATION.md)、[UI-003](VERIFICATION.md) | 可创建/cancel mock run、显示事件、审批、批准后恢复、diff 和命令输出卡片；仍非真实模型任务界面 |
| 模型适配器及 Agent 循环 | 已验证 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/model](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/model)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution) | [CORE-001](VERIFICATION.md)、[RUNAPI-001](VERIFICATION.md)、[MODEL-001](VERIFICATION.md)、[LOOP-001](VERIFICATION.md)、[LIFE-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md)、[APPROVAL-001](VERIFICATION.md)、[MODELAPI-001](VERIFICATION.md)、[REALMODEL-001](VERIFICATION.md)、[REALMODEL-002](VERIFICATION.md)、[COMMAND-001](VERIFICATION.md)、[MODELAPI-002](VERIFICATION.md)、[MODELAPI-003](VERIFICATION.md)、[MODELAPI-005](VERIFICATION.md) | 模型边界、多轮工具循环、预算、取消、工具超时、审批、OpenAI-compatible DeepSeek 适配器、真实 DeepSeek V4 Flash 原生 tool calling 只读 run、provider 空 content 带提醒单次重试、原生 tool calling 单元转换和 mock 命令审批闭环已验证；真实模型写入/命令 run 已通过 deepseek-chat 强指令 demo 验证；V4 Flash 自主/半自主 demo 未通过 |
| 文件工具、搜索与编辑 | 已验证 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace)、[workspaces/demo](../workspaces/demo) | [WORKSPACE-001](VERIFICATION.md)、[WRITE-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md)、[APPROVAL-001](VERIFICATION.md)、[COMMAND-001](VERIFICATION.md)、[DEMO-001](VERIFICATION.md) | list/read/search/write/replace/run_command 已验证；demo workspace 已升级为带故意失败测试的 Python pricing 任务；write/replace 会返回 unified diff；命令返回 stdout/stderr/exit code；可变更工具和命令均需审批 |
| 工具注册表与执行入口 | 已验证 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool) | [TOOLREG-001](VERIFICATION.md)、[WRITE-001](VERIFICATION.md)、[MODEL-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md)、[APPROVAL-001](VERIFICATION.md)、[COMMAND-001](VERIFICATION.md) | `list_files`、`read_file`、`search_text`、`write_file`、`replace_text`、`run_command` 已注册；可变更工具和命令经过审批策略和 approve/reject 流程；OpenAI-compatible 模型适配器可通过原生 tools 或 JSON content fallback 读取这些工具定义 |
| 命令执行、审批、取消 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceCommandTools.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceCommandTools.java)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolApprovalPolicy.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolApprovalPolicy.java)、[frontend/src/App.vue](../frontend/src/App.vue)、[frontend/src/run/toolCards.ts](../frontend/src/run/toolCards.ts) | [LIFE-001](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md)、[APPROVAL-001](VERIFICATION.md)、[COMMAND-001](VERIFICATION.md)、[UI-003](VERIFICATION.md)、[REALDEMO-001](VERIFICATION.md) | Run 取消、工具 timeout、可变更工具审批、approve/reject/resume、`run_command` mock HTTP 审批闭环和命令输出卡片已验证；完整进程树级取消和真实模型命令 run 已通过 deepseek-chat 强指令 demo 验证；V4 Flash 自主/半自主 demo 未通过 |
| 对话上下文及运行预算 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunBudget.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunBudget.java)、[backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java) | [LOOP-001](VERIFICATION.md)、[LOOP-002](VERIFICATION.md)、[MODELAPI-002](VERIFICATION.md) | 默认预算已调整为 8 轮/16 工具调用；真实模型提示要求一次最多一个工具；模型工具动作和工具观察均进入下一轮上下文；真实 token 计数和 provider-specific 裁剪未实现 |
| SSE、工具卡片、Diff、输出 | 已验证 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunEventStream.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunEventStream.java)、[frontend/src/App.vue](../frontend/src/App.vue)、[frontend/src/run/toolCards.ts](../frontend/src/run/toolCards.ts) | [RUNAPI-001](VERIFICATION.md)、[UI-002](VERIFICATION.md)、[CHANGE-001](VERIFICATION.md)、[APPROVAL-001](VERIFICATION.md)、[UI-003](VERIFICATION.md) | SSE 基础事件流、审批事件订阅、审批按钮、Diff 面板、工具卡片和命令 stdout/stderr/exit code 展示已验证 |
| 运行记录与历史回看 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunStore.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunStore.java) | [RUNAPI-001](VERIFICATION.md) | 进程内事件回看已验证；重启后持久化未实现 |
| 真实模型任务及回归测试 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClient.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClient.java)、[workspaces/demo](../workspaces/demo) | [MODELAPI-001](VERIFICATION.md)、[REALMODEL-001](VERIFICATION.md)、[REALMODEL-002](VERIFICATION.md)、[DEMO-001](VERIFICATION.md)、[REALDEMO-001](VERIFICATION.md)、[MODELAPI-005](VERIFICATION.md) | DeepSeek V4 Flash 原生 tool calling 真实只读 run 已验证；OpenAI-compatible 原生 tool calling 已通过替身 HTTP 单元验证；deepseek-chat 强指令 demo 已完成真实写入审批和命令验证；DeepSeek V4 Flash 自主/半自主 demo 未稳定通过 |
| 提交说明、视频与面试材料 | 未开始 | 尚无 | 尚无 | 依据真实能力编写 |

## 下一步

1. 为录屏选择主模型：建议使用已通过真实闭环的 `deepseek-chat`，或后续接入更强工具协议遵循模型；DeepSeek V4 Flash 不建议作为主录屏模型。
2. 补充命令进程树级取消或明确演示命令范围，避免长时间命令残留。
3. 增加完整录屏前浏览器验收，确认读文件、写入 diff、命令输出、Approve/Reject 按钮和 run history 的视觉状态。
4. 确认公开仓库的账户、名称及可公开文件；首次公开推送前复核题目 PDF、日志、密钥和演示材料。

## 风险与待定项

- DeepSeek V4 Flash 已完成真实只读端到端验证，但自主/半自主 demo 修复未稳定通过；deepseek-chat 强指令 demo 已完成真实写入审批、命令审批、unittest 验证和最终总结。
- 公开远程仓库尚未建立；当前只有本地 Git 历史。
- 演示案例已确定为 Python pricing bugfix；deepseek-chat 强指令修复闭环已验证，自然语言自主定位 bug 闭环尚未稳定通过。
- 当前有已验证的 mock run 前后端闭环、写入/编辑工具、命令执行工具、多轮 loop、取消、工具超时、审批拦截策略、完整 approve/reject/resume、diff 元数据/展示构建、工具卡片/命令输出 UI、OpenAI-compatible 模型适配器，以及一次 DeepSeek V4 Flash 原生 tool calling 真实只读端到端 run；但没有完整进程树级取消；自然语言自主定位 bug 的真实模型 run 尚未稳定通过。
