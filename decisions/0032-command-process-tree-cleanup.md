# ADR-0032：命令中断时清理进程树

- 日期：2026-08-30
- 状态：accepted
- 决策依据/确认来源：用户要求完善 Process timeout cleanup，避免只 `destroyForcibly(parent)` 导致 child/grandchild 残留。
- 实现状态：已实现并验证
- 取代/被取代：补充 ADR-0012、ADR-0016；解决 ADR-0016 中“当前只销毁直接子进程”的限制

## 问题与约束

`run_command` 由 Java `ProcessBuilder` 启动本地命令。Runner 的工具 timeout 会取消工具 Future，命令工具线程被中断后需要结束 OS 进程。

如果只对 parent process 调用 `destroyForcibly()`，由脚本或构建工具派生出的 child/grandchild 可能继续运行，造成演示后残留进程、占用端口或继续写文件。这会削弱本地 Agent 的安全性和可控性。

## 备选方案

1. 只销毁 parent process。实现简单，但已知可能残留后代进程。
2. 通过 shell/process group 管理全部命令。更适合 Unix，但会改变当前“不经 shell、argv 执行”的边界，并引入平台差异。
3. 保持 argv 执行，用 Java `ProcessHandle` 枚举 descendants，先销毁 descendants，再销毁 parent；若当前环境禁止进程枚举，则降级为 best-effort parent cleanup。

## 决定与理由

采用方案 3：

- `WorkspaceCommandTools` 在命令等待期间使用短间隔轮询，及时响应线程 interrupt。
- 发生 interrupt 时，先通过 `process.toHandle().descendants()` 收集进程树。
- 从叶子到根依次 `destroyForcibly()`，并给每个 handle 一个短暂退出等待窗口。
- 销毁 parent 后再次枚举 descendants 做 best-effort 补扫。
- 如果 sandbox 或 OS 权限禁止枚举 descendants，捕获该异常并至少销毁 parent，不让 cleanup 自己变成内部错误。

这保持了当前 `run_command` 的工具 API、审批策略和无 shell 执行边界，同时补上常见 timeout/cancel 残留进程风险。

## 代价与限制

- `ProcessHandle.descendants()` 依赖 OS 权限；受限 sandbox 中可能无法枚举，这时只能 best-effort 清理 parent。
- 这仍不是完整 OS 沙箱。进程树清理降低残留风险，但不限制命令本身能访问的主机资源。
- 极端情况下，进程可能 fork 后脱离父子关系；这类后台 daemon 化进程不能只靠 Java descendants 完全覆盖。
- 当前没有引入 shell process group 或容器隔离，避免扩大设计范围。

## 实现与验证证据

- 代码位置：[WorkspaceCommandTools.java](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceCommandTools.java)。
- 测试位置：[WorkspaceCommandToolsTests.java](../backend/src/test/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceCommandToolsTests.java)。
- 验证记录：[COMMAND-CLEANUP-001](../memory/VERIFICATION.md)。
- 关联提交/运行：尚未提交。

## 何时重新考虑

如果后续需要支持长时间后台任务、交互式命令、shell 组合命令或多用户部署，应重新设计命令隔离和进程组管理，而不是继续扩展当前本地 best-effort cleanup。
