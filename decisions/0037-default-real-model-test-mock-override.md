# ADR-0037：默认真实模型与测试 mock 覆盖

- 日期：2026-09-01
- 状态：accepted
- 决策依据/确认来源：用户指出本地启动后仍是 mock model，当前项目已进入真实模型 demo 阶段
- 实现状态：已实现，待重新验证完整测试
- 取代/被取代：取代 [ADR-0015](0015-openai-compatible-deepseek-model-adapter.md) 中“应用默认仍使用 mock”的默认 provider 策略

## 问题与约束

项目早期默认使用 mock provider，便于没有模型 key 时本地开发。但当前用户已配置 DeepSeek API key，并且主要目标是展示真实 Coding Agent 行为。继续默认 mock 会导致普通启动命令跑出 mock model 文案，影响自测和录屏判断。

同时，自动化测试不能依赖外部模型服务或真实 API key。测试应保持离线、可重复，不应因为网络、额度或环境变量缺失失败。

## 决定与理由

应用默认 provider 改为 OpenAI-compatible DeepSeek V4 Flash native tools：

```properties
agent.model.provider=openai-compatible
agent.model.base-url=https://api.deepseek.com
agent.model.name=deepseek-v4-flash
agent.model.tool-protocol=native-tools
agent.model.api-key-env=DEEPSEEK_API_KEY
```

Spring Boot 集成测试显式覆盖：

```properties
agent.model.provider=mock
```

这样普通本地启动会进入真实模型路径，符合当前 demo 阶段；测试仍使用 mock，不需要网络和密钥。

## 代价与限制

- 本地启动后创建任务会调用外部模型服务，必须确保 `DEEPSEEK_API_KEY` 已在 shell 环境中设置。
- 没有 key 时，后端仍可启动，但真实 run 会在调用模型时失败并提示缺少环境变量。
- 测试覆盖 mock provider，不代表每次 `mvn test` 都验证真实 DeepSeek 可用性；真实模型 flow 仍需单独自测和记录。

## 实现与验证证据

- 代码位置：
  - `backend/src/main/resources/application.properties`
  - `backend/src/test/java/com/zhumeiyuan/codingagent/CodingAgentBackendApplicationTests.java`
  - `backend/src/test/java/com/zhumeiyuan/codingagent/agent/api/RunControllerTests.java`
  - `backend/src/test/java/com/zhumeiyuan/codingagent/agent/api/WorkspaceControllerTests.java`
  - `backend/src/test/java/com/zhumeiyuan/codingagent/agent/tool/ToolRegistrySpringContextTests.java`
- 验证记录：待补。

## 何时重新考虑

- 项目需要面向没有模型 key 的第三方评审环境一键启动时，可考虑提供 `.env.example` 或明确 mock/dev profile。
- 增加模型 provider 选择 UI 时，可把默认 provider 变成用户可见设置。
