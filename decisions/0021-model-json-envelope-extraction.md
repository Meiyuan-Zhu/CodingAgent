# ADR-0021：模型 JSON 外壳容错提取

- 状态：accepted
- 日期：2026-08-28
- 关联：[ADR-0010](0010-model-boundary-response-parser.md)、[ADR-0020](0020-blank-tool-message-normalization.md)

## 背景

真实 DeepSeek V4 Flash demo run 在修复空 content 和空 message 后，出现 `Model response is not valid JSON`。这说明模型可能返回了带 Markdown 代码块或说明文字的 JSON，而不是纯 JSON object。

系统提示仍要求“只返回 JSON”，但真实模型在多轮工具上下文中可能多输出外壳。如果边界层完全拒绝，会让 demo 被展示格式问题阻断；如果放宽内部字段校验，则会损害安全性。

## 决策

`ModelResponseParser` 先按完整响应解析 JSON。失败时，从响应文本中提取第一个括号平衡的 JSON object，再按原有协议继续校验：`message`、`finish_reason`、`tool_calls`、工具参数仍走既有规则。

提取不到完整 JSON object，或提取出的对象仍无法解析时，继续以 `MODEL_PARSE_ERROR` 失败。

## 理由

- 兼容模型常见的 Markdown/prose 外壳。
- 不降低工具调用结构、重复 id、参数对象和 finish reason 校验。
- 解析逻辑集中在模型边界，Agent loop 和工具执行不承担格式猜测。

## 验证

- 待执行：后端 `mvn test` 覆盖 Markdown 代码块和前后夹杂文字的 JSON object 提取。
- 待执行：真实 DeepSeek V4 Flash demo 修复闭环。
