# ADR-0029：上下文窗口按工具调用配对裁剪

- 日期：2026-08-30
- 状态：accepted
- 决策依据/确认来源：用户要求完善 Context Management，重点修复 context trimming 可能从中间截断 assistant → tool pair 的风险。
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0011、ADR-0019、ADR-0023

## 问题与约束

Agent loop 会把模型工具动作和工具执行结果都加入 `List<ModelMessage>`。在 OpenAI-compatible native tool calling 协议下，`role=tool` 消息必须能对应前一条 assistant 的 `tool_calls`，否则 provider 可能拒绝请求，或者模型在下一轮看不到“为什么出现这个 observation”。

此前上下文窗口是“保留 system + 最近消息”的简单尾部裁剪。若裁剪边界刚好落在 assistant tool call 和后续 tool result 中间，就可能留下孤立的 tool result，破坏上下文结构。

## 备选方案

1. 暂停上下文裁剪，始终发送完整历史。实现最简单，但长任务更容易超 provider 上下文限制。
2. 按固定消息数量尾部裁剪。已有方案简单，但可能拆散 assistant/tool 配对。
3. 在现有消息数量预算内做配对感知裁剪：保留 system 和初始 user，再从尾部按消息组纳入最近上下文。

## 决定与理由

采用方案 3：

- 第一条 system prompt 保留。
- 初始 user task 保留，避免长任务裁剪后丢失原始目标。
- 最近消息从尾部按组加入窗口。
- 普通 user/assistant 消息作为单消息组。
- 一个 assistant tool_calls 加上其后连续的 tool result 作为不可拆分消息组。
- 发现孤立 tool result 时跳过，不把不完整 pair 发送给模型。
- 如果某个完整组放不进 `maxContextMessages`，停止纳入更早历史。

这样不改变现有 Agent Runtime 架构，也不引入复杂 token 估算，但能保证 provider-facing context 至少保持 tool-call/tool-result 结构合法。

## 代价与限制

- 预算仍按消息数计算，不是真实 token 数；超长文件内容或命令输出仍可能使请求过大。
- 当完整工具组本身过大、放不进窗口时，runner 会保留更少近期信息，而不是压缩 tool result。
- 多个工具调用同轮返回时，会把 assistant tool_calls 和连续 tool results 作为一个整体；这符合当前 transcript 结构，但不是更细粒度的 token 优化。
- 暂未实现 provider-specific context compression 或摘要 memory。

## 实现与验证证据

- 代码位置：[MockAgentRunner.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java)。
- 测试位置：[MockAgentRunnerTests.java](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunnerTests.java)。
- 验证记录：[CONTEXT-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚未提交。

## 何时重新考虑

如果后续接入 token 计数、长文件摘要、provider-specific 压缩策略或多 conversation 分段存储，需要把当前消息数组窗口升级为 token-aware context builder，但仍应保持 assistant/tool pair 不被拆散。
