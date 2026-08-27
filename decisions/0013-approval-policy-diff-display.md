# ADR-0013：审批策略与 diff/变更展示

- 日期：2026-08-27
- 状态：accepted
- 决策依据/确认来源：用户要求继续实现“审批策略和 diff/变更展示”，且此前已确认前端不持有密钥、不直接执行本地工具，后端负责本地 workspace 操作。
- 实现状态：已实现并验证第一阶段拦截式审批、写入/替换 diff 元数据和前端 diff 展示。
- 取代/被取代：无

## 问题与约束

`write_file`、`replace_text` 和未来的 `run_command` 都可能改变用户 workspace。Agent 不能在没有用户可见依据的情况下自动执行可变更操作；同时，录屏和面试需要能说清楚“模型想做什么、系统为什么拦截、实际有没有改文件”。

当前阶段还没有完整的前端 approve/reject/resume 流程，因此需要先建立安全默认策略：可变更工具必须审批；在审批恢复 API 未实现前，runner 不执行工具，并以明确事件和结束原因记录这个边界。

## 备选方案

1. 所有工具默认自动执行。
   - 优点：demo 看起来推进更快。
   - 缺点：模型一旦选择写入或命令工具就会直接改 workspace，不利于安全、追踪和面试解释。
2. 只在前端提示，但后端仍执行。
   - 优点：UI 变化少。
   - 缺点：审批不是可信边界，刷新、绕过前端或 API 调用都可能执行变更。
3. 后端建立审批策略，可变更工具先阻断；写入/替换工具返回 unified diff，前端从工具结果展示变更。
   - 优点：安全边界在后端，事件链可追踪，后续可自然扩展 approve/reject/resume。
   - 缺点：当前阶段可变更工具会被安全拒绝，真正“审批后执行”需要后续补 API 和 UI。

## 决定与理由

采用方案 3。

后端新增 `ToolApprovalPolicy`，按工具名判断审批模式：

- `write_file`：需要用户审批。
- `replace_text`：需要用户审批。
- `run_command`：预留为需要用户审批，即使命令工具尚未注册。
- 其他工具：自动通过审批策略，继续交给工具注册表做存在性和参数校验。

`MockAgentRunner` 在执行工具前先产出 `TOOL_CALL_REQUESTED`，其中包含审批模式和原因。若工具需要审批，则进入 `WAITING_FOR_APPROVAL`，发出 `APPROVAL_REQUIRED`；由于完整 approve/resume endpoint 尚未实现，当前阶段立即发出 `APPROVAL_RESOLVED`（`approved=false`，原因是 `approval_resume_api_not_implemented`），随后以 `APPROVAL_REJECTED` 结束 run，且不执行工具。

`WorkspaceWriteTools` 在 `write_file` 与 `replace_text` 成功时返回 `unifiedDiff` 字段；前端从 `TOOL_CALL_FINISHED` 的 JSON content 中提取 `unifiedDiff` 并在 Diff 面板展示。这使未来审批后执行和历史回看能复用同一个展示入口。

## 代价与限制

- 当前审批是安全拦截，不是完整的人机审批工作流；用户还不能在前端点击 Approve 后恢复同一个 run。
- Diff 当前是简化版 unified diff，偏向可读和可演示，不做最短编辑距离算法；大文件 diff 会截断。
- 因为可变更工具当前会被审批策略拦截，正常 run 不会产生真实写入 diff；diff 元数据已由工具单元测试验证，UI 由 TypeScript 构建验证。
- 未知工具不在审批策略中拦截，而是进入注册表并由注册表返回 `UNKNOWN_TOOL`。这避免审批层承担工具存在性判断。

## 实现与验证证据

- 代码位置：
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolApprovalPolicy.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceUnifiedDiff.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceWriteTools.java`
  - `frontend/src/App.vue`
  - `frontend/src/style.css`
- 验证记录：[CHANGE-001](../memory/VERIFICATION.md#change-001审批策略与-diff变更展示验证)。
- 关联提交/运行：`afe23a3 feat: add approval policy and diff display`；HTTP 验证 run 为 `68bb2c0d-b17f-4d5a-8219-d725451f1f68`。

## 何时重新考虑

- 实现完整 approve/reject/resume API 后，需要把“审批后恢复执行”的状态机和事件语义补充为新 ADR 或更新本 ADR。
- 引入命令工具后，需要进一步细分命令审批策略，例如危险命令拒绝、只读命令可审批、超时和进程树清理策略。
- 如果 diff 展示需要支持二进制文件、重命名、删除、多文件 patch 或超大文件流式展示，应重新评估 diff 生成方式。
