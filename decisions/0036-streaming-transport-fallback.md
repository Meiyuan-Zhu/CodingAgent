# ADR-0036：Streaming 传输失败降级

- 日期：2026-09-01
- 状态：accepted
- 决策依据/确认来源：用户要求装好本地编译环境后完整跑通流程，真实 run 中出现 `Model HTTP streaming request failed`
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0023、ADR-0033、ADR-0034

## 问题与约束

Provider token-level streaming 已作为 native tools 主路径启用。真实端到端 C++ 验证中，Agent 已完成写文件、编译失败 observation、修改代码和再次验证等多轮动作，但后续模型 streaming HTTP 连接出现 IO 层失败，run 被标记为 `MODEL_ERROR`。

这类传输错误和 provider 返回的 HTTP 400/429 不同。前者可能是一次 streaming 连接不稳定，非 streaming 请求仍可能成功；后者通常表示请求协议、额度或鉴权等问题，不应被静默掩盖。

## 备选方案

- 保持原状：任何 streaming HTTP 失败都直接结束 run。实现简单，但真实演示中抗抖动较弱。
- 对所有 streaming 失败都 fallback：能提高成功率，但可能掩盖 HTTP 400、鉴权失败、额度限制等真正需要暴露的问题。
- 只对 IO 传输异常 fallback：保留协议/额度/鉴权错误的可见性，同时给 transient streaming failure 一次恢复机会。

## 决定与理由

`OpenAiCompatibleModelClient.completeNativeToolStream` 在 native tools streaming 路径中增加窄范围 fallback：

- provider stream 为空时，继续沿用 ADR-0034 的非 streaming native retry。
- 若抛出的 `ModelClientException` 原因为 `IOException`，再用同一请求降级执行一次非 streaming native completion。
- HTTP 非 2xx、模型协议解析错误、非 IO 类 provider 错误仍按原逻辑抛出，并由 Runtime 标记为 infrastructure/model failure。

这样不改变 Agent loop、上下文和工具执行语义，只提高真实 provider streaming 主路径的恢复能力。

## 代价与限制

- fallback 后这一轮不再有 token-level delta，只会收到最终完整 assistant 消息。
- 如果 provider streaming 和非 streaming 都不可用，run 仍会失败。
- 本策略不修复本机 C++ linker/Xcode CommandLineTools 问题；那属于 workspace 命令 observation 或本地环境配置问题。

## 实现与验证证据

- 代码位置：[OpenAiCompatibleModelClient](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClient.java)。
- 测试位置：[OpenAiCompatibleModelClientTests](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClientTests.java)。
- 验证记录：[BUGFIX-003](../memory/VERIFICATION.md)、[REALFLOW-001](../memory/VERIFICATION.md)。
- 关联提交/运行：真实 run `2c110940-e4d7-4e42-b376-4ec0529046c3`；提交后补充。

## 何时重新考虑

如果真实 provider 经常出现 streaming IO 失败，需要增加有限次数退避、provider-specific 诊断事件或配置开关。若后续接入的模型不支持非 streaming native tools，应按 provider 能力禁用该 fallback。
