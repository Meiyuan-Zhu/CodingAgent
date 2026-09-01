# ADR-0035：Runtime 单工具调用强制

- 日期：2026-09-01
- 状态：accepted
- 决策依据/确认来源：用户截图反馈真实模型 run 在工具 observation 后再次 `model_error`
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0017、ADR-0023、ADR-0029、ADR-0033

## 问题与约束

真实 DeepSeek native tools run 中，模型虽然被 prompt 要求“一次最多一个工具”，但仍可能在单个 assistant response 中返回多个 `tool_calls`。随后这些多工具 assistant 消息进入历史上下文；长任务继续向 provider 发送 native messages 时，DeepSeek 返回 HTTP 400：

```text
An assistant message with 'tool_calls' must be followed by tool messages responding to each 'tool_call_id'.
```

这会让 run 在工具已经成功执行、并且本应继续恢复的情况下以 `MODEL_ERROR` 失败。

## 决定与理由

Runtime 层不再信任模型遵守单工具轮次约束。每次模型返回 `TOOL_CALLS` 时，只接收第一个 tool call 进入 transcript，并只执行这一个 tool call；其他同批 tool calls 被丢弃。工具 observation 回填后，下一轮再让模型基于新状态决定下一步。

这样 provider-facing context 始终保持稳定形态：

```text
assistant(one tool_call)
tool(result for that tool_call)
```

该策略与 Codex-like 的逐步执行体验一致，也让审批、撤销、预算和上下文裁剪更简单。

## 代价与限制

- 如果模型一次返回多个只读工具，执行会从并行变为逐轮串行，可能增加 round 数。
- 被丢弃的同批工具不会执行；模型需在下一轮重新提出仍然必要的工具。
- 该策略不能修复本机编译器/Xcode 环境问题，只避免 provider 因 tool-call history 格式拒绝请求。

## 实现与验证证据

- 代码位置：[MockAgentRunner](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java)。
- 测试位置：[MockAgentRunnerTests](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunnerTests.java)。
- 验证记录：[BUGFIX-002](../memory/VERIFICATION.md)。
- 关联提交/运行：尚无，提交后补充。

## 何时重新考虑

如果后续明确要支持并行工具调用，需要引入 provider-specific capability 检测、严格的 assistant/tool result 成组存储和更复杂的审批恢复状态。目前作业 demo 更需要稳定闭环，单工具轮次优先。
