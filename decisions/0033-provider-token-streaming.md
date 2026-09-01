# ADR-0033：Provider Token-Level Streaming

- 日期：2026-08-30
- 状态：accepted
- 决策依据/确认来源：用户明确要求“做成 token-level streaming”
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0024、ADR-0023

## 问题与约束

前端此前已有 SSE 事件流和消息级渐进 reveal，但模型客户端仍等待 provider 返回完整 assistant message 后才发出 `MODEL_MESSAGE_RECEIVED`。这能展示 Agent 步骤，却不是真正的模型 token-level streaming。

需要在不破坏现有 Agent loop、tool calling、历史回放和 mock 测试的前提下，让 OpenAI-compatible native tools 路径可以边接收 provider stream chunk 边推送文本增量。

## 备选方案

1. 替换 `ModelClient.complete` 为全流式接口。协议更纯，但会重写 mock、测试和 runner 主流程，风险偏大。
2. 在 `ModelClient` 外增加可选 streaming 扩展接口。支持 streaming 的 provider 走增量回调，不支持的 provider 继续同步返回。
3. 只做前端假流式 reveal。成本最低，但不满足 token-level streaming。

## 决定与理由

采用方案 2：新增 `StreamingModelClient` 和 `ModelStreamListener`，runner 在发现当前模型客户端支持 streaming 时调用 `completeStreaming`，并把每段文本 delta 持久化为 `MODEL_MESSAGE_DELTA` run event。最终完整 response 仍以 `MODEL_MESSAGE_RECEIVED` 事件保存，用于历史回放、最终状态、tool call 解析和既有 UI 兼容。

OpenAI-compatible native tools 请求增加 `stream=true`，HTTP transport 读取 `text/event-stream` 行；客户端累计 `delta.content` 作为可见文本，并累计 fragmented `delta.tool_calls[].function.arguments`，最终按 provider `finish_reason` 组装成现有 `ModelResponse`。

## 代价与限制

- 当前只对 native tools 协议实现真正 provider streaming；JSON content 兼容协议继续走同步 complete。
- Streaming tool call arguments 需要等 provider 发完整后才能执行工具，因此工具调用本身仍在 round 结束时进入 Agent loop。
- `MODEL_MESSAGE_DELTA` 会增加事件数量和本地持久化写入次数。
- 不保存 provider usage/token 计费数据。

## 实现与验证证据

- 代码位置：[OpenAiCompatibleModelClient](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClient.java)、[StreamingModelClient](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/StreamingModelClient.java)、[MockAgentRunner](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java)、[RunEventType](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/run/RunEventType.java)、[frontend timeline](../frontend/src/run/timeline.ts)。
- 验证记录：[STREAM-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚无，提交后补充。

## 何时重新考虑

如果后续要支持 provider usage 统计、多 provider streaming 差异、真正取消 HTTP stream request、或把 token stream 与 tool-call stream 展示成更细粒度 UI 状态，需要把当前简单 delta event 扩展为更完整的 model stream protocol。
