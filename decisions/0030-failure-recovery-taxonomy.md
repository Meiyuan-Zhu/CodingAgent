# ADR-0030：Failure Recovery 错误分类与可恢复工具失败

- 日期：2026-08-30
- 状态：accepted
- 决策依据/确认来源：用户要求将子任务 4 Failure Recovery 作为 P0，明确区分 recoverable tool error、resource/policy termination 和 infrastructure failure。
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0027、ADR-0028、ADR-0029

## 问题与约束

一个 Coding Agent 不能假设第一次工具调用总是正确。文件名错误、路径错误、编辑文本未命中、命令非 0 退出、参数错误等都应成为下一轮 LLM 的 observation，让模型继续调整计划。

如果这些情况直接让 run 进入 `FAILED`，项目会更像带工具调用的聊天机器人，而不是真正的 iterative coding agent。

同时，资源限制和基础设施失败仍必须能终止 run，避免无限循环或隐藏系统问题。

## 备选方案

1. 所有工具失败直接终止 run。实现简单，但不符合 Agent “执行 → 观察 → 修正”的核心能力。
2. 所有异常都吞成 tool observation。恢复能力强，但会把模型 API、内部状态损坏或序列化 bug 伪装成可恢复任务错误。
3. 明确分三类：可恢复工具错误继续 loop；预算、取消和审批拒绝按策略终止；模型 provider 和 runtime 内部异常失败。

## 决定与理由

采用方案 3：

- Recoverable tool error：作为 `ToolResult.success=false` observation 回填给模型，继续下一轮。
- Resource / policy termination：`MAX_ROUNDS`、`MAX_TOOL_CALLS`、模型 length/token stop、用户取消、审批拒绝会结束 run。
- Infrastructure failure：模型 API/provider 错误、模型响应解析错误、runner 内部异常会结束 run。

工具失败 JSON 显式包含：

- `success=false`
- `failureKind=RECOVERABLE_TOOL_ERROR`
- `recoverable=true`
- `errorCode`
- `message`
- `recoveryHint`
- `metadata`

Workspace 错误进一步拆成更利于恢复的类型：`WORKSPACE_NOT_FOUND`、`WORKSPACE_INVALID_PATH`、`WORKSPACE_PERMISSION_DENIED`、`WORKSPACE_CONFLICT`、`WORKSPACE_EDIT_MISS` 和泛化 `WORKSPACE_ACCESS_DENIED`。

## 代价与限制

- 模型可能在 recoverable error 后继续失败；最终仍依赖 round/tool-call budget 截断。
- `recoveryHint` 是短提示，不是复杂 planner；真实模型是否完全遵循仍需要 demo 验证。
- `run_command` 的非 0 exit code 在命令 JSON 中表示 `success=false`，但外层工具执行仍可成功返回 observation；面试时需要解释这是“命令失败，不是工具通道崩溃”。
- 未实现对重复同类失败的 no-progress 自动检测；目前由 `MAX_ROUNDS` / `MAX_TOOL_CALLS` 兜底。

## 实现与验证证据

- 代码位置：[ToolExecutionErrorCode.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolExecutionErrorCode.java)、[ToolRegistry.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolRegistry.java)、[WorkspaceToolFactory.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/WorkspaceToolFactory.java)、[AgentRunner.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunner.java)。
- 测试位置：[AgentRunnerTests.java](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunnerTests.java)、[WorkspaceToolFactoryTests.java](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/tool/workspace/WorkspaceToolFactoryTests.java)、[ToolRegistryTests.java](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/tool/ToolRegistryTests.java)。
- 验证记录：[FAILURE-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚未提交。

## 何时重新考虑

如果后续发现模型在同一错误上重复循环，应增加 no-progress detector；如果接入多 workspace、多用户或更强权限命令，应把 policy termination 和用户确认策略继续细化。
