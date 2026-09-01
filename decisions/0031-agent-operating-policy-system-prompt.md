# ADR-0031：Agent Operating Policy System Prompt

- 日期：2026-08-30
- 状态：accepted
- 决策依据/确认来源：用户要求完善子任务 5 System Prompt，将 prompt 设计为简短 operating policy，而不是长篇编程教程。
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0010、ADR-0019、ADR-0027、ADR-0030

## 问题与约束

Agent Runtime、tools、context 和 failure recovery 已经具备基础闭环，但 system prompt 仍偏临时，只说明“本地 coding agent”和 JSON 格式。真实模型需要更明确的 operating policy：

- 它只能通过工具访问文件和命令结果。
- 修改前应先检查相关文件。
- 编辑要小而聚焦。
- 修改后应尽量验证。
- 工具错误和命令失败是 observation，应分析后恢复。
- 不能反复重试同一失败动作。
- 不能在未合理验证时声称完成。

同时，prompt 不能膨胀成几千字；过长会消耗上下文，也会把清晰行为边界淹没。

## 备选方案

1. 保持短 prompt，只依赖模型常识。改动最小，但容易出现 hallucinated file access、未验证即完成、失败后重复尝试。
2. 写长篇教程式 prompt。覆盖多，但消耗 context，也难维护。
3. 将 runner system prompt 定义为简短 operating policy；provider adapter 只负责 JSON/native tool calling 协议说明。

## 决定与理由

采用方案 3：

- `MockAgentRunner.SYSTEM_PROMPT` 描述 agent role、workspace awareness、核心 workflow、failure recovery、探索约束和最终回答要求。
- OpenAI-compatible adapter 继续只补 provider 输出协议：JSON schema 或 native tool calling 使用规则。
- 用测试锁定 system prompt 中最关键的行为约束，避免后续无意删掉。

这种分层让“如何做事”和“如何按 provider 协议输出”分开，便于面试解释，也更容易维护。

## 代价与限制

- Prompt 只能影响模型倾向，不能代替 runtime guardrail；安全、预算、审批和工具校验仍由代码执行。
- 当前没有 prompt A/B 实验或真实模型专项回归，只通过单元测试确认 prompt 被注入且包含关键 operating policy。
- 不同模型对同一 prompt 的遵循程度不同，真实 demo 前仍需用选定模型跑一次端到端验证。

## 实现与验证证据

- 代码位置：[MockAgentRunner.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java)、[OpenAiCompatibleModelClient.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClient.java)。
- 测试位置：[MockAgentRunnerTests.java](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunnerTests.java)。
- 验证记录：[PROMPT-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚未提交。

## 何时重新考虑

如果真实模型 demo 出现过度探索、跳过验证、重复失败动作或输出格式冲突，应优先微调 prompt 的对应短句，而不是扩写成大段教程。
