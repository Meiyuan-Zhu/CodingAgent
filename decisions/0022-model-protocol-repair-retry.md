# ADR-0022：模型协议修复重试

- 状态：accepted
- 日期：2026-08-28
- 取代：扩展 [ADR-0018](0018-empty-model-content-retry.md) 的空 content 重试策略。
- 关联：[ADR-0020](0020-blank-tool-message-normalization.md)、[ADR-0021](0021-model-json-envelope-extraction.md)

## 背景

真实 DeepSeek V4 Flash demo run 连续暴露 provider 空 content、空 message 和非 JSON 外壳问题。解析器已经能处理 JSON 外壳和带合法工具调用的空 message，但如果模型返回无可执行动作且 message 空白，仍应失败。

完全失败会让真实模型的轻微协议漂移中断任务；盲目放宽解析又会掩盖工具调用错误。

## 决策

OpenAI-compatible 适配器在模型响应还没有产生可执行动作前，对以下可恢复协议问题最多重试一次：

- provider HTTP 2xx 但 `choices[0].message.content` 为空/缺失。
- 响应无法解析为 JSON，且无法提取完整 JSON object。
- JSON 协议中的 `message` 为空/缺失，且不能被 ADR-0020 的合法工具调用降级处理。

重试会在原上下文末尾追加协议修复提醒，说明上一条响应不能被接受，并要求重新输出一个非空 JSON 协议对象。工具结构错误、参数错误、未知 `finish_reason`、HTTP 错误、工具失败、审批和预算错误不重试。

## 理由

- 重试发生在模型边界，还未执行工具，不会绕过审批策略。
- 将 provider/模型格式漂移与工具安全校验分开。
- 一次重试成本可控，第二次仍失败则保留第一次错误，便于排查。

## 验证

- 待执行：后端 `mvn test` 覆盖空 content 和空 message 的协议修复重试。
- 待执行：真实 DeepSeek V4 Flash demo 修复闭环。
