# ADR-0019：Agent transcript 中保留模型工具动作

- 状态：accepted
- 日期：2026-08-28
- 关联：[ADR-0010](0010-model-boundary-response-parser.md)、[ADR-0011](0011-agent-loop-budget.md)、[ADR-0014](0014-approval-resume-workflow.md)

## 背景

真实 DeepSeek V4 Flash demo run 暴露出一个上下文问题：runner 发给下一轮模型的 assistant 消息只包含 `message` 字段，也就是用户可读解释；上一轮模型实际提出的 `tool_calls` 只存在事件流和 Java 对象中，没有进入下一轮模型上下文。

这会导致真实模型难以把“我刚才请求了哪个工具”和“现在收到的是哪个工具观察”稳定关联起来。mock 模型对这种缺失不敏感，但真实模型会重复动作或返回空 content。

## 决策

当模型返回工具调用时，runner 写入下一轮上下文的 assistant transcript 不再只包含用户可读 `message`，还包含每个工具调用的 `tool_call_id`、工具名和参数摘要。工具执行结果仍作为单独的 tool observation 消息回填。

事件流保持原样：前端继续通过结构化事件展示模型消息、工具请求、审批、执行结果和 diff。

## 理由

- Agent loop 的上下文需要记录“模型动作”和“工具观察”两类事实。
- 保留工具动作能让真实模型更容易继续推理，也让运行可追踪性更强。
- 不改变 HTTP/SSE 事件协议，前端无需同步改动。

## 验证

- 待执行：后端 `mvn test` 覆盖下一轮 ModelRequest 包含上一轮工具动作 transcript。
- 待执行：真实 DeepSeek V4 Flash demo 修复闭环。
