# ADR-0015：OpenAI-compatible DeepSeek 模型适配器

- 日期：2026-08-28
- 状态：accepted
- 决策依据/确认来源：用户已配置 DeepSeek API key，并要求先接入 DeepSeek V4 Flash 试用
- 实现状态：已实现，DeepSeek V4 Flash 真实只读端到端 run 已验证
- 取代/被取代：无

## 问题与约束

项目需要从 mock 模型推进到真实模型 API，同时保持题目要求的 Agent 核心自研边界：Agent loop、工具定义、本地工具执行、审批和响应解析都由本项目控制，不能依赖模型厂商托管的文件或代码执行工具。

DeepSeek V4 Flash 提供 OpenAI 兼容 API，适合作为第一个真实模型 provider。API key 属于凭据，只能从本地环境变量读取，不能写入源码、开发记录、日志截图或提交说明。

## 备选方案

1. 使用 DeepSeek 专用 SDK。
   - 优点：调用封装更直接。
   - 代价：新增厂商依赖，迁移 Qwen/OpenAI 时要写更多分支；也不利于说明 provider-neutral 边界。

2. 直接写死 DeepSeek endpoint 和模型名。
   - 优点：最快能跑通。
   - 代价：后续切 DeepSeek V4 Pro、Qwen 或 OpenAI 要改代码；配置和凭据边界不清晰。

3. 实现可配置的 OpenAI-compatible HTTP 客户端。
   - 优点：DeepSeek、Qwen、OpenAI 这类兼容 Chat Completions 的模型可以共用一条适配路径；密钥从环境变量读取；测试可通过替身 HTTP transport 验证请求和响应解析。
   - 代价：需要维护一层 provider 响应提取逻辑；不同厂商的细节差异后续可能需要配置开关。

## 决定与理由

采用方案 3：新增 `OpenAiCompatibleModelClient`，默认配置指向 DeepSeek V4 Flash：

```properties
agent.model.provider=openai-compatible
agent.model.base-url=https://api.deepseek.com
agent.model.name=deepseek-v4-flash
agent.model.api-key-env=DEEPSEEK_API_KEY
```

应用默认仍使用 `mock`，只有启动参数或本地配置显式切换到 `openai-compatible` 时才调用真实模型。这样可以保证本地开发、测试和演示准备不依赖付费 API，也避免 CI 或他人环境因为没有 key 直接失败。

真实模型回复仍要求遵守项目内部 JSON 协议，并继续交给 `ModelResponseParser` 校验；工具执行和审批仍由本项目完成。当前不使用 provider-native tool calls，避免把工具调用状态绑定到某家 API 的消息格式。DeepSeek 适配路径显式关闭 thinking mode，因为当前内部 `ModelMessage` 不保存 provider 的 `reasoning_content`，关闭后多轮 JSON 协议更容易保持稳定。

## 代价与限制

- 真实 DeepSeek 调用会把用户 prompt、系统协议、工具定义，以及后续工具观察发送给外部模型服务；执行前需要用户明确授权。
- 当前适配器只实现 Chat Completions 风格的单次请求，不处理流式 token、provider usage 统计、thinking/reasoning 内容保存或模型原生 tool call message。
- 工具观察被映射为普通 user message，以保持 OpenAI 兼容格式简单；如果后续需要原生 tool calls，应重新设计 `ModelMessage`，记录 assistant tool calls 和 tool_call_id。
- 默认 JSON 模式依赖模型遵循提示；模型输出异常仍会以 `MODEL_PARSE_ERROR` 结束 run。

## 实现与验证证据

- 代码位置：
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClient.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/AgentModelProperties.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/JavaHttpModelTransport.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/ModelConfiguration.java`
- 验证记录：[MODELAPI-001](../memory/VERIFICATION.md#modelapi-001openai-compatible-deepseek-adapter-测试)、[REALMODEL-001](../memory/VERIFICATION.md#realmodel-001deepseek-v4-flash-真实只读-run-验证)
- 关联提交/运行：`60aeba7 feat: add openai compatible model adapter`。

## 何时重新考虑

- 需要使用 provider-native tool calls、流式输出或 usage/token 精确计费时。
- DeepSeek/OpenAI/Qwen 的兼容 API 出现不可忽略的字段差异，导致单一适配器出现过多条件分支时。
- 项目需要多 provider fallback、限流重试或更细粒度错误分类时。
