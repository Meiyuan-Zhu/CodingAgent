# ADR-0018：OpenAI-compatible 空 content 响应重试

- 状态：accepted
- 日期：2026-08-28
- 关联：[ADR-0015](0015-openai-compatible-deepseek-model-adapter.md)、[ADR-0017](0017-real-model-demo-loop-budget.md)

## 背景

真实 DeepSeek V4 Flash demo run 中，provider 返回 HTTP 2xx，`choices[0].finish_reason=stop`，`message` 也包含 `content` 字段，但 `content` 为空字符串。由于本项目要求模型输出必须是本地 Agent 协议 JSON，空 content 无法被解析为有效模型动作。

如果直接把空 content 当作最终回答，会掩盖协议错误；如果直接失败，真实 demo 受 provider 偶发空回复影响较大。

## 决策

OpenAI-compatible 适配器对“HTTP 2xx 且 content 为空/缺失”的响应最多自动重试一次。重试仍使用同一个请求体，不修改用户 prompt、工具定义或上下文。第二次仍为空时保留第一次错误并终止为模型错误。

不对以下情况重试：

- HTTP 非 2xx。
- provider 返回非 JSON。
- content 非空但不符合本地 Agent JSON 协议。
- 工具执行、审批、取消或预算错误。

## 理由

- 空 content 没有可执行语义，不能进入 Agent loop。
- 一次重试可以吸收 provider 偶发空回复，代价可控。
- 不重试协议解析失败，避免把模型格式错误伪装成网络抖动。

## 验证

- 待执行：后端 `mvn test` 覆盖一次空 content 后成功的重试路径。
- 待执行：真实 DeepSeek V4 Flash demo 修复闭环。
