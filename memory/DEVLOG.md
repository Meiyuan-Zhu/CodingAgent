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
