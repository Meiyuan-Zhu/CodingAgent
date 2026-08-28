# ADR-0016：Workspace 命令执行工具

- 日期：2026-08-28
- 状态：accepted
- 决策依据/确认来源：用户要求继续实现命令执行工具；此前已接受可变更工具需审批，且 `run_command` 已纳入审批策略
- 实现状态：已实现并通过 mock HTTP 审批闭环验证；真实模型命令 run 尚未验证
- 取代/被取代：无

## 问题与约束

项目需要让 Agent 能在本地 workspace 内执行测试、构建或简单命令，并把 stdout、stderr 和退出码作为工具观察回填给模型。这个能力风险高：命令可能修改文件、读取本机环境、长时间运行或产生大量输出，因此不能像只读文件工具一样自动执行。

已确定边界是前端不直接执行本地工具，命令只能由后端工具注册表触发；项目也不能依赖模型服务端托管的代码执行能力。命令执行边界必须便于测试、可解释，并和已有取消、超时、审批流程兼容。

## 备选方案

1. 让前端传 shell 字符串，由后端通过 `/bin/sh -c` 执行。
   - 优点：用户和模型写命令最方便，支持管道、重定向和 shell 内建语法。
   - 代价：注入风险和解释成本高；命令字符串更难做参数级校验；模型生成的复杂 shell 行为不利于追踪和审批。

2. 只允许固定白名单命令，例如 `mvn test`、`npm run build`。
   - 优点：安全面更小，演示时稳定。
   - 代价：工具扩展性弱；后续要支持更多真实项目任务时会频繁改代码；Agent 很难处理一般性调试和检查任务。

3. 使用 argv 数组执行本地命令，并通过审批、workspace cwd、最小环境变量和输出限制控制风险。
   - 优点：避免 shell 解释和命令注入；命令参数结构化，便于展示和审批；可以支持 Maven、npm、grep 等常见工具；实现和测试不依赖第三方 Agent 框架。
   - 代价：不支持管道、重定向、通配符等 shell 语法；复杂命令需要后续拆成多个工具调用或显式引入受控 shell 策略。

## 决定与理由

采用方案 3：新增 `run_command` 工具，参数为：

- `command`：必填字符串数组，直接传给 Java `ProcessBuilder`，不经过 shell。
- `cwd`：可选，相对 workspace 的工作目录；必须解析到 workspace 内已存在目录。
- `max_output_chars`：可选，限制 stdout/stderr 各自返回长度；后端设置上限。

`run_command` 仍走统一工具注册表和 Agent loop，并被 `ToolApprovalPolicy` 标记为需要用户审批。模型可以提出命令，但只有用户批准后才会执行。命令执行结果中的非零退出码是正常观察数据，工具结果仍视为成功返回；只有参数错误、workspace 越界、启动失败、超时或中断才属于工具执行失败。

命令进程使用最小环境变量：清空继承环境后只传 `PATH`、`LANG`、可选 `LC_ALL`、`CI=true` 和 `PWD`。第一版不允许模型提供自定义环境变量，避免误把 API key、HOME 目录配置或开发机隐私暴露给命令。

stdout 和 stderr 分开捕获，均做长度截断并返回 `stdoutTruncated` / `stderrTruncated`。命令超时继续使用已有 runner 工具 timeout；命令线程被中断时会 `destroyForcibly()` 当前进程。

## 代价与限制

- 这不是操作系统级沙箱。workspace cwd 限制只约束工作目录，不阻止命令通过绝对路径访问主机上进程权限可读的其他文件。因此必须保留审批，并在演示和文档中避免把它描述成强沙箱。
- 当前只销毁直接子进程，不保证完整进程树清理；复杂命令或脚本派生的孙进程需要后续补充 process-tree 管理。
- 清空 `HOME`、自定义环境变量和大部分 inherited env 会提升安全性，但可能导致依赖用户配置的命令失败，例如依赖 Maven settings、npm 私有 registry 或 shell profile 的命令。
- 不支持 shell 管道、重定向、通配符和内建命令。需要此类能力时应新增单独 ADR，设计受控 shell 策略和更强审批展示。

## 实现与验证证据

- 代码位置：
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/WorkspaceCommandTools.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/CommandExecutionResult.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/WorkspaceToolFactory.java`
  - `backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/ToolApprovalPolicy.java`
- 验证记录：[COMMAND-001](../memory/VERIFICATION.md#command-001workspace-命令执行工具验证)
- 关联提交/运行：本阶段提交后补充；HTTP run `88cb4eef-61a2-4acc-92af-a0d13eda0d19`。

## 何时重新考虑

- 需要支持 shell 组合命令、交互式命令、长时间后台进程或完整 process tree 终止时。
- 需要让命令访问受控凭据、私有依赖仓库或特定构建环境变量时。
- 项目从本地单用户演示扩展到多用户或公网部署时，应重新设计隔离模型，而不是沿用当前本地进程执行方案。
