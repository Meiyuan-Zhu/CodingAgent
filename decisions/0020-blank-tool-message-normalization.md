# ADR-0020：工具调用响应的空 message 降级

- 状态：accepted
- 日期：2026-08-28
- 关联：[ADR-0010](0010-model-boundary-response-parser.md)、[ADR-0019](0019-agent-transcript-tool-call-context.md)

## 背景

真实 DeepSeek V4 Flash demo run 在工具动作 transcript 修复后，不再表现为 provider 空 content，而是返回了可解析 JSON，但 `message` 字段为空。原解析器把任何空 `message` 都视为协议错误。

在本地 Agent 协议中，`message` 主要用于前端展示和用户解释；`tool_calls` 才是本轮可执行动作。若 `tool_calls` 合法，仅因展示文案为空而中断，会降低真实模型兼容性。

## 决策

当模型响应满足以下条件时，解析器为 `message` 补默认值 `Model requested tool execution.` 并继续：

- `finish_reason` 为 `tool_calls`。
- `tool_calls` 是合法且非空的数组。
- `message` 缺失、null 或空白字符串。

如果 `finish_reason` 不是 `tool_calls`，或工具调用结构无效，仍按解析错误终止。非字符串 `message` 仍视为解析错误。

## 理由

- 保留对可执行动作的严格校验。
- 将展示文案缺失降级为默认文案，不掩盖工具参数错误。
- 前端和事件流仍能展示具体工具名、参数、审批和结果。

## 验证

- 待执行：后端 `mvn test` 覆盖空 message + 合法工具调用的降级路径。
- 待执行：真实 DeepSeek V4 Flash demo 修复闭环。
