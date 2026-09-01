# 决策索引

这里只记录“为什么这样设计”，当前进度查看 [开发状态](../memory/STATUS.md)。

| 编号 | 主题 | 状态 | 实现情况 |
| --- | --- | --- | --- |
| [ADR-0001](0001-frontend-backend-stack.md) | Vue 3 + Java/Spring Boot，前后端分离 | accepted | 工程骨架已建立，Agent 核心未实现 |
| [ADR-0002](0002-development-records.md) | 决策与开发记录分开维护 | accepted | 目录和初始记录已建立 |
| [ADR-0003](0003-framework-baseline.md) | Vue 3 与 Spring Boot 工程基线 | accepted | 前后端骨架及健康接口已验证 |
| [ADR-0004](0004-local-workbench-ui.md) | 本地 workspace 工作台式 Web 界面 | accepted | Vue 工作台壳已实现，Agent 执行未实现 |
| [ADR-0005](0005-run-protocol-domain.md) | Agent 运行协议领域模型 | accepted | 后端领域模型和状态转换测试已验证 |
| [ADR-0006](0006-workspace-boundary-read-tools.md) | Workspace 边界与只读文件工具 | accepted | 后端路径边界和 list/read/search 已验证 |
| [ADR-0007](0007-tool-registry.md) | 工具注册表边界 | accepted | 后端工具定义、参数校验、执行入口和 workspace 只读工具注册已验证 |
| [ADR-0008](0008-run-api-sse-mock-runner.md) | Run API、SSE 事件流与 mock runner | accepted | 后端 run 创建/查询/事件回看/SSE 和前端 Run 交互已验证 |
| [ADR-0009](0009-workspace-write-edit-tools.md) | Workspace 写入与文本编辑工具 | accepted | `write_file`、`replace_text`、hash 冲突检测和注册表接入已验证 |
| [ADR-0010](0010-model-boundary-response-parser.md) | 模型适配边界与响应解析器 | accepted | 模型请求/响应边界、JSON 解析、mock 客户端接入和解析失败终止已验证；真实 API 接入由 ADR-0015 补充 |
| [ADR-0011](0011-agent-loop-budget.md) | 多轮 Agent loop 与运行预算 | superseded | 多轮模型/工具循环、轮次上限、工具调用上限和上下文消息窗口已验证；默认预算和真实模型单工具轮次策略由 ADR-0017 部分取代 |
| [ADR-0012](0012-run-cancellation-timeout-lifecycle.md) | Run 取消、超时与后台任务生命周期 | accepted | 取消 API、后台任务句柄、工具超时和前端 Cancel 入口已验证；命令进程级取消未实现 |
| [ADR-0013](0013-approval-policy-diff-display.md) | 审批策略与 diff/变更展示 | superseded | 可变更工具审批策略、审批事件、写入/替换 diff 元数据和前端 Diff 面板已验证；审批恢复限制被 ADR-0014 取代 |
| [ADR-0014](0014-approval-resume-workflow.md) | 审批后的 Agent 恢复执行工作流 | accepted | Approve/Reject API、pending approval continuation、前端审批按钮和审批后 diff 闭环已验证 |
| [ADR-0015](0015-openai-compatible-deepseek-model-adapter.md) | OpenAI-compatible DeepSeek 模型适配器 | superseded | OpenAI-compatible adapter 设计仍保留；默认 provider 策略被 ADR-0037 取代 |
| [ADR-0016](0016-workspace-command-tool.md) | Workspace 命令执行工具 | accepted | `run_command` 已接入工具注册表、审批策略和 mock HTTP 审批闭环；真实模型命令 run 尚未验证 |
| [ADR-0017](0017-real-model-demo-loop-budget.md) | 真实模型 demo 的单工具轮次策略与预算调整 | accepted | 默认 round 上限提高为极端兜底，主要依赖工具调用上限/取消/模型 STOP 终止；模型提示要求一次最多一个工具 |
| [ADR-0018](0018-empty-model-content-retry.md) | OpenAI-compatible 空 content 响应重试 | accepted | content 为空/缺失时最多追加协议修复提醒并重试一次；策略由 ADR-0022 扩展 |
| [ADR-0019](0019-agent-transcript-tool-call-context.md) | Agent transcript 中保留模型工具动作 | accepted | 下一轮模型上下文包含上一轮工具调用摘要；真实 demo 修复闭环待验证 |
| [ADR-0020](0020-blank-tool-message-normalization.md) | 工具调用响应的空 message 降级 | accepted | tool_calls 合法时为空 message 补默认展示文案；真实 demo 修复闭环待验证 |
| [ADR-0021](0021-model-json-envelope-extraction.md) | 模型 JSON 外壳容错提取 | accepted | 解析器可从 Markdown/prose 外壳中提取第一个 JSON object；真实 demo 修复闭环待验证 |
| [ADR-0022](0022-model-protocol-repair-retry.md) | 模型协议修复重试 | accepted | 对空 content、不可提取 JSON、不可降级空 message 最多追加协议修复提醒并重试一次；真实 demo 修复闭环待验证 |
| [ADR-0023](0023-native-tool-calling-protocol.md) | OpenAI-compatible 原生 tool calling 协议 | accepted | native tools 请求/响应、结构化 tool message、JSON content fallback 和 DeepSeek V4 Flash 真实只读 run 已验证 |
| [ADR-0024](0024-codex-like-workbench-ui-projection.md) | Codex-like 工作台 UI 与事件投影层 | accepted | 前端三栏 + 底部 terminal、Timeline/Inspector 投影层已实现并通过 build/目视检查 |
| [ADR-0025](0025-agent-change-undo.md) | Agent 文件变更撤销 | accepted | 用户触发单次文件工具变更撤销，后端私有 journal 保存旧内容，撤销前校验当前 hash；后端测试和前端构建已验证 |
| [ADR-0026](0026-h2-run-persistence.md) | H2 持久化 run、事件与撤销记录 | accepted | H2 file mode + Spring JDBC 已接入，持久化 run/event/pending approval/undo snapshot；后端测试、前端构建和跨重启 HTTP 验证通过 |
| [ADR-0027](0027-recoverable-tool-failure-observations.md) | 工具失败作为可恢复 Observation | accepted | `ToolResult.success=false` 不再直接终止 run，而是回填给 LLM 继续下一轮；后端测试已验证 |
| [ADR-0028](0028-tool-observation-schema-and-edit-alias.md) | 工具 Observation 结构化与 edit_file 别名 | accepted | 新增 `edit_file` alias，工具成功/失败 observation JSON 显式包含 success/message，命令结果包含 timedOut；后端测试和前端构建已验证 |
| [ADR-0029](0029-context-window-tool-pair-trimming.md) | 上下文窗口按工具调用配对裁剪 | accepted | 保留 system 和初始 user；尾部 context 按 assistant tool_calls + tool results 成组裁剪，避免孤立 tool message；后端测试已验证 |
| [ADR-0030](0030-failure-recovery-taxonomy.md) | Failure Recovery 错误分类与可恢复工具失败 | accepted | 工具失败 observation 增加 recoverable/failureKind/recoveryHint；workspace 错误码细分；已验证 read_file 缺失后继续 list_files/read_file 恢复 |
| [ADR-0031](0031-agent-operating-policy-system-prompt.md) | Agent Operating Policy System Prompt | accepted | Runner system prompt 改为简短 operating policy；adapter 仅保留 provider 输出协议；后端测试已验证关键约束注入 |
| [ADR-0032](0032-command-process-tree-cleanup.md) | 命令中断时清理进程树 | accepted | `run_command` interrupt/timeout 时按 descendants→parent 清理进程树；受限环境降级 best-effort；后端测试已验证 |
| [ADR-0033](0033-provider-token-streaming.md) | Provider token-level streaming | accepted | OpenAI-compatible native tools 请求支持 `stream=true`，runner 持久化 `MODEL_MESSAGE_DELTA`，前端可实时拼接展示文本增量 |
| [ADR-0034](0034-command-argument-normalization-and-stream-fallback.md) | 命令参数归一化与空 Stream 降级 | accepted | `run_command` 兼容 JSON-string argv array 但继续拒绝 shell 字符串；provider 空 stream 降级为非 streaming native retry；后端/前端测试已验证 |
| [ADR-0035](0035-single-tool-call-runtime-enforcement.md) | Runtime 单工具调用强制 | accepted | 模型一次返回多个 tool calls 时 Runtime 只接收并执行第一个，避免 native tools 历史上下文触发 provider HTTP 400 |
| [ADR-0036](0036-streaming-transport-fallback.md) | Streaming 传输失败降级 | accepted | native tools streaming 遇到 IO 传输异常时降级为非 streaming native retry；HTTP/provider 协议错误仍显式失败 |
| [ADR-0037](0037-default-real-model-test-mock-override.md) | 默认真实模型与测试 mock 覆盖 | accepted | 应用默认使用 DeepSeek V4 Flash native tools；Spring Boot 集成测试显式覆盖 `agent.model.provider=mock` |

## 规则

- 使用四位递增编号和简短主题，按 [模板](TEMPLATE.md) 新建文件，不重用旧编号。
- 状态：`proposed` 为待确认，`accepted` 为已接受，`superseded` 为已被新决策取代，`rejected` 为明确不采用。
- 决策被接受不代表功能已经实现或验证。
- 重要决策记录需求、备选、理由、代价、代码与验证证据。没有证据就写尚无，不填虚构路径或提交。
- 常规重构和样式修改不必新增 ADR；改变已有架构、权限或接口语义时才需要。
- 修改已接受的决定须保留旧理由，并链接到新的取代记录。目录搬迁、链接修复可直接维护并注明。
