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
| [ADR-0015](0015-openai-compatible-deepseek-model-adapter.md) | OpenAI-compatible DeepSeek 模型适配器 | accepted | DeepSeek V4 Flash 配置、HTTP 客户端、环境变量密钥读取、替身 HTTP 测试和授权后的真实只读 run 已验证 |
| [ADR-0016](0016-workspace-command-tool.md) | Workspace 命令执行工具 | accepted | `run_command` 已接入工具注册表、审批策略和 mock HTTP 审批闭环；真实模型命令 run 尚未验证 |
| [ADR-0017](0017-real-model-demo-loop-budget.md) | 真实模型 demo 的单工具轮次策略与预算调整 | accepted | 默认预算调整为 8 轮/16 工具调用，模型提示要求一次最多一个工具；真实 demo 修复闭环待验证 |
| [ADR-0018](0018-empty-model-content-retry.md) | OpenAI-compatible 空 content 响应重试 | accepted | content 为空/缺失时最多重试一次；真实 demo 修复闭环待验证 |

## 规则

- 使用四位递增编号和简短主题，按 [模板](TEMPLATE.md) 新建文件，不重用旧编号。
- 状态：`proposed` 为待确认，`accepted` 为已接受，`superseded` 为已被新决策取代，`rejected` 为明确不采用。
- 决策被接受不代表功能已经实现或验证。
- 重要决策记录需求、备选、理由、代价、代码与验证证据。没有证据就写尚无，不填虚构路径或提交。
- 常规重构和样式修改不必新增 ADR；改变已有架构、权限或接口语义时才需要。
- 修改已接受的决定须保留旧理由，并链接到新的取代记录。目录搬迁、链接修复可直接维护并注明。
