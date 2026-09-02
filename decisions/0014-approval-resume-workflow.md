# ADR-0014：审批后的 Agent 恢复执行工作流

- 日期：2026-08-28
- 状态：accepted
- 决策依据/确认来源：用户要求继续完成审批工作流；此前 ADR-0013 已建立可变更工具审批策略和 diff 展示，但明确记录 approve/resume API 尚未实现。
- 实现状态：已实现并验证。
- 取代/被取代：取代 [ADR-0013](0013-approval-policy-diff-display.md) 中“审批后不能恢复执行、只能安全拒绝”的阶段性限制；ADR-0013 的审批策略和 diff 展示仍作为本决策基础。

## 问题与约束

可变更工具已经能被审批策略拦截，但如果用户不能在页面上批准并恢复同一个 run，演示链路仍不完整：模型提出变更后只能失败，无法展示“人批准后执行工具并产出 diff”。

完整工作流需要满足：

- 后端必须继续作为可信审批边界，不能只在前端做按钮。
- 审批前不能执行可变更工具。
- Approve 后必须恢复同一个 run 的上下文，让工具结果继续回填到 Agent loop。
- Reject 后必须以明确结束原因停止 run，且不执行工具。
- 当前 runner 串行执行工具，因此先支持单个 pending approval，不提前设计复杂队列。

## 备选方案

1. Approve 时重新创建一个新 run。
   - 优点：实现简单。
   - 缺点：上下文、事件和 diff 分散到两个 run，面试解释会变混乱。
2. 把整个 runner 线程阻塞在审批点等待用户输入。
   - 优点：continuation 保存在调用栈中。
   - 缺点：长期占用线程，取消和超时难管理，也不利于后续持久化。
3. 将 pending approval 作为运行时 continuation 存储，runner 返回；Approve 后用保存的上下文启动恢复任务。
   - 优点：线程不阻塞，事件仍属于同一个 run，approve/reject/cancel 都能清楚建模。
   - 缺点：当前 continuation 仍是进程内存，后端重启后无法恢复。

## 决定与理由

采用方案 3。

新增 `PendingToolApproval` 表示一个挂起的工具调用，包含 run id、round、tool call、审批决策、当前模型消息上下文和已使用工具调用数。`AgentRunStore` 在 run 旁保存一个 pending approval 槽；这不是产品运行时“长期记忆”，而是当前进程内 run 的恢复点。

Runner 遇到需要审批的工具时：

1. 发出 `TOOL_CALL_REQUESTED`。
2. 保存 `PendingToolApproval`。
3. 将 run 转为 `WAITING_FOR_APPROVAL`。
4. 发出 `APPROVAL_REQUIRED`。
5. 返回，不占用 run executor 线程。

用户 Approve 时：

1. `POST /api/runs/{runId}/approvals/{toolCallId}/approve` 校验 run 正在等待审批并消费 pending approval。
2. run 转回 `RUNNING`。
3. 发出 `APPROVAL_RESOLVED approved=true`。
4. 通过 `RunTaskManager` 启动恢复任务，执行已批准工具。
5. 工具结果进入上下文，继续下一轮模型请求。

用户 Reject 时：

1. `POST /api/runs/{runId}/approvals/{toolCallId}/reject` 消费 pending approval。
2. 发出 `APPROVAL_RESOLVED approved=false`。
3. run 以 `FAILED / APPROVAL_REJECTED` 结束。
4. 不执行工具。

取消 run 时清理 pending approval。`RunTaskManager.start` 允许清理已完成但尚未从 active map 移除的旧 task，降低用户快速点击 Approve 时的竞态风险。

前端根据 `APPROVAL_REQUIRED` 和 `APPROVAL_RESOLVED` 计算当前 pending approval，在任务线程中显示工具名、参数、审批原因，以及 Approve and run / Reject 按钮。Approve 后继续使用同一条 SSE 流接收后续 `TOOL_CALL_STARTED`、`TOOL_CALL_FINISHED`、`RUN_FINISHED` 事件，Diff 面板展示工具结果中的 `unifiedDiff`。

## 代价与限制

- 当前 pending approval 只保存在进程内存，后端重启会丢失等待审批的 continuation。
- 当前只支持单个 pending approval 槽；这符合当前串行工具执行模型。若未来支持并行工具调用或批量审批，需要扩展为队列或按 toolCallId 的 map。
- Approve 后执行的仍是 mock 模型选择的工具；这不证明真实模型 API 已接入。
- 前端按钮只是触发后端审批 API，安全边界仍以后端为准。

## 实现与验证证据

- 代码位置：
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/PendingToolApproval.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunStore.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunService.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/AgentRunner.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/api/RunController.java`
  - `frontend/src/App.vue`
  - `frontend/src/api/runs.ts`
- 验证记录：[APPROVAL-001](../memory/VERIFICATION.md#approval-001完整审批恢复工作流验证)。
- 关联提交/运行：`736aa7e feat: add approval resume workflow`；HTTP 验证 run 为 `fd830860-d275-4484-8fbc-249a51142722`。

## 何时重新考虑

- 需要持久化 run 历史或后端重启后恢复等待审批任务时，应将 continuation 存储从内存迁移到持久层。
- 需要并行工具调用、批量审批或多工具一次 approve 时，应把单槽 pending approval 改为可索引的 pending approval 集合。
- 引入命令工具后，需要补充命令级风险分类、命令 diff/输出展示和进程树取消策略。
