# ADR-0027：工具失败作为可恢复 Observation

- 日期：2026-08-30
- 状态：accepted
- 决策依据/确认来源：用户要求梳理 Agent Runtime 三类退出语义，并明确 tool 执行失败不应属于不可恢复异常。
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0011、ADR-0012、ADR-0014 的运行时终止语义

## 问题与约束

Coding Agent 的核心循环必须是：

User Task → LLM → Tool Call → Local Tool Execution → Observation → LLM → ... → Final Answer。

此前 runner 已经维护消息历史、多轮调用工具、工具结果回填和预算限制，但对 `ToolResult.success=false` 的处理过早：工具失败后直接把 run 标记为 `TOOL_ERROR` 或 `TIME_LIMIT`，没有把失败原因作为 observation 交回模型。这会让常见可恢复问题无法由 Agent 自修，例如文件路径猜错、文本替换 miss、unknown tool、参数错误或命令超时。

## 备选方案

1. 保持工具失败即 run failed。实现简单，但不符合 Agent 自主迭代目标。
2. 所有工具失败都继续交回 LLM。更符合 Agent loop；由 round/tool budget 防止无限重试。
3. 只对部分工具失败继续，timeout 等仍直接终止。更精细，但早期规则复杂，容易误分可恢复错误。

## 决定与理由

采用方案 2：工具执行失败不再直接终止 run，而是追加为 `ModelMessage.tool(...)`，内容中包含 `tool_call_id`、`tool_name`、`success=false` 和失败文本，然后进入下一轮模型调用。

Runtime 的退出语义划分为：

- 正常完成：模型不再调用工具，返回最终回答，run `SUCCEEDED / COMPLETED`。
- 系统强制结束：round limit、tool call limit、token/length limit、用户取消等防护机制触发，run 终止。
- 不可恢复失败：模型 API 不可用、provider 响应无法解析、内部 runtime 异常等，run `FAILED`。

工具失败属于可恢复 observation，不属于不可恢复失败。若模型无法恢复，最终会由预算限制或模型最终回答来结束。

## 代价与限制

- 模型可能在错误路径上重试多轮；依赖 `maxRounds` 和 `maxToolCalls` 防止无限循环。
- 当前 observation 文本是简洁文本格式，不是结构化 JSON；后续如需更稳定的模型行为，可把 error code、metadata 和 content 统一包装为 JSON。
- 命令 timeout 现在也会作为失败 observation 交给模型；如果命令留下子进程，仍需要单独的进程树清理改进。

## 实现与验证证据

- 代码位置：[MockAgentRunner.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java)。
- 测试位置：[MockAgentRunnerTests.java](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunnerTests.java)。
- 验证记录：[RUNTIME-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚未提交。

## 何时重新考虑

如果真实模型频繁在失败 observation 后无效重试，应增加失败类型策略，例如对 permission/security 错误直接终止，对 path/edit miss/command non-zero 继续；或引入同一工具同一错误的 no-progress 检测。
