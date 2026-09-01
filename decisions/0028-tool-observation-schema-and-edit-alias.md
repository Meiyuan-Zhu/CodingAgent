# ADR-0028：工具 Observation 结构化与 edit_file 别名

- 日期：2026-08-30
- 状态：accepted
- 决策依据/确认来源：用户要求完善 Tool System，仅补 `edit_file` 语义统一和结构化工具返回，保持现有工具集合与架构不扩张。
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0007、ADR-0009、ADR-0016、ADR-0027

## 问题与约束

当前工具集合已经覆盖 Coding Agent 的 Observe → Act → Verify：

- Observe：`list_files`、`read_file`、`search_text`
- Act：`write_file`、`replace_text`
- Verify：`run_command`

但题目或评审可能使用 `edit_file` 这个泛化名称检查编辑能力；同时，工具 observation 是下一轮 LLM 推理的输入，若成功/失败和错误类型只靠自然语言文本表达，模型更难恢复。

## 备选方案

1. 只在 README 中说明 `replace_text` 是轻量 edit-file 工具。最少改动，但工具 schema 中没有 `edit_file`。
2. 把 `replace_text` 重命名为 `edit_file`。语义清楚，但会破坏已有真实 demo、测试和文档中的工具名。
3. 增加 `edit_file` 作为别名，内部复用 `replace_text`，并统一工具返回 JSON 字段。

## 决定与理由

采用方案 3：

- 新增 `edit_file` 工具，参数与 `replace_text` 相同，内部调用同一套 exact text replacement 实现。
- `edit_file` 与 `replace_text` 一样需要用户审批，并支持 diff 展示和撤销快照。
- 成功工具结果 JSON 显式包含 `success=true` 和 `message`。
- 命令 observation JSON 包含 `success`、`message`、`exitCode`、`stdout`、`stderr`、`stdoutTruncated`、`stderrTruncated`、`timedOut` 和 `durationMillis`。
- 工具执行失败由 `ToolRegistry` 包装为 JSON，包含 `success=false`、`message`、`toolName`、`errorCode`、`timedOut` 和 `metadata`。

这样既保留已验证的 `replace_text` 能力，又让对外工具语义更接近通用 Coding Agent。

## 代价与限制

- `edit_file` 和 `replace_text` 会同时暴露给模型，模型可能任选其一；这不会影响执行语义，但会让事件里出现两种等价编辑工具名。
- `run_command` 的 JSON `success=false` 表示命令退出码非 0；外层 `ToolResult.success=true` 仍表示工具本身成功执行并返回 observation。这两层语义需要在面试中解释清楚。
- 当前失败 JSON 是在 registry 层统一包装，少数测试替身或直接构造的 `ToolResult.failure` 不自动获得该结构。

## 实现与验证证据

- 代码位置：[WorkspaceToolFactory.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/WorkspaceToolFactory.java)、[ToolRegistry.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolRegistry.java)、[WorkspaceCommandTools.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceCommandTools.java)。
- 前端位置：[display.ts](../frontend/src/run/display.ts)、[timeline.ts](../frontend/src/run/timeline.ts)。
- 验证记录：[TOOL-OBS-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚未提交。

## 何时重新考虑

如果后续引入 patch-based editing 或 AST-aware editing，可让 `edit_file` 成为更高层编辑工具，而将 `replace_text` 保留为底层精确替换能力。
