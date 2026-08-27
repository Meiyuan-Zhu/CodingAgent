# 开发日志

按实际阶段追加。记录发生了什么、原因和证据，不复制完整聊天，不补造时间、代码或提交。当前进度看 [STATUS.md](STATUS.md)。

## 2026-08-27：按用户要求整理决策和开发记录

- 请求：保证可维护性与决策可追踪性；决策放 decisions/，开发文档及后续实现情况放 memory/。
- 将已有项目方案迁入 memory/PROJECT_PLAN.md，将已有技术选型 ADR 迁入 decisions/；保留此前真实方案和选择理由。
- 建立决策索引、模板和 ADR-0002，明确决策接受与功能实现是不同状态。
- 建立 memory 的状态、日志、验证记录和入口；建立根目录 AGENTS.md，规定代码边界与阶段更新流程。
- 添加基础 .gitignore，避免常见凭据文件、构建产物和本地运行数据误入库；忽略规则不能替代提交前的凭据检查。
- 检查文档链接、旧目录引用、Markdown 围栏和状态边界；结果见 [DOC-001](VERIFICATION.md)。
- 本阶段没有新增业务代码、初始化 Git、安装依赖、调用模型或执行应用测试。
- 当前没有关联提交；后续有真实提交后再关联，不伪造哈希。

此前讨论内容保存在迁移后的方案与 ADR-0001 中，本条不是对早期过程补造逐次提交记录。

## 2026-08-27：Git 初始化与框架骨架

- 初始化本地 Git 仓库，默认分支调整为 `main`。
- 创建首个提交 `ced7616 docs: establish project planning records`，保存需求、决策、状态和开发约定基线；题目 PDF 和 `tmp/` 未入库。
- 使用 Vite 创建 `frontend/`，选择 Vue 3 + TypeScript 模板并安装依赖。
- 使用 Spring Initializr 创建 `backend/` Maven 项目，随后根据 Maven Central 可解析版本调整为 Spring Boot 3.5.16 + Java 21。
- 保留依赖最小集：后端使用 Spring Web、Validation、Starter Test；前端使用 Vite 模板生成的 Vue/TypeScript 依赖。
- 新增 `GET /api/health` 健康接口，前端通过 Vite proxy 请求 `/api/health` 并显示后端状态。
- 清理默认 Vite 示例页面与未引用模板素材。
- 新增根目录 `README.md`，记录前后端运行和检查命令。
- 新增 ADR-0003 记录框架基线与版本选择。
- 执行 APP-001 骨架验证：后端 `mvn test`、前端 `npm run build` 均通过。
- 创建框架提交 `3f220c9 chore: scaffold vue and spring boot apps`。
- 已知环境现象：本机 Maven settings 有一个 `repositories` 标签位置警告，但未阻止构建；第一次尝试 Boot `4.1.1.RELEASE` 未能通过本机 Maven 镜像解析，因此未采用。

## 2026-08-27：确认本地工作台式 Web 形态

- 用户提出 Web 界面希望做成类似 Codex 的简洁、清晰、大气风格，并确认不应走上传文件式工作流。
- 新增 ADR-0004，记录产品形态：前端是本地 workspace 控制台，后端负责文件、命令、模型和运行记录。
- 更新 `frontend/src/App.vue`：左侧运行列表、中间任务线程与事件流、底部任务输入区、右侧文件/Diff/检查结果标签。
- 更新 `frontend/src/style.css`：使用更克制的工程工具布局，减少展示页感。
- `Run` 按钮暂时禁用，避免在任务接口和 Agent Core 未实现前制造假能力。
- 执行 UI-001 验证：`npm run build` 通过。
- 创建 UI 提交 `8fc625b feat: shape codex-like workbench shell`。

## 2026-08-27：子任务 1 - Agent 运行协议领域模型

- 目标：先建立后端核心运行协议，不接真实模型、不接工具执行、不接 Controller/SSE。
- 新增 `backend/src/main/java/com/zhumeiyuan/codingagent/agent/run/`：
  - `RunId`、`RunStatus`、`StopReason`、`RunEventType`、`RunEvent`。
  - `ToolCall`、`ToolResult`。
  - `AgentRun`、`RunEventEnvelope`。
- 运行协议使用显式状态和结束原因，事件使用递增序号，工具参数和事件 payload 在构造时复制为不可变快照。
- 新增 ADR-0005 记录为什么先做领域模型，以及为什么暂时不把它绑定到 Controller、SSE 或数据库。
- 新增后端单元测试覆盖正常状态转换、审批状态、非法转换、终态约束、事件序号、Map 不可变快照和工具结果。
- 执行 CORE-001 验证：`cd backend && mvn test` 通过，12 tests, 0 failures, 0 errors。
- 创建子任务提交 `e0369eb feat: add agent run protocol domain`。
- 限制：尚未实现 workspace、工具注册表、模型适配器、Agent loop、SSE 或运行持久化。

## 2026-08-27：子任务 2 - Workspace 边界与只读文件工具

- 目标：建立后端本地 workspace 边界，让文件工具只能访问配置的开发 workspace。
- 新增 `backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/`：
  - `WorkspacePathResolver` 统一处理相对路径、normalize、realpath 和 symlink 逃逸校验。
  - `WorkspaceAccessException` 与 `WorkspaceAccessCode` 明确拒绝原因。
  - `WorkspaceReadTools` 提供 `listFiles`、`readFile`、`searchText`。
  - DTO/结果类型：`FileListing`、`ListedWorkspaceFile`、`ReadFileResult`、`SearchResult`、`SearchMatch`。
- 后端配置 `agent.workspace.root=../workspaces/demo`，并新增安全 demo workspace 文件。
- 调整根 `.gitignore`：继续默认忽略 `workspaces/` 下私有内容，但允许提交 `workspaces/demo/`；忽略 IDE 自动生成的 `.vscode/`。
- 测试覆盖绝对路径拒绝、`..` 逃逸拒绝、`.env` 拒绝、symlink 逃逸拒绝、目录读取拒绝、非法 UTF-8 拒绝、搜索跳过敏感文件和结果截断。
- 首轮测试暴露 macOS 临时目录 `/var` 与 `/private/var` realpath 差异，已改为对 workspace root 使用真实路径再比较。
- 执行 WORKSPACE-001 验证：`cd backend && mvn test` 通过，21 tests, 0 failures, 0 errors。
- 限制：只读工具尚未暴露给 HTTP、SSE、模型工具注册表或真实 Agent loop；写入、编辑和命令工具未实现。

## 2026-08-27：子任务 3 - 工具注册表

- 目标：建立后端模型工具调用边界，把已有只读 workspace 工具注册为统一的工具定义和执行入口。
- 新增 `backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/`：
  - `ToolDefinition`、`RegisteredTool`、`ToolHandler`、`ToolExecutionResult`。
  - `ToolArgumentReader` 对模型传入参数做运行时校验。
  - `ToolRegistry` 暴露工具定义并执行 `ToolCall`，返回统一 `ToolResult`。
  - `ToolExecutionErrorCode` 和 `ToolExecutionException` 归一化未知工具、参数错误、workspace 拒绝和运行时错误。
- 新增 `backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/`，将 `WorkspaceReadTools` 适配为 `list_files`、`read_file`、`search_text`。
- 保持注册表与模型提供商无关，后续再由模型适配器把 `ToolDefinition` 翻译成具体 API schema。
- 执行 TOOLREG-001 验证：`cd backend && mvn test` 通过，39 tests, 0 failures, 0 errors。
- 限制：注册表尚未接入 HTTP、SSE、真实模型循环；当前只包含只读 workspace 工具。

## 2026-08-27：子任务 4 - Run API、SSE 与 mock runner

- 目标：打通第一个可演示的前后端任务闭环，但明确保持 mock 模型，不接真实 API 密钥。
- 新增 `backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/`：
  - `AgentRunStore` 保存进程内 run 状态和有序事件。
  - `AgentRunService` 校验 prompt、创建 run、发初始事件并启动 runner。
  - `MockAgentRunner` 模拟模型选择，只通过 `ToolRegistry` 执行一个只读 workspace 工具。
  - `RunEventStream` 使用 `SseEmitter` 做事件 replay 和实时发布。
- 新增 `backend/src/main/java/com/zhumeiyuan/codingagent/agent/api/`：
  - `POST /api/runs` 创建 run。
  - `GET /api/runs/{runId}` 查询状态。
  - `GET /api/runs/{runId}/events` 回看事件。
  - `GET /api/runs/{runId}/events/stream` 订阅 SSE。
- 更新 Vue 工作台：Run 按钮现在会创建真实后端 run，通过 EventSource 展示事件 timeline，并从 `list_files` 结果更新右侧文件列表。
- 联调发现 `WorkspaceProperties` 使用 `Path` 接收 `../workspaces/demo` 时，真实 `spring-boot:run` 会触发 Spring Boot 资源路径转换失败；改为用 `String` 接收配置，再由项目代码转换为 `Path`。
- 执行 RUNAPI-001 验证：`cd backend && mvn test` 通过，48 tests, 0 failures, 0 errors；本地 HTTP 创建 run 成功，最终 `SUCCEEDED`，事件回看 10 条，SSE replay 包含 `event:run_finished`。
- 执行 UI-002 验证：`cd frontend && npm run build` 通过；in-app browser 打开前端、点击 Run、显示 10 条事件、控制台无 warning/error、无横向溢出。
- 限制：当前 run 存储为进程内内存，重启丢失；mock runner 不是模型能力；没有写入、编辑、命令、取消或审批能力。

## 2026-08-27：子任务 5 - Workspace 写入与文本编辑工具

- 目标：在已有 workspace 安全边界上加入可变更文件的工具能力，但不把写入动作直接开放成前端裸 API。
- 扩展 `WorkspacePathResolver`，新增 `resolveForWrite`：允许目标文件不存在，但要求父目录存在并经过 realpath 校验，防止路径逃逸和 symlink 逃逸。
- 新增 `WorkspaceWriteTools`：
  - `writeFile` 创建或覆盖完整 UTF-8 文本文件。
  - `replaceText` 对已有 UTF-8 文件做精确文本替换。
  - 两者都限制写入大小，拒绝敏感路径和非法 UTF-8，并返回 SHA-256 与变更摘要。
- 扩展工具注册表适配：新增 `write_file`、`replace_text`，并增加 boolean 参数与可为空文本参数校验。
- 写入错误在工具结果中区分为 workspace access denied、workspace conflict、workspace edit miss，便于后续 UI/Agent loop 判断。
- 保持 mock runner 默认不写文件，避免用户反复点击 Run 时污染 Git 工作区。
- 执行 WRITE-001 验证：`cd backend && mvn test` 通过，64 tests, 0 failures, 0 errors。
- 限制：尚未实现 diff 渲染、用户审批、取消、真实模型调用或命令工具。


## 2026-08-27：子任务 6 - 模型适配边界与响应解析器

- 目标：把 mock runner 从直接“假装选择工具”改为经过模型客户端边界和响应解析器，为后续真实模型 API 接入留出可测试缝隙。
- 新增 `backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/`：
  - `ModelClient` 作为 runner 依赖的模型调用边界。
  - `ModelRequest`、`ModelMessage`、`ModelRole`、`ModelResponse`、`ModelFinishReason` 表达 provider-neutral 的内部协议。
  - `ModelResponseParser` 将模型原始 JSON 文本解析为内部响应，并校验 finish reason、工具调用数组、参数对象、重复 call id 和消息长度。
  - `HeuristicMockModelClient` 继续提供可重复 demo，但现在先生成原始 JSON，再走解析器。
- 更新 `MockAgentRunner`：通过 `ModelClient` 获取响应；模型解析错误以 `MODEL_PARSE_ERROR` 结束 run；工具执行失败仍以 `TOOL_ERROR` 结束。
- 增加模型解析器、模型请求、mock 模型客户端和 runner 解析失败测试。
- 执行 MODEL-001 验证：`cd backend && mvn test` 通过，78 tests, 0 failures, 0 errors。
- 限制：仍未接入真实模型 API；runner 仍是单轮请求，不包含上下文裁剪、预算、取消或多轮 observe-think-act 循环。


## 2026-08-27：子任务 7 - 多轮 Agent loop 与运行预算

- 目标：在接入真实模型前先把核心循环和预算控制做出来，避免真实 provider 行为与 loop bug 混在一起。
- 新增 `RunBudget`，默认限制为 4 轮、12 次工具调用、30 条上下文消息。
- 更新 `MockAgentRunner`：
  - 每轮通过 `ModelClient` 请求模型，并在事件中记录 round、contextMessages 和 toolCallsUsed。
  - `TOOL_CALLS` 响应先检查工具调用预算，再通过 `ToolRegistry` 执行工具。
  - 工具结果作为 tool observation message 追加到上下文，进入下一轮模型请求。
  - `STOP` 正常完成，`LENGTH` 转为 `TOKEN_BUDGET_LIMIT`，超轮次转为 `ROUND_LIMIT`，超工具调用转为 `TOOL_CALL_LIMIT`。
  - 上下文超过消息窗口时保留 system prompt 和最近消息。
- 更新 `HeuristicMockModelClient`：收到 tool message 后返回 `STOP`，因此 mock run 现在能演示两轮 observe/tool/finish 流程。
- 执行 LOOP-001 验证：`cd backend && mvn test` 通过，85 tests, 0 failures, 0 errors；`cd frontend && npm run build` 通过；本地 HTTP run 产生 11 条事件、2 次模型请求、1 次工具完成并以 `COMPLETED` 成功结束。
- 限制：真实模型 API、取消、超时、审批、命令工具和精确 token 计数仍未实现。


## 2026-08-27：子任务 8 - 取消、超时与后台任务生命周期

- 目标：为多轮 Agent loop 增加可取消、可超时、可追踪的运行生命周期，避免后台任务 fire-and-forget。
- 新增 `RunTaskManager`：按 `RunId` 保存 active `Future`，支持启动、取消和完成后清理。
- 后端 executor 调整：run executor 和 tool executor 分离，均由 Spring 负责 `shutdownNow`。
- 更新 `AgentRunService` 与 `RunController`：新增 `POST /api/runs/{runId}/cancel`，非终态 run 先发 `RUN_CANCELLING`，再以 `USER_CANCELLED` 结束；终态取消保持幂等。
- 更新 `MockAgentRunner`：启动前、每轮模型请求前后、工具执行前后检查取消状态；工具执行通过 tool executor 和 `RunBudget.toolTimeout` 限时等待，超时转为 `TIME_LIMIT`。
- 更新前端：新增 Cancel 按钮，调用 cancel endpoint，并订阅 `run_cancelling` SSE 事件。
- 执行 LIFE-001 验证：`cd backend && mvn test` 通过，94 tests, 0 failures, 0 errors；`cd frontend && npm run build` 通过；本地 HTTP cancel run `844f0b97-1a46-4955-bf44-2bc47e8b2802` 最终 `CANCELLED / USER_CANCELLED`。
- 限制：Java Future 取消依赖线程中断；未来 shell 命令工具还需要显式销毁 OS 子进程和进程树。

## 2026-08-27：子任务 9 - 审批策略与 diff/变更展示

- 目标：在接入真实模型和命令工具之前，先把可变更工具的安全审批边界和变更可视化做出来。
- 新增 `ToolApprovalPolicy`、`ToolApprovalDecision`、`ToolApprovalMode`：`write_file`、`replace_text` 和预留的 `run_command` 均要求用户审批；其他工具继续交由注册表做正常校验。
- 更新 `MockAgentRunner`：工具执行前发出带审批信息的 `TOOL_CALL_REQUESTED`；遇到需审批工具时发出 `APPROVAL_REQUIRED`，当前因 approve/resume API 未实现而发出 `APPROVAL_RESOLVED approved=false`，随后以 `APPROVAL_REJECTED` 结束 run，且不执行工具。
- 新增 `WorkspaceUnifiedDiff`，让 `write_file` 和 `replace_text` 返回 `unifiedDiff`，同时保留已有 hash、大小和冲突校验。
- 更新前端：订阅审批事件，从工具结果中提取 `unifiedDiff`，在 Diff 面板展示文件变更。
- 更新 `HeuristicMockModelClient`：包含 write/create/写入/创建 的 prompt 会请求 `write_file`，用于验证审批拦截路径。
- 执行 CHANGE-001 验证：后端 `mvn test` 通过 98 tests；前端 `npm run build` 通过；本地 HTTP run `68bb2c0d-b17f-4d5a-8219-d725451f1f68` 最终 `FAILED / APPROVAL_REJECTED`，事件含审批请求和拒绝，不含工具开始事件。
- 限制：当前不是完整人工审批恢复流程；真实点击 Approve 后恢复执行、命令审批细分和浏览器视觉验收待后续实现。
