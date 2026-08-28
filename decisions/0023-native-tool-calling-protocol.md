# ADR-0023: OpenAI-compatible 原生 tool calling 协议

## 状态

accepted

## 背景

早期 DeepSeek 接入采用 `response_format=json_object`，要求模型在 `message.content` 中输出项目自定义 JSON 协议。这个方案能快速复用已有 `ModelResponseParser`，但真实 demo 暴露出协议漂移：模型可能返回空 content、Markdown 外壳、或把工具意图放在 provider 原生字段之外，导致我们需要不断增加容错。

DeepSeek 的 OpenAI-compatible Chat Completions 文档支持 `tools` 参数和 assistant `tool_calls` 返回；工具调用中的 `function.arguments` 是 JSON 字符串，应用需要在本地执行工具并把 `role=tool`、`tool_call_id` 的结果消息回填。文档也提醒模型生成的参数仍可能无效，因此不能跳过本地校验和审批。

## 决策

OpenAI-compatible 模型适配器默认使用 provider 原生 tool calling 协议：

- 请求体通过 `tools` 传递本地工具定义，工具类型限定为 `function`。
- 模型返回 assistant `tool_calls` 时，适配器解析为内部 `ToolCall`，再交给本地 `ToolRegistry`、`ToolApprovalPolicy` 和 runner 执行。
- 工具执行结果以 `role=tool`、`tool_call_id` 形式进入下一轮模型上下文，保留 provider 可追踪的调用链。
- 请求中不再使用 `response_format=json_object` 作为默认工具协议。
- `thinking` 显式保持 disabled；DeepSeek thinking mode + tools 需要额外回传 `reasoning_content`，当前不纳入主路径。
- 保留 `agent.model.tool-protocol=json-content` 作为兼容 fallback，用于复现旧解析器测试或在 provider 原生 tools 不可用时临时切换。

内部 `ModelMessage` 扩展为结构化消息：assistant message 可携带上一轮 `toolCalls`，tool message 携带 `toolCallId` 和 `toolName`。HTTP 层和前端事件协议不直接依赖 provider 原生格式。

## 理由

原生 tool calling 比 content JSON 更稳定，也更符合 DeepSeek/OpenAI-compatible 协议；同时它把“模型提出工具调用”和“本地执行工具”分开，和本项目的审批、diff 展示、命令输出回填天然一致。

保留 fallback 的原因是降低迁移风险：已有 JSON 协议解析器、协议修复重试和相关测试仍有价值，后续接入不支持 tools 的兼容模型时也可以复用。

## 替代方案

1. 继续只使用 content JSON 协议。实现简单，但真实模型协议漂移成本高，录屏时稳定性较差。
2. 引入 Spring AI、LangChain4j 或其他 Agent 框架。能减少样板代码，但不符合项目“核心逻辑自行实现”的边界。
3. 同时启用 DeepSeek thinking mode。可能提升推理能力，但工具消息链需要额外处理 `reasoning_content`，当前会扩大验证面。

## 影响

- `OpenAiCompatibleModelClient` 同时支持 native tools 和 legacy JSON content 两种协议。
- `ModelMessage` 从纯文本消息变为可表达 assistant tool calls / tool result ids 的结构化消息。
- Runner 不再把工具调用动作拼成 assistant 文本 transcript；legacy JSON content 的 transcript 只在适配器序列化时生成。
- 本地参数校验、审批策略、工具执行和 workspace 边界不变。

## 验证

- `mvn test`：2026-08-28 20:09 CST 通过，124 tests passed。
- 新增/更新测试覆盖 native `tools` 请求体、native `tool_calls` 响应解析、assistant tool_calls 与 tool result 消息回填、legacy JSON content fallback、以及 runner 上下文中的结构化工具动作。
- DeepSeek V4 Flash 真实只读 run `0edd0f1a-cc84-484e-b436-0888a85a30a5` 通过：模型通过原生 `tool_calls` 调用 `list_files` 和 `read_file`，本地执行工具并回填后以 `SUCCEEDED / COMPLETED` 结束。
