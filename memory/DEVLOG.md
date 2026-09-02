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

## 2026-08-28：子任务 10 - 完整审批恢复工作流

- 目标：把上一阶段的“审批拦截”推进为完整 approve/reject/resume 闭环，让可变更工具能在用户批准后继续执行并展示 diff。
- 新增 `PendingToolApproval`：保存单个挂起工具调用的 run id、round、tool call、审批决策、上下文消息和已使用工具调用数。
- 扩展 `AgentRunStore`：在 run 旁保存和消费 pending approval，不把开发文档或聊天历史注入 Agent 上下文。
- 重构 `MockAgentRunner`：遇到需审批工具时保存 continuation、进入 `WAITING_FOR_APPROVAL` 并返回；Approve 后执行已批准工具，将结果回填上下文并进入下一轮模型请求。
- 扩展 `AgentRunService` 与 `RunController`：新增 approve/reject endpoint；reject 以 `APPROVAL_REJECTED` 结束且不执行工具；cancel 会清理 pending approval。
- 更新 `RunTaskManager`：允许替换已完成但尚未从 active map 移除的旧 task，降低用户快速点击 Approve 时的竞态风险。
- 更新 Vue 前端：根据审批事件计算 pending approval，展示工具名、参数、审批原因，以及 Approve and run / Reject 按钮；批准后继续展示同一 run 的工具事件和 diff。
- 执行 APPROVAL-001 验证：后端 `mvn test` 通过 101 tests；前端 `npm run build` 通过；本地 HTTP run `fd830860-d275-4484-8fbc-249a51142722` 经 approve 后最终 `SUCCEEDED / COMPLETED`，事件含审批、工具执行、diff 和 run finished。
- 验证创建的 `workspaces/demo/src/mock-note.txt` 已删除，未作为项目改动保留。
- 限制：pending approval 仍为进程内存；当前只支持串行工具模型下的单个 pending approval；真实模型 API 和命令工具仍未实现。

## 2026-08-28：DeepSeek V4 Flash / OpenAI-compatible 模型适配器

- 目标：按用户要求先接入 DeepSeek V4 Flash，但保持默认 mock 模式，避免没有付费 key 时本地开发和测试失败。
- 新增 `AgentModelProperties`，支持通过配置切换 `mock` 与 `openai-compatible` 模型 provider，默认模型名为 `deepseek-v4-flash`，API key 仅从 `DEEPSEEK_API_KEY` 环境变量读取。
- 新增 `OpenAiCompatibleModelClient`，通过 Java 21 `HttpClient` 调用 `/chat/completions`，请求中启用 JSON response format，并把项目内部 JSON 响应协议和工具定义注入 system message。
- 新增 `ModelHttpTransport`、`ModelHttpRequest`、`ModelHttpResponse` 和 `JavaHttpModelTransport`，用于隔离真实 HTTP 调用和单元测试替身。
- 更新 runner 事件 payload：不再把 provider 写死为 `mock`，由 `ModelClient.providerName()` 报告当前模型来源。
- 更新前端文案和检查项，从 “mock runner” 改为 “backend agent runner”，避免真实模型模式下的界面描述不一致。
- 新增 ADR-0015，记录为什么使用 OpenAI-compatible HTTP 适配器而不是 DeepSeek SDK 或写死 DeepSeek。
- 执行 MODELAPI-001 验证：后端 `mvn test` 通过 106 tests；前端 `npm run build` 通过；真实 DeepSeek 端到端调用因会向外部服务发送 prompt、工具定义和后续工具观察，待用户明确授权后执行。
- 限制：当前不使用 provider-native tool calls，不做流式 token 输出或 usage 统计；真实 DeepSeek V4 Flash 任务尚未验证成功。

## 2026-08-28：DeepSeek V4 Flash 真实只读 run 验证

- 用户明确授权向 DeepSeek 发送 demo workspace 测试上下文后，执行真实 DeepSeek V4 Flash 端到端验证。
- 首次真实 run `7e68ea76-0b04-4381-bf9c-90506eb6fb0b` 成功完成模型调用、`list_files`、`read_file` 与 run 结束，但暴露最终 `STOP` 消息为空的问题。
- 收紧 `ModelResponseParser`：模型响应必须包含非空 `message`；新增缺失/空白 message 的解析失败测试。
- 增加 `ModelClientException` 在 runner 中的专门处理，provider 调用错误现在以 `MODEL_ERROR` 结束，不再泛化为 `INTERNAL_ERROR`。
- 根据 DeepSeek 官方文档，V4 默认开启 thinking mode，且 thinking/tool 场景需要额外回传 `reasoning_content`；当前内部消息模型不保存该字段，因此在 OpenAI-compatible 请求中显式设置 `thinking: {type: "disabled"}`。
- 验证 run `eade6508-edad-476f-986c-5d48ef18458a` 成功：DeepSeek V4 Flash 三轮模型循环，调用 `list_files` 和 `read_file`，最终以中文总结结束，状态 `SUCCEEDED / COMPLETED`。
- 后端测试通过 108 tests。
- 限制：真实写入审批、取消、超时、命令工具和浏览器 UI 下的 DeepSeek run 尚未验证。

## 2026-08-28：子任务 12 - Workspace 命令执行工具

- 目标：让 Agent 能在 workspace 内通过审批后的 `run_command` 执行本地命令，用于后续测试、构建和诊断类任务。
- 新增 `WorkspaceCommandTools` 与 `CommandExecutionResult`：使用 argv 数组调用 Java `ProcessBuilder`，不经过 shell；cwd 必须解析到 workspace 内的已存在目录；stdout/stderr 分开捕获并做长度截断；非零退出码作为正常工具观察返回。
- 命令进程启动前清空继承环境，只保留 `PATH`、`LANG`、可选 `LC_ALL`、`CI=true` 和 `PWD`；第一版不允许模型提供自定义环境变量。
- 将 `run_command` 接入 `WorkspaceToolFactory`、`WorkspaceToolConfiguration`、`ToolRegistry` 和 Spring 上下文；扩展 `ToolArgumentReader` 校验字符串数组参数。
- 更新 `HeuristicMockModelClient`：包含 test/command/build/命令/测试/构建 的 prompt 会请求 `run_command`，便于 mock 模式验证审批路径。
- 继续保留 `run_command` 的用户审批要求，并把审批说明从 shell command 修正为 local command，避免误导实现边界。
- 修复新增测试暴露的取消竞态：当 runner 和 cancel endpoint 同时尝试结束 run 时，`completeCancellation` 现在对终态保持幂等，不再对已终止 run 做二次状态转换。
- 执行 COMMAND-001 验证：后端 `mvn test` 通过 118 tests；本地 HTTP run `88cb4eef-61a2-4acc-92af-a0d13eda0d19` 先进入 `WAITING_FOR_APPROVAL`，批准后执行 `/bin/echo mock command` 并最终 `SUCCEEDED / COMPLETED`。
- 限制：当前不是 OS 级沙箱；只销毁直接子进程，不保证完整进程树清理；清空 HOME 等环境变量可能让依赖本机配置的构建命令失败；真实 DeepSeek 命令 run 尚未验证。


## 2026-08-28：前端工具卡片和命令输出展示

- 目标：让录屏能清楚呈现“模型提出命令 → 用户审批 → 执行 → stdout/stderr/exit code 回填”的链路。
- 更新 Vue 工作台：在时间线之外新增 tool card stack，按 tool call id 聚合 `TOOL_CALL_REQUESTED`、`APPROVAL_REQUIRED`、`APPROVAL_RESOLVED`、`TOOL_CALL_STARTED`、`TOOL_CALL_FINISHED`。
- 新增 `frontend/src/run/toolCards.ts`，将事件聚合、命令结果解析、命令行展示、状态标签和输出格式化逻辑从 `App.vue` 抽出，降低页面组件复杂度。
- 为 `run_command` 增加专门命令卡片：展示 argv 合成的命令行、cwd、exit code、duration、stdout、stderr 和截断标记。
- 右侧详情栏新增 Command tab，展示最近一次命令输出，便于演示时聚焦查看。
- 修复浏览器验收中发现的审批按钮禁用问题：前端已收到 pending approval 事件时允许点击 Approve/Reject，合法状态仍由后端 approve/reject endpoint 校验。
- 执行 UI-003 验证：`cd frontend && npm run build` 通过；in-app browser 打开 `http://127.0.0.1:5173/`，默认命令任务进入审批，Approve 后 run 成功，页面显示 `run_command` Finished、`exit: 0`、stdout `mock command`、stderr 空输出，console 无 warning/error 且无横向溢出。
- 限制：本阶段验证使用 mock 模型和固定 `/bin/echo mock command`；真实 DeepSeek 命令审批 run 尚未验证。


## 2026-08-28：真实 demo 编程任务构造

- 目标：为真实模型演示准备一个稳定、无第三方依赖、可读可改可测试的小型编程任务。
- 将 `workspaces/demo` 从简单文本 workspace 升级为 Python pricing demo：新增 `src/price_calculator.py`、`src/__init__.py` 和 `tests/test_price_calculator.py`。
- 设计一个明确 bug：`calculate_total` 把折扣百分比加到 subtotal 上，导致 10% discount 和 100% discount 用例失败；正确行为应为折扣先减少税前 subtotal，再计算税。
- 更新 `workspaces/demo/README.md`，写明 demo task 和验证命令 `python3 -m unittest discover -s tests -v`。
- 更新前端默认 prompt 为真实 bugfix 任务，便于录屏时直接触发完整流程。
- 更新 `.gitignore` 忽略 `__pycache__/` 与 `*.py[cod]`，避免 demo 测试运行后产生的 Python 缓存误入库。
- 执行 DEMO-001 验证：在 `workspaces/demo` 运行 unittest，4 个测试中 2 个按预期失败，失败点集中在折扣计算；`cd frontend && npm run build` 通过。
- 限制：这是故意失败的 demo 基线，不代表主项目构建失败；真实 DeepSeek 修复、写入审批和命令验证尚未执行。

## 2026-08-28：真实模型 demo 首次运行与 loop 策略调整

- 事实：授权后的 DeepSeek V4 Flash demo 修复首次运行已创建 run `34f13ac1-a277-41fe-94fa-c0ef15e045f1`，模型完成 `list_files`、`read_file README.md`、`list_files src`、`list_files tests` 等只读步骤；随后 provider 响应缺少 `choices[0].message.content`，run 以 `MODEL_ERROR` 结束，未进入写入审批，未修改 demo workspace。
- 调整：新增 ADR-0017，将默认预算从 4 轮/12 工具调用调整到 8 轮/16 工具调用；OpenAI-compatible 系统提示要求真实模型一次最多调用一个工具，并补充 `run_command` argv 示例。
- 排障：provider 缺少 content 时只记录安全响应形状（`finish_reason` 和 message 字段名），不记录完整原始响应。
- 验证：后端 `mvn test` 通过，118 tests, 0 failures, 0 errors。

## 2026-08-28：OpenAI-compatible 空 content 响应重试

- 事实：真实 demo run `e186f02c-94f1-4c67-949d-6401d4295568` 在第三轮失败，错误形状为 `finish_reason=stop, message_fields=[role, content]`，说明 provider 返回了空 content；run 未进入写入审批，demo workspace 未被修改。
- 决策：新增 ADR-0018，仅对 HTTP 2xx 且 `choices[0].message.content` 为空/缺失的响应追加一条协议修复提醒，并最多重试一次。
- 验证：后端 `mvn test` 通过，119 tests, 0 failures, 0 errors。

## 2026-08-28：Agent transcript 保留工具动作

- 事实：多次 DeepSeek V4 Flash demo run 在读完 README 后返回空 content；排查发现 runner 只把模型 `message` 放入下一轮上下文，未保留上一轮 `tool_calls` 动作。
- 决策：新增 ADR-0019。模型返回工具调用时，下一轮 assistant transcript 记录用户可读 message 以及工具调用 id、名称和参数摘要；工具观察继续作为单独消息回填。
- 验证：后端 `mvn test` 通过，119 tests, 0 failures, 0 errors；新增断言覆盖第二轮 ModelRequest 包含上一轮工具动作 transcript。

## 2026-08-28：工具调用响应空 message 降级

- 事实：真实 run `82b73d41-6eae-4b85-a92b-219462d13c2c` 在 transcript 修复后进入 `MODEL_PARSE_ERROR`，错误为 `Model field 'message' must be a non-blank string`，说明 provider content 已非空，但 JSON 协议里的展示 message 为空。
- 决策：新增 ADR-0020。仅当 `finish_reason=tool_calls` 且 `tool_calls` 合法非空时，为空/缺失 message 补默认展示文案；最终回答或无工具动作的空 message 仍失败。
- 验证：后端 `mvn test` 通过，120 tests, 0 failures, 0 errors。

## 2026-08-28：模型 JSON 外壳容错提取

- 事实：真实 run `1c85a983-fb96-4692-b387-1a9a545c104f` 在第二轮进入 `MODEL_PARSE_ERROR`，错误为 `Model response is not valid JSON`，说明真实模型可能返回带 Markdown 或说明文字的 JSON 外壳。
- 决策：新增 ADR-0021。解析器先按纯 JSON 解析，失败时提取第一个完整 JSON object，再执行原协议字段校验。
- 验证：后端 `mvn test` 通过，121 tests, 0 failures, 0 errors；新增测试覆盖 Markdown 代码块和前后夹杂文字。

## 2026-08-28：模型协议修复重试

- 事实：真实模型多轮 demo 暴露出空 content、非 JSON、不可降级空 message 等 provider/模型格式漂移；这些失败发生在执行任何新工具动作之前。
- 决策：新增 ADR-0022。OpenAI-compatible 适配器对可恢复协议问题追加一条协议修复提醒并最多重试一次；工具结构错误、参数错误、未知 finish reason、HTTP 错误、工具/审批/预算错误不重试。
- 验证：后端 `mvn test` 通过，122 tests, 0 failures, 0 errors；新增测试覆盖空 message 协议修复重试。

## 2026-08-28：真实 demo 修复闭环验证

- DeepSeek V4 Flash 尝试结果：多次真实 run 能完成部分只读步骤，但在读 README 或读源码后出现 provider 空 content、空 message、非 JSON 或无法提取 JSON 的响应；最后一次强约束 run `5016a759-aa62-44f9-86e7-dbc5dcc8180b` 成功读取测试与源码，但在应提出 `replace_text` 的轮次以 `MODEL_PARSE_ERROR` 失败。结论：DeepSeek V4 Flash 当前不适合作为录屏主模型。
- DeepSeek `deepseek-chat` 强指令 demo 成功：run `546c7fe9-8b05-447b-a25d-87c0ad7dd601` 由真实模型提出 `replace_text`，人工检查并批准后修改 `src/price_calculator.py`；随后真实模型提出 `run_command`，人工检查并批准后运行 unittest。
- 工具链路证据：`replace_text` 只替换一行，将 `discounted = base * (1 + discount_percent / 100)` 改为 `discounted = base * (1 - discount_percent / 100)`；`run_command` 命令为 `python3 -m unittest discover -s tests -v`，cwd 为 `.`，exit code 为 0。
- 验证：Agent 工具输出显示 4 个 unittest 全部 OK；随后在 `workspaces/demo` 外部直接执行同一 unittest，也通过 4 tests, 0 failures, 0 errors。
- 注意：当前 `workspaces/demo/src/price_calculator.py` 保留真实模型修复后的未提交修改，便于检查 diff；如果要录制“从失败到修复”的完整演示，需要先恢复 demo failing baseline。

## 2026-08-28 原生 tool calling 协议接入

- 事实：新增 ADR-0023，OpenAI-compatible 模型适配器默认改为 provider 原生 `tools` / `tool_calls` 协议，并保留 `agent.model.tool-protocol=json-content` 兼容路径。
- 事实：`ModelMessage` 扩展为结构化消息，assistant 消息可携带上一轮工具调用，tool 消息携带 `tool_call_id`，runner 将工具动作和工具结果以结构化形式回填下一轮上下文。
- 事实：新增/更新模型适配器测试，覆盖 native tools 请求体、native tool_calls 响应解析、assistant tool_calls 与 tool result 消息序列化，以及 legacy JSON content fallback。
- 验证：在 `backend/` 执行 `mvn test`，124 个测试通过；DeepSeek V4 Flash 原生 tool calling 只读 run `0edd0f1a-cc84-484e-b436-0888a85a30a5` 成功，模型通过原生 `tool_calls` 调用本地 `list_files` / `read_file` 并最终完成。

## 2026-08-28：DeepSeek V4 Flash 原生工具真实修复 demo

- 事实：先将 `workspaces/demo/src/price_calculator.py` 恢复到 failing baseline，并用 unittest 确认 4 个测试中 2 个失败。
- 事实：DeepSeek V4 Flash 在 native tool calling 模式下完成真实修复 run `d2f4ab1e-5f4c-4458-9248-c38383aa31fa`：读文件、定位折扣符号错误、提出 `replace_text`、审批后改一行、提出 `run_command`、审批后运行测试并最终总结。
- 验证：Agent 内部 `run_command` exitCode 0，4 个 unittest 全部 OK；随后外部直接执行同一 unittest，也通过 4 tests, 0 failures, 0 errors。
- 注意：这次成功基于 ADR-0023 的原生 `tools` / `tool_calls` 协议，和此前 JSON content 协议下 V4 Flash 不稳定的结果应分开解释。

## 2026-08-28：Codex-like 前端工作台重构

- 事实：用户指出旧界面过度暴露原始事件 JSON，缺少 Codex-like 对话体验、权限审批、右侧文件/diff 面板和底部 terminal。
- 决策：新增 ADR-0024。后端事件协议保持审计语义，前端新增 `run/timeline.ts` 投影层，将事件转换为对话消息、工具卡、审批卡、Inspector 和 Terminal 视图。
- 实现：拆分 `ProjectSidebar`、`ChatTimeline`、`ApprovalCard`、`ToolCallCard`、`InspectorPane`、`BottomTerminal`、`ComposerBox`，`App.vue` 改为编排状态和 API 调用。
- 验证：`frontend/` 执行 `npm run build` 通过；浏览器打开 dev server 目视检查三栏 + 底部 terminal，并触发一次 run 到 `run_command` 审批点，确认权限审批卡、文件 Inspector 和命令 terminal 可见。

## 2026-08-28：工作台面板收起与拖拽调整

- 目标：根据用户对 Codex 界面的对比反馈，把右侧 Inspector 和底部 Terminal 从固定区域改为可收起、可展开、可拖拽伸缩的工作台面板。
- 实现：`App.vue` 新增 `inspectorOpen`、`inspectorWidth`、`terminalOpen`、`terminalHeight` 和统一拖拽状态；右侧 Inspector 选中文件、diff、command 时自动展开；出现 `run_command` 时底部 Terminal 自动展开。
- 实现：`InspectorPane` 增加隐藏按钮和面板开合 props；`BottomTerminal` 增加展开/收起按钮，收起时保留紧凑标题栏；`ChatTimeline` 增加新消息自动滚动到底部。
- 样式：新增右侧竖向 resize handle、底部横向 resize handle、右侧折叠后的 reopen pill，并压缩工具卡片视觉密度，让对话区更接近 Codex 的轻量工作流。
- 验证：`frontend/` 执行 `npm run build` 通过。
- 限制：本次只做前端构建验证，尚未做浏览器拖拽和完整审批后终态目视验收；Terminal 仍显示命令完成后的聚合 stdout/stderr，不是进程级实时字符流。

## 2026-08-28：精简审查/文件侧栏并移除底部命令栏

- 目标：根据用户反馈继续向 Codex 图二靠近，取消底部 command/terminal 常驻栏，减少右侧面板无用入口。
- 实现：从 `App.vue` 移除 `BottomTerminal` 使用、底部 terminal 开合和纵向拖拽状态；工作台改回单行主布局，主对话区获得更多垂直空间。
- 实现：删除 `BottomTerminal.vue`；`InspectorPane` 右侧入口精简为「审查」和「文件」。点击「审查」优先展示已产生的 diff；点击「文件」展示文件列表并支持选中文件预览。
- 实现：清理 `timeline.ts` 中已无 UI 入口的 checks/terminal 投影类型和函数，避免展示层模型残留无用概念。
- 验证：`frontend/` 执行 `npm run build` 通过。
- 限制：命令结果仍可从工具卡进入审查详情查看，但不再作为独立底部栏或右侧 tab；本次未做浏览器目视验收。

## 2026-08-28：Codex-like 视觉降噪

- 目标：继续对照 Codex 图二调整空白工作台状态，降低侧栏、右侧工具入口和 welcome 区域的视觉重量。
- 实现：左侧栏宽度从 300px 收紧到 260px；项目卡不再重复显示完整 workspace path，仅保留项目名；品牌副标题移除。
- 实现：右侧 Inspector header 移除 `backend ok` 开发态 chip，仅保留 workspace 标题和收起按钮；「审查/文件」从大面积按钮改成轻量工具入口。
- 样式：压缩 workspace header、empty thread、composer、Inspector tab 和文件卡片的 padding/radius/shadow，让初始界面更接近 Codex 的清爽工作台。
- 验证：`frontend/` 执行 `npm run build` 通过；in-app browser 刷新 `http://localhost:5173/` 后确认左栏为 260px、右侧两个入口约 34px 高、Inspector 无 backend chip、底部 terminal DOM 不存在、console 无 warning/error。
- 限制：本次仍是空状态目视验收，尚未录制或验收完整真实模型修复闭环的动态界面。

## 2026-08-29

- 使用 Frontend Design Premium / frontend-design 约束重整前端工作台语义展示：新增 `DESIGN.md` 记录 Codex-like 本地工作台视觉与交互约定。
- 将工具事件从 raw card 映射为 Codex-like 轻量动作行和文件变更摘要卡：隐藏空的 tool-call 模型消息，运行中无可见内容时显示“正在思考”，run 结束不再展示 rounds/tools 等内部元数据。
- 重做右侧 Workspace 面板：保留“审查/文件”，审查面板以行号和增删色块显示 diff；文件面板以文件列表和行号预览展示已读取内容。
- 修复设计审计发现的假 affordance 与 form/textarea 约束：项目行不再是无动作按钮，composer 使用 `novalidate`，textarea 明确不可拖拽。

## 2026-08-29：审批、Markdown 与审查面板可用性修正

- 根据用户对真实 Codex 交互的截图反馈，修正审批卡文案：`write_file` / `replace_text` 不再以工具名为主标题，而是展示“申请写入/修改具体文件”，并在可用时直接展示拟写入内容预览。
- 新增安全 Markdown 渲染组件，不使用 `v-html`，支持标题、列表、代码块、行内代码和加粗；run 完成后 timeline 只保留最后一条 assistant 文本，隐藏中间思考式输出。
- Composer 支持 Return 发送，Command-Return/Control-Return 保留；run 创建成功后清空输入框，创建失败时恢复草稿。
- 修正审查面板滚动布局：diff/file preview 的滚动收束在右侧面板内部，避免长代码行把横向滚动条暴露在页面中间。

## 2026-08-29：审批空引用错误与前端流式输出

- 定位截图中的红色错误：批准工具后 SSE 会刷新 pending approval 状态，`approvePendingTool` 随后继续读取 `pendingApproval.value.name`，可能变成 `null.name`。修复为批准/拒绝前先保存 approval 快照。
- 成功收到 `RUN_FINISHED` 且状态为 `SUCCEEDED` 时主动清理 `runError`，避免旧错误残留在成功 run 下方。
- `ChatTimeline` 新增基于现有 SSE 模型消息事件的前端流式 reveal：活动 run 中 assistant 内容按小块显示，并提供光标动效；这不是 provider token-level streaming，后者需后端模型客户端另行接入 stream API。

## 2026-08-29：Codex-like 前端视觉 polish

- 根据用户继续要求和 `$gpt-taste` 约束，做一轮工作台视觉审查和 polish；本次不引入新前端依赖，避免为局部视觉优化增加构建风险。
- 顶部从“LOCAL WORKSPACE + 路径”改为任务标题优先，workspace path 降级为副标题，状态 chip 改为中文任务状态。
- 左侧栏压缩为更接近 Codex 的任务导航：按钮和分组文案中文化，run 状态本地化，并修正长标题/状态导致的横向溢出风险。
- 中央对话区降低卡片边框和阴影噪音，用户消息改为浅灰气泡，composer 收窄居中并中文化 placeholder、hint 和按钮。
- 右侧审查/文件面板去掉管理后台式卡片感，diff/file 预览改为面板内换行和行号布局，避免长代码行把横向滚动条带到页面中部。

## 2026-08-29：Agent 文件变更撤销

- 目标：补齐类似 Codex 的“Agent 修改可撤销”体验，让用户在审批并执行文件变更后仍可从 UI 回退该次变更。
- 后端实现进程内 `WorkspaceChangeJournal`：`write_file`/`replace_text` 成功后通过工具私有 metadata 记录撤销快照；旧内容不进入工具 JSON、模型观察或 SSE payload。
- 新增 `POST /api/runs/{runId}/changes/{toolCallId}/undo`：用户直接触发撤销，不作为模型工具暴露；撤销前校验当前文件 hash 等于 Agent 修改后的 hash，若用户或后续工具已改动文件则拒绝覆盖。
- 撤销新建文件时删除该文件；撤销替换/覆盖时恢复旧 UTF-8 内容；撤销成功后追加 `CHANGE_UNDONE` 事件。
- 前端在变更摘要卡和右侧审查面板显示“撤销/撤销中/已撤销”，撤销后回拉事件列表并更新卡片状态。
- 执行验证：`cd backend && mvn test` 通过 128 tests；`cd frontend && npm run build` 通过；Frontend Design Premium strict audit 0 findings；`git diff --check` 通过。
- 限制：撤销 journal 目前为进程内存，后端重启后不能撤销旧 run 的变更；未做真实浏览器点击撤销的目视验收。

## 2026-08-29：Codex-like 前端体验升级

- 使用 Frontend Design Premium 和 `$gpt-taste` 约束做一轮产品工作台升级，不新增前端依赖。
- 新增共享 `UiIcon` 线性图标组件，替换 sidebar、action row、Inspector tabs、run/stop、diff 和 undo 上的散装文字图标。
- Composer 改为 IME 友好的 Return 发送：中文输入法 composition 期间不拦截，Shift-Return 换行，Command/Control-Return 仍可发送；停止/运行按钮统一图标和中文状态。
- Timeline 保留用户阅读位置：只有用户停在底部时才随新事件自动滚动；待审查变更计数排除已撤销项。
- Inspector 审查面板补齐 Codex-like 细节：按文件扩展名显示 `PY/VUE/MD` 等语言徽标，撤销按钮统一状态和可访问标签，命令详情中文化工作目录/退出码。
- App 顶部健康状态、错误 fallback、面板 aria/title 文案中文化，并订阅 `CHANGE_UNDONE` SSE 事件。
- 样式层新增 Codex-like v2 覆盖：Geist-like 字体栈、统一 token、轻量 hover motion、reduced-motion 保护、全局 scrollbar、无负字距和无新增装饰渐变。
- 同步更新 `DESIGN.md`：记录已实现撤销入口、已撤销变更不再计入待审查、timeline 滚动策略和 IME composer 规则。
- 验证：`cd frontend && npm run build` 通过；Frontend Design Premium strict audit 0 findings；`git diff --check` 通过；静态扫描未发现旧英文错误 fallback、负字距、装饰渐变、hero/orb/bokeh。

## 2026-08-29：Composer 对话框专项优化

- 根据用户截图反馈，聚焦底部对话框体验：移除默认填入的英文 demo prompt，初始输入框改为空白等待用户指令。
- Composer 增加三枚轻量快捷建议：修复 demo 测试、审查最近改动、优化前端体验；点击后只填入输入框，不自动发送。
- 输入框焦点样式从硬蓝色 textarea outline 改为外层 composer 的柔和 focus ring，footer 和按钮区更像一个整体命令面板。
- Footer hint 改为更轻的分隔点样式，运行/停止按钮高度收紧，移动端建议 chip 自动铺开。
- 额外本地化 `fetchRunEvents` 失败 fallback 文案。
- 验证：`cd frontend && npm run build` 通过；Frontend Design Premium strict audit 0 findings；`git diff --check` 通过。

## 2026-08-29：本地后端启动与 mock 功能自测

- 按用户要求启动后端：`cd backend && mvn spring-boot:run`，Tomcat 在 `8080` 启动成功；既有 Vite dev server 在 `5173` 继续运行。
- 验证后端健康接口和前端 `/api` 代理均返回 `status=ok`。
- 通过 HTTP API 完成 mock 写入审批闭环：创建 run、产生 `APPROVAL_REQUIRED`、批准 `write_file`、生成 diff 和 `undoable=true`、文件落盘、run 成功结束。
- 通过 HTTP API 完成撤销闭环：撤销刚才的 `write_file`，新建文件被删除，事件回看包含 `CHANGE_UNDONE`。
- 通过 HTTP API 完成命令审批闭环：批准 `run_command` 后得到 `exitCode=0`、stdout 和 duration，run 成功结束。
- 通过 HTTP API 完成拒绝审批闭环：拒绝 `write_file` 后 run 以 `APPROVAL_REJECTED` 失败，未执行工具。
- 限制：本次没有通过浏览器实际点击 UI，也没有跑真实 DeepSeek 模型；记录见 `SELFTEST-001`。

## 2026-08-29：H2 本地持久化

- 数据库选择：采用 H2 file mode + Spring JDBC；理由是适合本地单用户作业场景，Spring 集成和测试简单，不需要 Postgres/MySQL 外部服务，也比 JSONL 更利于结构化查询和 schema 演进。
- 新增 `spring-boot-starter-jdbc` 和 H2 runtime 依赖；默认数据库位于 `backend/data/coding-agent.*`，并已加入 `.gitignore`。
- 新增 `schema.sql` 初始化 `agent_runs`、`agent_run_events`、`pending_tool_approvals` 和 `workspace_change_undo`。
- `AgentRunStore` 启动时从 JDBC persistence adapter 加载历史 run、事件和 pending approval；运行中保存 run 状态、事件和 pending approval 消费状态。
- `WorkspaceChangeJournal` 持久化 undo snapshot、撤销状态和撤销结果，旧内容仍只保存在本地数据库，不进入模型观察、SSE payload 或前端事件日志。
- 新增 `GET /api/runs` 返回历史 run 列表；前端启动后拉取历史 run，并通过事件回看恢复侧栏任务标题。
- 新增 ADR-0026 记录数据库选择和边界。
- 验证：`cd backend && mvn test` 通过 131 tests；`cd frontend && npm run build` 通过；`git diff --check` 通过；本地后端跨重启后 `GET /api/runs` 和指定 run 事件回看仍可读。

## 2026-08-30：Agent Runtime 退出语义与工具失败恢复

- 根据用户对 Agent Runtime 子任务的拆分，保持现有 loop 架构不大改，只收紧三类退出语义：模型最终回答为正常完成；round/tool/token/cancel 等预算或用户动作属于系统强制终止；模型 API、provider 响应解析和内部 runtime 异常属于不可恢复失败。
- 修正 `MockAgentRunner` 对 `ToolResult.success=false` 的处理：工具失败不再直接 `TOOL_ERROR`/`TIME_LIMIT` 结束 run，而是作为 `success=false` tool observation 加回 `List<ModelMessage>`，继续下一轮 LLM，让模型有机会修正路径、参数、替换内容或命令。
- 审批恢复路径同步使用相同语义：用户批准后若工具执行失败，也回填 observation 并继续下一轮，而不是直接 failed。
- 新增 ADR-0027 记录“工具失败作为可恢复 Observation”的决策和代价。
- 验证：`cd backend && mvn test` 通过 131 tests；新增/更新测试覆盖普通工具失败和工具 timeout 都会进入下一轮模型请求。

## 2026-08-30：Tool System 语义与 Observation 结构化

- 根据用户对子任务 2 Tool System 的建议，保持现有 Observe → Act → Verify 工具集合不扩张，只补语义统一和 observation 质量。
- 新增 `edit_file` 工具，作为 `replace_text` 的兼容别名：参数、执行逻辑、审批策略、diff 和撤销 snapshot 均复用 exact text replacement 能力。
- `WorkspaceChangeJournal`、`ToolApprovalPolicy` 和前端 change-tool 映射同步识别 `edit_file`。
- 工具成功结果 JSON 增加统一 `success=true` 和 `message` 字段；命令结果增加 `success` 和 `timedOut`，其中非 0 exit code 表示命令 observation 的 `success=false`，但工具调用本身仍成功返回 observation。
- `ToolRegistry` 将未知工具、参数错误、workspace 拒绝和运行时错误包装为结构化失败 JSON，包含 `success=false`、`message`、`toolName`、`errorCode`、`timedOut` 和 metadata，便于 LLM 下一轮恢复。
- 验证：`cd backend && mvn test` 通过 132 tests；`cd frontend && npm run build` 通过。

## 2026-08-30：Context Management 配对感知裁剪

- 根据用户对子任务 3 Context Management 的要求，修复上下文窗口可能从中间截断 assistant tool call 与 tool result 的风险。
- `MockAgentRunner` 的 context window 继续保留 system prompt 和初始 user task；最近历史改为从尾部按消息组纳入窗口。
- 普通 user/assistant 消息按单条处理；assistant tool_calls 与其后连续 tool results 作为不可拆分组处理。
- 若裁剪过程中遇到孤立 tool result，则跳过该消息，避免向 native tool calling provider 发送缺少对应 assistant tool_calls 的 `role=tool` 消息。
- 新增 ADR-0029 记录这一 context strategy 的边界和限制。
- 验证：`cd backend && mvn test` 通过 133 tests；`git diff --check` 通过；测试生成的本地 H2 数据库文件已清理。

## 2026-08-30：Failure Recovery P0

- 根据用户对子任务 4 的要求，将错误恢复正式拆成三类：recoverable tool error、resource/policy termination、infrastructure failure。
- `ToolRegistry` 的失败 JSON 增加 `failureKind=RECOVERABLE_TOOL_ERROR`、`recoverable=true` 和 `recoveryHint`，让 LLM 下一轮知道该如何修正。
- Workspace 错误码从泛化 access denied 细分为 `WORKSPACE_NOT_FOUND`、`WORKSPACE_INVALID_PATH`、`WORKSPACE_PERMISSION_DENIED`、`WORKSPACE_CONFLICT`、`WORKSPACE_EDIT_MISS` 和 `WORKSPACE_ACCESS_DENIED`。
- `MockAgentRunner` 的 system prompt 增加恢复策略：工具失败是 observation，模型应读取 `errorCode/message/recoveryHint` 后调整计划继续，而不是直接最终失败。
- 新增端到端式单元测试：模型先 `read_file("src/foo.py")` 失败，随后 `list_files(".")` 找到文件，再 `read_file("README.md")` 并正常完成。
- 验证：`cd backend && mvn test` 通过 135 tests；`git diff --check` 通过；测试生成的本地 H2 数据库文件已清理。

## 2026-08-30：System Prompt operating policy

- 根据用户对子任务 5 System Prompt 的建议，将 runner system prompt 从临时“local coding agent + JSON 格式”提示升级为简短 operating policy。
- 新 prompt 明确 role、workspace awareness、Inspect → Understand → Modify → Verify → Recover 工作流、避免无关探索、避免重复失败动作、完成时总结修改与验证。
- 保持 prompt 克制，不写成长篇编程教程；provider adapter 仍只负责 JSON/native tool calling 输出协议和单工具轮次规则。
- 新增 `MockAgentRunnerTests.systemPromptDefinesAgentOperatingPolicy`，锁定关键 prompt 约束已注入模型请求。
- 验证：`cd backend && mvn test` 通过 136 tests；`git diff --check` 通过；测试生成的本地 H2 数据库文件已清理。

## 2026-08-30：Process timeout cleanup

- 根据用户要求，补齐 `run_command` 中断/timeout 时的进程树清理，避免只销毁 parent 导致 child/grandchild 残留。
- `WorkspaceCommandTools` 的命令等待从无限 `waitFor()` 改为短间隔轮询，以便更稳定响应线程 interrupt。
- 命令线程被中断时，使用 `ProcessHandle.descendants()` 收集后代进程，按 descendants → parent 顺序 `destroyForcibly()` 并短暂等待退出；销毁 parent 后再 best-effort 补扫 descendants。
- 若当前 OS/sandbox 禁止 descendants 枚举，则捕获异常并至少清理 parent，不让 cleanup 自己变成 runtime failure。
- 新增 `WorkspaceCommandToolsTests.interruptedCommandDestroysChildProcessTree`，验证子进程被中断清理后不再继续产生输出。
- 验证：普通 sandbox 下 `mvn -Dtest=WorkspaceCommandToolsTests test` 通过，因进程枚举受限跳过 1 项；提权后同一测试 8 tests 全部通过且 0 skipped；完整 `cd backend && mvn test` 通过 137 tests。

## 2026-08-30：Provider token-level streaming

- 根据用户要求，将此前前端消息级 reveal 升级为真实 provider token-level streaming。
- 新增 `StreamingModelClient` 和 `ModelStreamListener`，runner 对支持 streaming 的模型客户端调用 `completeStreaming`，并把文本增量持久化为 `MODEL_MESSAGE_DELTA` 事件。
- `OpenAiCompatibleModelClient` 在 native tools 协议下发送 `stream=true`，逐行解析 provider SSE chunk；`delta.content` 立即回调给 runner，fragmented `delta.tool_calls` 继续累计到完整 `ModelResponse` 后进入既有 Agent loop。
- `JavaHttpModelTransport` 增加 streaming response 支持，普通 mock/JSON content 路径保持同步 complete 行为。
- 前端 SSE 订阅新增 `model_message_delta`，timeline 会按 round 拼接 delta，在最终 `MODEL_MESSAGE_RECEIVED` 到达后使用完整消息替换临时流式内容，历史回放仍由持久化事件重建。
- 新增 ADR-0033 记录流式协议边界。
- 验证：`cd backend && mvn -Dtest=OpenAiCompatibleModelClientTests,MockAgentRunnerTests test` 通过，26 tests；完整 `cd backend && mvn test` 通过，140 tests；`cd frontend && npm run build` 通过；`git diff --check` 通过。

## 2026-08-30：全项目提交前检查与文档更新

- 按题目 PDF 重新对照当前项目：核心 Agent loop、本地工具、context management、termination、recoverable failure、workspace safety、system prompt、UI 展示和 demo workspace 均已有实现与测试证据。
- 新增根目录 `README.txt`，作为提交 zip 中 1000 汉字以内说明草稿，包含公开 Git 仓库地址、运行方式、核心功能和验证命令。
- 重新执行后端全量测试、前端生产构建和 demo workspace unittest。
- 扫描禁用 Agent 框架关键字，未在后端/前端依赖中发现 LangChain、LlamaIndex、OpenAI Agents SDK、Claude Agent SDK、AutoGen、CrewAI 或 Spring AI。
- 扫描常见密钥形态，未发现真实 API key；命中项为正常变量名、CSS token 命名或测试假数据。
- 检查 `.gitignore` 命中题目 PDF、`backend/data/` 本地 H2 数据库和非 demo 私有 workspace。
- 待处理：当前仍有大量未提交改动；demo workspace 中还有 untracked C++ 临时文件，提交前应清理或明确不纳入。

## 2026-09-01：真实命令 run 截图问题定位与修复

- 根据用户截图和本地后端测试报告定位：截图中的 `g++ -O2 -o solution solution.cpp` 退出码 69 来自本机 Xcode license 未同意；`g++ --version` 可复现相同提示。
- 进一步排查到模型把下一次 `run_command.command` 发成了 JSON 字符串形式的 argv array：`"[\"which\", \"g++\"]"`。旧后端只接受真实 JSON array，会把该调用判为 `INVALID_ARGUMENTS`。
- 修复 `run_command` 参数读取：继续要求 argv array；若收到字符串，只接受内容为 JSON string array 的情况并归一化；普通 shell 字符串如 `which g++` 仍然拒绝。
- 修复 OpenAI-compatible native tools streaming：provider stream 若没有 assistant content/tool call，则降级为一次非 streaming native completion，避免空 stream 直接导致 `model_error`。
- 前端工具卡补充 JSON-string argv 展示兼容，使旧事件中的命令显示为 `which g++` 而不是原始 JSON payload。
- Spring 集成测试改用 H2 memory database，避免全量测试争抢真实 `backend/data/coding-agent.mv.db` 导致 `Database may be already in use`。
- 新增 ADR-0034 记录命令参数归一化和空 stream 降级策略。
- 验证：`cd backend && mvn -Dtest=WorkspaceToolFactoryTests,OpenAiCompatibleModelClientTests test` 通过，26 tests；`cd backend && mvn test` 通过，143 tests；`cd frontend && npm run build` 通过；`git diff --check` 通过。

## 2026-09-01：多 tool calls native history 400 修复

- 根据用户第二张截图继续排查 H2 事件库快照，最新 run 以 `MODEL_ERROR` 失败，完整 provider 响应为 HTTP 400：`An assistant message with 'tool_calls' must be followed by tool messages responding to each 'tool_call_id'`。
- 事件序列显示 DeepSeek 在第 2 轮无视 prompt，一次返回了 4 个只读 tool calls；后续这类多工具 assistant message 进入 native tools 历史后触发 provider 校验错误。
- 修复 `MockAgentRunner`：每次 `TOOL_CALLS` 响应只接收第一个 tool call 写入 transcript 并执行，其他同批 tool calls 丢弃，让后续请求始终保持 `assistant(one tool_call) -> tool(result)` 的稳定形态。
- 调整 tool-call budget 测试语义，并新增 `acceptsOnlyFirstToolCallFromEachModelResponse` 锁定单工具强制策略。
- 前端 `RUN_FINISHED` 展示补充精简后的 `errorMessage`，以后 model/provider 错误不再只显示 `model_error`。
- 新增 ADR-0035 记录 Runtime 单工具调用强制策略。
- 验证：`cd backend && mvn -Dtest=MockAgentRunnerTests,OpenAiCompatibleModelClientTests,WorkspaceToolFactoryTests test` 通过，42 tests；`cd backend && mvn test` 通过，144 tests；`cd frontend && npm run build` 通过；`git diff --check` 通过。

## 2026-09-01：Streaming fallback 与真实端到端流程复核

- 用户表示本地环境已安装完成后，请求完整跑通流程；重新启动真实 DeepSeek V4 Flash native tools 后端并执行自测。
- 复核发现系统 `g++`/`clang++` 仍因 Xcode/CommandLineTools linker 问题失败，Homebrew clang 可做 compile-only，但完整 C++ link 仍受本机环境影响；因此本轮用 Python 任务验证 Agent Runtime 闭环。
- 真实 C++ run 在多轮写入/编译反馈后暴露 `Model HTTP streaming request failed`，定位为 streaming HTTP IO 传输异常被直接视为 `MODEL_ERROR`。
- 修复 `OpenAiCompatibleModelClient`：native tools streaming 遇到 `IOException` cause 的 `ModelClientException` 时，降级为非 streaming native completion；HTTP 400/429 等 provider 错误继续显式失败。
- 新增 `OpenAiCompatibleModelClientTests.fallsBackToNonStreamingNativeRequestWhenProviderStreamTransportFails`，锁定 streaming transport failure fallback 行为。
- 真实 Python 端到端 run `2c110940-e4d7-4e42-b376-4ec0529046c3` 成功：模型写入 `selftest_factorial.py`，用户审批后运行 `echo 5 | python3 selftest_factorial.py`，命令返回 exit 0/stdout `120`，模型最终总结完成。
- 验证：`cd backend && mvn -Dtest=OpenAiCompatibleModelClientTests,MockAgentRunnerTests test` 通过，29 tests；`cd backend && mvn test` 通过，145 tests；`cd frontend && npm run build` 通过；`git diff --check` 通过。

## 2026-09-01：Command Line Tools 修复后真实 C++ Agent flow

- 用户完成 `sudo xcode-select --switch /Library/Developer/CommandLineTools` 后，`xcrun --find ld` 已返回 `/Library/Developer/CommandLineTools/usr/bin/ld`。
- 本地最小 C++ 验证通过：`clang++ -std=c++17 workspaces/demo/hello.cpp -o /private/tmp/coding-agent-hello` 成功，运行输出 `Hello, world!`。
- 真实 Agent 首次使用裸 `clang++` 创建 `selftest_sum.cpp` 后，命令工具的最小环境下 Apple clang 找不到 `iostream`，模型尝试恢复但最终撞到 8 轮上限；这不是 linker 问题，而是干净命令环境和本机 C++ 标准库查找之间的兼容问题。
- 随后使用明确路径 `/opt/homebrew/bin/g++-15` 运行真实 DeepSeek V4 Flash native tools C++ flow，run `019de8c5-f6f9-4af1-b308-52b33bc995e0` 成功完成。
- 成功 flow 包含 4 个模型 round、3 次工具调用：写入 `selftest_sum_gcc.cpp`、编译生成 `selftest_sum_gcc`、用输入 `7 35` 运行并得到 stdout `42`。
- 说明：C++ 录屏若要稳定，prompt 中建议明确使用 `/opt/homebrew/bin/g++-15`；长期可以考虑给 `run_command` 增加受控 stdin 字段和更完整的 macOS toolchain 环境白名单。

## 2026-09-01：修复 run_command 的 macOS C++ 环境

- 根据用户截图继续处理两个问题：`iostream` 找不到，以及 run 达到 8 轮上限后停止。
- 解释并确认 8 轮停止来自 `RunBudget` 的 `MAX_ROUNDS` 防无限循环硬限制；它是 termination 机制，不是崩溃。
- 修复 `WorkspaceCommandTools` 的命令环境：PATH 默认优先包含 `/opt/homebrew/opt/llvm/bin` 与 `/opt/homebrew/bin`，再合并后端进程继承 PATH；保留 `TMPDIR`，避免 clang 在极简环境下无法创建临时文件。
- 增加 macOS 编译器窄范围 alias：当模型请求 `g++`/`gcc` 且 `/opt/homebrew/bin/g++-15`/`gcc-15` 存在时，工具执行前解析为 Homebrew GCC，避免落到本机 Apple clang 的不完整 C++ header 路径。
- 根据用户反馈，将默认 `maxRounds` 从 8 调高到 1000，仅作为极端兜底；正常任务主要通过模型 STOP、`maxToolCalls=64`、用户取消和 provider/工具错误恢复策略结束。
- 后端单元测试新增 `resolvesHomebrewGccAliasWhenAvailable`，同时锁定最小环境仍不暴露 `HOME` 或 `DEEPSEEK_API_KEY`。
- 真实验证 run `9718dc9f-2a36-4111-a22f-b62c56b85b1d`：用户任务中编译命令为普通 `g++ -std=c++17 ...`，工具实际执行 `/opt/homebrew/bin/g++-15 ...`，编译 exit 0，运行输出 `42`，最终 `SUCCEEDED / COMPLETED`。
- 验证：`cd backend && mvn -Dtest=WorkspaceCommandToolsTests test` 通过 9 tests；`cd backend && mvn test` 通过 146 tests；调整默认 round 上限后重新执行后端全量测试通过。

## 2026-09-01：Codex-like 审查与文件面板升级

- 根据用户提供的 Codex 参考图，重做右侧 Inspector 的审查/文件两个入口。
- 审查面板不再跟随最后一次工具详情，而是按当前对话内所有成功修改聚合文件列表；点击某个文件后在该项下展开对应 diff，并保留撤销入口和新增/删除统计。
- diff 视图改为代码列 `max-content` 布局并允许横向滚动，避免长行在窄侧栏内强制折行影响审查。
- 新增只读 workspace API：`GET /api/workspace/files` 返回工作目录列表，`GET /api/workspace/file` 返回文件内容；实现复用既有 workspace path resolver 和 read tools，继续拒绝路径穿越。
- 文件面板改为目录树形式，按目录优先排序、支持展开目录；点击文件后在右侧预览区读取并显示内容，内容区同样支持横向滚动。
- 前端启动、切换 run 和回到新任务页时会预加载 workspace 根目录，避免文件面板首次打开为空。
- 新增 `WorkspaceControllerTests` 覆盖根目录列表、读取文件和拒绝 `../` 越界路径。
- 验证：`cd frontend && npm run build` 通过；`cd backend && mvn -Dtest=WorkspaceControllerTests,WorkspaceCommandToolsTests test` 通过 12 tests；`cd backend && mvn test` 通过 149 tests；`git diff --check` 通过。

## 2026-09-01：默认真实模型与文件面板空态修正

- 根据用户反馈，定位当前页面出现 mock 输出的原因：后端使用普通 `mvn spring-boot:run` 启动时仍读取默认 `agent.model.provider=mock`。
- 将应用默认 provider 改为 `openai-compatible`，默认模型仍为 DeepSeek V4 Flash，协议为 native tools，密钥继续只从 `DEEPSEEK_API_KEY` 环境变量读取。
- Spring Boot 集成测试显式覆盖 `agent.model.provider=mock`，避免自动化测试依赖外部模型服务、网络或真实密钥。
- 新增 ADR-0037，并将 ADR-0015 中“应用默认仍使用 mock”的默认 provider 策略标记为被取代。
- 文件面板在未选中文件时不再显示右侧空预览块，目录树单列占满 Inspector；选中文件后才切换为目录树 + 文件内容预览双列。
- 验证：`cd frontend && npm run build` 通过；`cd backend && mvn test` 通过 149 tests；后端已用新默认配置重启到 8080，前端 Vite 保持 5173。

## 2026-09-01：审查面板仅显示修改文件

- 根据用户反馈，进一步收紧右侧审查面板职责：审查过程中不再显示命令、工具参数或 stdout/stderr 返回。
- 批准 `run_command` 后 Inspector 保持在审查文件列表；只有 `write_file`、`replace_text`、`edit_file` 这类文件修改审批后才打开对应 diff。
- 点击 timeline 中的命令卡或普通工具卡时，右侧仍回到审查文件列表；点击文件修改卡才展开该文件 diff。
- 移除 Inspector 中命令详情和普通工具详情分支，避免历史/未来 selection 误入后展示命令结果。
- 验证：`cd frontend && npm run build` 通过。

## 2026-09-01：结束状态与审查展开保持修复

- 根据用户反馈修复两个前端状态问题。
- 结束状态：timeline 构建时只要事件列表出现 `RUN_FINISHED`，即使 run 状态刷新尚未完成，也立即将 assistant 消息视为非 streaming，避免最终回答后仍显示流式光标或运行中状态。
- App 层新增 effective run status，以 `RUN_FINISHED` 事件优先驱动顶部状态、取消按钮和 timeline active 判断，降低状态刷新延迟对 UI 的影响。
- 审查展开保持：批准或点击 `run_command`/普通工具不再修改当前 Inspector selection；如果用户已展开某个文件 diff，命令审批刷新后继续保持展开。
- 只有文件修改工具 `write_file`、`replace_text`、`edit_file` 的审批或卡片点击会切换到对应 diff。
- 验证：`cd frontend && npm run build` 通过。

## 2026-09-01：审查面板撤销状态同步

- 根据用户反馈修复聊天区撤销后右侧审查面板未明显同步的问题。
- 审查聚合现在分别计算原始 diff 统计和仍有效的 active diff 统计；顶部 `修改位置` 只统计未撤销变更。
- 如果一个文件的修改都已撤销，审查文件列表显示 `已撤销`，不再继续显示绿色新增/删除统计。
- 单个 diff block 会根据 `tool.undone` 显示 `已撤销`，撤销按钮禁用，diff 内容弱化展示，和聊天卡片的撤销状态保持一致。
- 验证：`cd frontend && npm run build` 通过。

## 2026-09-01：Codex-like 高级视觉升级

- 根据用户提供的 Codex 参考图和当前前端截图，使用 Frontend Design Premium 与 gpt-taste 约束重新收敛工作台视觉方向。
- 保持现有三栏产品结构不变，不引入 landing page 或无功能装饰；重点优化 sidebar、workspace header、timeline、composer、审查 diff 和文件预览的视觉层级。
- 左侧栏宽度从 260px 调整为 288px，中间主列最小宽度从 520px 调整为 560px，提升任务标题和中英文混排的可读性。
- 新增 Codex-like token 覆盖层：Geist-like 字体栈、浅色技术网格底纹、玻璃质感 header/composer、克制阴影、统一圆角、稳定滚动条和更清晰的成功/失败/运行状态芯片。
- 优化中间对话区：消息阅读宽度收敛到 820px，用户气泡改为深色高对比，assistant 内容保持轻量白色叙事面；底部 composer 改为更精致的大输入面板和灰色主运行按钮。
- 优化右侧 Inspector：审查文件行、语言徽标、diff block、横向滚动代码区和文件预览面板进一步接近 Codex 的密度与层次。
- 更新 `DESIGN.md`，记录本轮高级视觉语言，避免后续 UI 回退到普通后台模板。
- 根据用户反馈继续修正对话区：上一轮深色用户气泡被通用段落色覆盖，导致文字不可读；本轮改为 Codex 参考中的右侧浅灰用户气泡，并把 assistant 正文、工具动作行和变更卡片收敛到中央对话流。
- 根据用户继续反馈修正三个细节：顶部左侧 workspace 图标改为共享 folder icon，移除导致异常空白方块的 CSS 伪元素；顶部右侧移除在线/运行状态 chip，仅保留一个小型面板图标按钮；composer 去掉预设任务 chip，压窄宽度并改为 Codex-like 的轻量输入框和圆形发送按钮。
- 根据用户对 Codex composer 的进一步参考，将输入框宽度恢复为自适应的 `min(760px, calc(100% - 56px))`，去掉 `+` 和 `本地 workspace` 等底部杂项文案；发送按钮改为向上箭头 icon。
- 新增前端“帮我批准”模式：composer 中提供一个 Codex-like pill 开关；开启后，前端在检测到待审批工具调用时自动调用现有 approve API，已出现待审批时打开开关也会立即批准。该模式不绕过后端审批事件和审查记录。
- 将“帮我批准”从单一开关改为 Codex-like 权限模式选择器：点击后弹出菜单，可在“请求批准”和“帮我批准”之间切换；当前模式在 composer 底部以轻量 pill 展示。
- 根据用户截图修复 composer 权限菜单显示异常：最终生效 CSS 中 `composer-box` 改为允许 popover 溢出，避免菜单只露出一个选项；发送按钮不再使用 CSS 伪元素拼出的 `UiIcon arrow-up`，改为稳定文本箭头；权限按钮图标改为控件内局部 class，避免被全局 `.ui-icon` 规则污染。
- 根据用户进一步反馈，权限模式选择器改为纯文字显示：移除按钮和菜单项中的图标、对勾以及未使用的 arrow-up/approval 图标样式；当前选中模式只通过浅色背景表达。
- 将“变更待审查”提示从页面中部偏移改回贴近 composer 上沿的位置：最终 CSS 覆盖 `floating-change-chip bottom: 10px`，避免遮挡正文内容。
- 根据用户最新反馈，移除 timeline 中央悬浮的“1 个变更待审查”提示，不再在对话区重复展示右侧审查信息。
- 修复右侧审查面板纵向滚动：Inspector 外壳固定裁剪，`.inspector-body` 负责 `overflow-y: auto`，长审查列表和 diff 可在侧栏内上下滚动。
- 根据用户截图发现上一版滚动修复仍不够：Inspector 在主 grid 中未显式跨满全部行，导致右侧面板滚动边界仍可能被中间 composer 行影响。
- 进一步修复右侧滚动高度链路：Inspector 显式 `grid-row: 1 / -1`、`height: 100vh`、flex column 布局，header/tabs 固定，body 占剩余空间并独立纵向滚动。
- 清理所有遗留 `.floating-change-chip` 样式，确保“变更待审查”悬浮提示不会被后续样式复活。

## 2026-09-01：项目切换与任务删除

- 根据用户反馈修正左侧任务列表排序：`upsertRun` 仍更新任务状态，但最终按 `createdAt` 倒序排序，不再因为点击历史任务而把该任务移动到最上面。
- 新增本地项目管理能力：后端持久化 `workspace_projects`，启动时自动注册默认 workspace；前端左侧项目区可添加本地目录或创建新目录，并切换当前项目。
- `WorkspacePathResolver` 支持运行时切换 root；切换项目后，文件树、文件读取和后续 Agent 工具执行都会基于当前项目目录。
- 新增 task 删除能力：`DELETE /api/runs/{runId}` 会取消非终态任务并删除 run、事件、pending approval 和 undo snapshot；前端每条任务行提供删除按钮。
- 补充 API 测试覆盖：删除 run 后查询返回 404；添加并选择本地 project 后，workspace 文件列表来自新目录。

## 2026-09-02：本机项目选择与用户消息操作

- 根据用户反馈，将左侧“添加项目”从手动路径输入为主改为系统文件夹选择入口，样式更接近 Codex/ChatGPT 的本机文件夹 picker 流程。
- 新增后端 `FolderChooserService` 和 `POST /api/workspace/projects/choose-folder`：本地 macOS 环境通过 `osascript choose folder` 打开系统文件夹选择器，返回 POSIX path；用户取消时返回 `cancelled=true`。
- 前端新增 `chooseWorkspaceProjectFolder` API；选择文件夹后复用既有 `addWorkspaceProject` 逻辑添加并切换项目。手动路径输入保留为折叠兜底，用于 picker 不可用或需要新建目录时。
- 用户消息气泡新增轻量 hover 操作：`复制` 将消息写入剪贴板，`修改` 将原用户 prompt 放回 composer 并聚焦输入框，方便基于历史任务快速改写后重新运行。
- `ComposerBox` 暴露 `focus()`，App 层接收 timeline 的消息操作事件并更新 draft。
- 验证：`cd backend && mvn test` 通过 151 tests；`cd frontend && npm run build` 通过。

## 2026-09-02：H2 启动锁修复

- 根据用户反馈排查后端 `spring-boot:run` 的 build failure；`mvn clean test` 可通过，实际失败点在应用启动阶段。
- 启动日志显示 `backend/data/coding-agent.mv.db` 被旧 Java 进程锁住，H2 报 `Database may be already in use`，导致 Spring datasource 初始化失败并被 Maven 标记为 `BUILD FAILURE`。
- 默认 datasource 从普通 H2 file mode 调整为 `jdbc:h2:file:./data/coding-agent;AUTO_SERVER=TRUE`，允许本地开发中临时并发/残留实例共享同一个 H2 文件库，降低重启时锁库概率。
- 验证过程中发现 H2 2.3 不支持 `AUTO_SERVER=TRUE` 与 `DB_CLOSE_ON_EXIT=FALSE` 同时使用，因此最终移除 `DB_CLOSE_ON_EXIT=FALSE`。
- 停止残留锁库进程后，用 18080 和 18081 两个临时端口并发启动后端，两个实例均能启动；18081 健康检查返回 `{"status":"ok"}`。
