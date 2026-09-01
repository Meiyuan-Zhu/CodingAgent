# ADR-0034：命令参数归一化与空 Stream 降级

- 日期：2026-09-01
- 状态：accepted
- 决策依据/确认来源：用户截图反馈真实模型 run 在命令验证阶段 `model_error`
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0016、ADR-0023、ADR-0033

## 问题与约束

真实模型在一次 C++ 任务中把 `run_command.command` 输出为 JSON 字符串形式的 argv 数组：

```json
{"command":"[\"which\", \"g++\"]","cwd":"."}
```

后端原本只接受真实 JSON array，因此该参数会被判为 `INVALID_ARGUMENTS`。同时，provider streaming 可能只返回空内容或 `[DONE]`，导致 run 被标记为 `model_error`，用户界面上表现为任务停止但没有足够清晰的恢复路径。

命令工具仍必须坚持 argv 数组语义，不能为了容错接受 shell command 字符串，例如 `which g++`，否则会削弱命令执行的安全边界。

## 决定与理由

`run_command` 对 `command` 做窄范围归一化：

- 接受正常 JSON array。
- 若收到字符串，只在字符串内容本身是 JSON string array 时解析为 argv。
- 继续拒绝普通 shell command 字符串和非字符串数组项。

OpenAI-compatible native tools streaming 路径在 provider stream 没有产生 assistant content 或 tool call 时，降级重试一次同请求的非 streaming native completion。这样保留 token-level streaming 的主路径，同时避免空 stream 直接让 run 进入 `model_error`。

前端工具卡同步解析历史事件中的 JSON-string argv array，使审查面板展示 `which g++`，而不是裸 JSON payload。

## 代价与限制

- 容错只覆盖 JSON-string argv array，不覆盖 shell 语法，也不帮模型拆分命令。
- 空 stream fallback 的那一次响应不再是 token-level streaming。
- 本地 `g++` 因 Xcode license 未同意返回 exit code 69，这是环境问题，不属于 Agent Runtime bug。
- 如果 provider 持续不可用或非 streaming 响应也异常，run 仍会按 infrastructure failure 失败。

## 实现与验证证据

- 代码位置：[WorkspaceToolFactory](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/WorkspaceToolFactory.java)、[ToolArgumentReader](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolArgumentReader.java)、[OpenAiCompatibleModelClient](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClient.java)、[frontend tool cards](../frontend/src/run/toolCards.ts)。
- 测试位置：[WorkspaceToolFactoryTests](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/tool/workspace/WorkspaceToolFactoryTests.java)、[OpenAiCompatibleModelClientTests](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/model/OpenAiCompatibleModelClientTests.java)。
- 验证记录：[BUGFIX-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚无，提交后补充。

## 何时重新考虑

如果后续要支持更复杂的命令 intent 修复，应交给模型通过 tool observation 自行改正，而不是在工具层引入 shell parser。若 provider 空 stream 频繁出现，需要增加 provider-specific 日志和重试退避策略。
