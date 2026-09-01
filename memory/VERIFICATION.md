# 验证记录

记录实际执行的检查，区分文档核验、静态检查、核心测试、工具/API 集成、浏览器测试和真实模型任务。未执行不能写通过；模拟模型结果不计作真实模型能力。

每条记录包含日期、范围、方法/命令、结果、限制、相关决策，以及实际存在的代码版本或运行 ID。后续修改影响结果时追加新验证，或注明旧结果已过期，不静默沿用。

## DOC-001：开发文档组织核验

- 日期：2026-08-27。
- 类型：文档与忽略规则检查。
- 范围：AGENTS.md、decisions/、memory/、.gitignore。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、迁移后的文件存在性、旧目录引用与当前状态；使用 `git check-ignore --no-index --stdin -z` 在临时空仓库验证忽略规则，不初始化本项目仓库。
- 结果：通过。检查 10 个 Markdown 文件、27 个本地链接、18 个忽略规则样例；迁移后的文件存在，旧文件未重复保留，当前状态未声称已实现业务功能。
- 检查脚本修正：首轮按行解析 Git 输出时，中文路径被 Git 转义，导致误判题目 PDF 未被忽略；改用 NUL 分隔输入/输出后全部通过，忽略规则本身未修改。
- 关联：ADR-0001、ADR-0002。
- 代码版本/运行 ID：尚无；项目未初始化 Git，未运行应用。
- 限制：只验证文档组织和指定忽略样例，不证明应用可构建、可运行、模型可用或任意秘密都能被忽略规则识别。

## APP-001：前后端框架骨架验证

- 日期：2026-08-27。
- 类型：应用骨架构建与后端接口测试。
- 范围：Spring Boot 后端上下文启动、`GET /api/health` MockMvc JSON 响应、Vue 3 + TypeScript + Vite 生产构建、两个 dev server 同时运行后的 Vite 代理请求。
- 环境：Java 21.0.3、Maven 3.9.11、Node.js 20.19.0、npm 11.6.0。
- 命令与结果：
  - `cd backend && mvn test`：通过，2 tests, 0 failures, 0 errors。
  - `cd frontend && npm run build`：通过，`vue-tsc -b` 与 `vite build` 均成功。
  - `cd backend && mvn spring-boot:run`：通过，Tomcat started on port 8080。
  - `cd frontend && npm run dev -- --host 127.0.0.1`：通过，Vite ready at `http://127.0.0.1:5173/`。
  - `curl -fsS http://127.0.0.1:8080/api/health`：通过，返回 `status=ok`、`service=coding-agent-backend`、`javaVersion=21`。
  - `curl -fsS http://127.0.0.1:5173/api/health`：通过，经 Vite proxy 返回同类后端健康 JSON。
- 观察：
  - 初次 `mvn test` 使用 Spring Initializr 生成的 `4.1.1.RELEASE` 失败，本机 Maven 镜像未解析到该 parent POM。随后查询 Maven Central metadata 并改用 `3.5.16`。
  - Maven 每次读取用户全局 settings 时提示 `Unrecognised tag: 'repositories'`，目前不影响项目构建。
  - 前端构建时 Homebrew shellenv 输出 `/bin/ps: Operation not permitted`，目前不影响构建。
- 关联：ADR-0003。
- 代码版本/运行 ID：`3f220c9 chore: scaffold vue and spring boot apps`。
- 限制：不包含真实浏览器交互截图；不包含模型 API、Agent 循环、文件工具、命令执行或安全边界。

## DOC-002：框架接入后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、STATUS 中 Git/APP-001 状态、ADR-0003 索引。
- 结果：通过。检查 14 个 Markdown 文件。
- 关联：ADR-0003。
- 代码版本/运行 ID：`3f220c9 chore: scaffold vue and spring boot apps`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。

## UI-001：Codex-like 工作台壳验证

- 日期：2026-08-27。
- 类型：前端构建与浏览器视觉检查。
- 范围：`frontend/src/App.vue`、`frontend/src/style.css`。
- 方法：
  - 更新 Vue 页面为工作台式布局后执行 `cd frontend && npm run build`。
  - 使用 in-app browser 打开 `http://127.0.0.1:5173/`，检查 DOM 布局、控制台日志、横向溢出和当前截图。
- 结果：
  - 通过，`vue-tsc -b` 与 `vite build` 均成功。
  - 1280 x 720 视口下三栏存在，状态为 `ok`，`Run` 按钮处于禁用状态，浏览器 console 无 warning/error。
  - 发现并修复右侧长文件路径造成的横向溢出；修复后 `documentElement.scrollWidth` 等于视口宽度，`hasHorizontalOverflow=false`。
- 关联：ADR-0004。
- 代码版本/运行 ID：`8fc625b feat: shape codex-like workbench shell`。
- 限制：未做多尺寸响应式截图核验；未实现任务提交接口、SSE、文件树真实数据、Diff 真实数据或 Agent 执行。

## DOC-003：工作台界面决策后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0004 索引、STATUS 对 ADR-0004 的引用、UI-001 浏览器检查记录。
- 结果：通过。检查 15 个 Markdown 文件。
- 关联：ADR-0004。
- 代码版本/运行 ID：`8fc625b feat: shape codex-like workbench shell`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。

## CORE-001：Agent 运行协议领域模型测试

- 日期：2026-08-27。
- 类型：后端核心单元测试。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/run/` 与对应测试。
- 方法：实现运行 ID、状态、结束原因、事件、工具调用、工具结果和运行状态转换后执行 `cd backend && mvn test`。
- 结果：通过。12 tests, 0 failures, 0 errors；其中新增 10 个运行协议相关测试，原有 2 个 Spring Boot 骨架测试仍通过。
- 覆盖：
  - 正常创建、启动、事件递增和成功结束。
  - 等待审批与审批后恢复。
  - 非法状态转换拒绝。
  - 失败运行必须携带失败原因和错误消息。
  - 终态必须有 `StopReason`。
  - `RunEvent` payload 与 `ToolCall` arguments 构造后不可被外部 Map 修改。
  - `ToolResult` 区分成功和失败。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；Mockito/ByteBuddy 动态 agent 也有 JDK 未来兼容警告，目前不影响测试。
- 关联：ADR-0005。
- 代码版本/运行 ID：`e0369eb feat: add agent run protocol domain`。
- 限制：不包含并发运行、持久化、SSE、真实工具执行、模型调用、模型响应解析或前端接入验证。

## DOC-004：运行协议子任务后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0005 索引、STATUS 对 ADR-0005 的引用、CORE-001 验证记录。
- 结果：通过。检查 16 个 Markdown 文件。
- 关联：ADR-0005。
- 代码版本/运行 ID：`e0369eb feat: add agent run protocol domain`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。

## WORKSPACE-001：Workspace 边界与只读文件工具测试

- 日期：2026-08-27。
- 类型：后端核心单元测试与 Spring 上下文测试。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/`、`workspaces/demo/`、`.gitignore`、`application.properties`。
- 方法：实现 workspace 路径解析、拒绝规则和只读工具后执行 `cd backend && mvn test`。
- 结果：通过。21 tests, 0 failures, 0 errors。
- 覆盖：
  - 相对路径 normalize 后仍在 workspace 内。
  - 绝对路径和 `..` 路径逃逸被拒绝。
  - `.env` 与嵌套 `.env.local` 被拒绝。
  - symlink 指向 workspace 外部时被拒绝。
  - `listFiles` 隐藏敏感文件。
  - `readFile` 读取 UTF-8 文本，拒绝目录和非法 UTF-8。
  - `searchText` 能找到匹配，跳过敏感文件，并在结果上限处标记截断。
  - Spring Boot 上下文能加载 workspace 配置。
- 修正记录：
  - 首次编译失败：误用 `Path.stream()`；改为遍历 `Path`。
  - 首次测试失败：macOS 临时目录 realpath 中 `/var` 与 `/private/var` 不一致，导致误判 symlink 逃逸；改为 workspace root 也使用 `toRealPath()`。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；Mockito/ByteBuddy 动态 agent 仍有 JDK 未来兼容警告，目前不影响测试。
- 关联：ADR-0006。
- 代码版本/运行 ID：`93f572f feat: add workspace boundary and read tools`。
- 限制：不包含 HTTP API、SSE、模型工具注册表、真实 Agent loop、写入工具、编辑工具或命令工具验证。

## DOC-005：Workspace 子任务后的文档与忽略规则检查

- 日期：2026-08-27。
- 类型：文档链接、状态一致性与 Git 忽略规则检查。
- 范围：README.md、decisions/、memory/、`.gitignore`、`workspaces/demo/`。
- 方法：
  - 使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0006 索引、STATUS 对 ADR-0006 和 WORKSPACE-001 的引用。
  - 使用 `git status --short --ignored` 检查本阶段改动与忽略文件范围。
  - 使用 `git check-ignore -v -- .vscode/settings.json workspaces/private/a.txt workspaces/demo/README.md 推免考核题目学生版.pdf backend/target/classes/x frontend/node_modules/x frontend/dist/x tmp/x` 检查关键忽略/反忽略样例。
  - 使用 `git ls-files --others --exclude-standard workspaces/demo` 确认 demo workspace 文件会进入待跟踪列表。
- 结果：通过。检查 18 个 Markdown 文件；`.vscode/`、私有 `workspaces/`、题目 PDF、构建产物和 `tmp/` 保持忽略，`workspaces/demo/README.md` 与 `workspaces/demo/src/hello.txt` 可被跟踪。
- 关联：ADR-0006。
- 代码版本/运行 ID：`93f572f feat: add workspace boundary and read tools`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档、状态描述和指定 Git 忽略样例，不证明应用运行、模型调用或工具集成能力。

## TOOLREG-001：工具注册表与 workspace 工具适配测试

- 日期：2026-08-27。
- 类型：后端核心单元测试与 Spring 上下文测试。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/`、`backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/`。
- 方法：实现工具定义、注册表、参数校验、执行结果归一化和 workspace 只读工具适配后执行 `cd backend && mvn test`。
- 结果：通过。39 tests, 0 failures, 0 errors。
- 覆盖：
  - 工具定义名称和说明校验。
  - 工具 schema 深拷贝，不被外部修改。
  - 注册表按工具名稳定排序。
  - 重复工具名拒绝。
  - 已知工具执行成功并补充基础 metadata。
  - 未知工具返回失败结果。
  - 参数错误返回 `INVALID_ARGUMENTS`。
  - workspace 路径拒绝返回 `WORKSPACE_ACCESS_DENIED`。
  - 未预期运行时异常返回 `TOOL_RUNTIME_ERROR`，不把内部异常消息直接暴露给模型。
  - `list_files`、`read_file`、`search_text` 经注册表返回 JSON 文本结果。
  - Spring Boot 上下文能加载 `ToolRegistry` 和三个 workspace 工具。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；Mockito/ByteBuddy 动态 agent 仍有 JDK 未来兼容警告，目前不影响测试。
- 关联：ADR-0007。
- 代码版本/运行 ID：`38c3091 feat: add agent tool registry`。
- 限制：不包含 HTTP API、SSE、模型适配器、真实 Agent loop、写入工具、编辑工具或命令工具验证。

## DOC-006：工具注册表子任务后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0007 索引、STATUS 对 ADR-0007 和 TOOLREG-001 的引用。
- 结果：通过。检查 19 个 Markdown 文件。
- 关联：ADR-0007。
- 代码版本/运行 ID：`38c3091 feat: add agent tool registry`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。

## RUNAPI-001：Run API、事件回看、SSE 与 mock runner 验证

- 日期：2026-08-27。
- 类型：后端核心单元测试、Spring MVC 测试、本地 HTTP/SSE 集成测试。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/`、`backend/src/main/java/com/zhumeiyuan/codingagent/agent/api/`、`WorkspaceProperties` 配置绑定。
- 方法：
  - 实现 run store、run service、mock runner、SSE stream 和 run controller 后执行 `cd backend && mvn test`。
  - 执行 `cd backend && mvn spring-boot:run` 启动真实后端。
  - 使用本地 HTTP 请求 `POST /api/runs` 创建 run，随后请求 `GET /api/runs/{runId}` 和 `GET /api/runs/{runId}/events`。
  - 使用本地 HTTP 请求 `GET /api/runs/{runId}/events/stream` 检查 SSE replay 文本格式。
- 结果：
  - `mvn test` 通过。48 tests, 0 failures, 0 errors。
  - 真实后端启动通过，Tomcat started on port 8080。
  - `POST /api/runs` 返回 HTTP 202。
  - 测试 run `23076b47-df28-4e33-ae06-3f7ba709d313` 最终状态为 `SUCCEEDED`。
  - 事件回看返回 10 条：`RUN_CREATED`、`USER_MESSAGE_ACCEPTED`、`RUN_STARTED`、`MODEL_REQUESTED`、`MODEL_MESSAGE_RECEIVED`、`TOOL_CALL_REQUESTED`、`TOOL_CALL_STARTED`、`TOOL_CALL_FINISHED`、`MODEL_MESSAGE_RECEIVED`、`RUN_FINISHED`。
  - `TOOL_CALL_FINISHED` 显示 `list_files` 成功，并返回 `README.md` 与 `src`。
  - SSE replay 样例 run `bb8511f5-6fe8-4d08-b636-4a3624ee1330` 返回 `id/event/data` 格式，并包含 `event:run_finished`。
- 修正记录：
  - 首次真实 `spring-boot:run` 失败：`WorkspaceProperties` 直接绑定 `Path` 时，Spring Boot 将 `../workspaces/demo` 作为资源路径转换并失败；改为配置字段使用 `String`，在 getter 中由项目代码 `Path.of(...)` 转换。
  - 8080 和 5173 存在旧开发进程，联调前使用 `lsof` 定位并清理旧 Java/Vite 进程。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；Mockito/ByteBuddy 动态 agent 仍有 JDK 未来兼容警告，目前不影响测试。
- 关联：ADR-0008。
- 代码版本/运行 ID：`6bddeed feat: wire mock run api and event stream`。
- 限制：mock runner 不证明真实模型 API、真实响应解析、多轮推理、写入工具、命令执行、取消或审批能力。

## UI-002：Vue 工作台接入 run API 与 SSE 验证

- 日期：2026-08-27。
- 类型：前端构建与浏览器交互验证。
- 范围：`frontend/src/App.vue`、`frontend/src/api/runs.ts`、Vite `/api` proxy、后端 run/SSE API。
- 方法：
  - 执行 `cd frontend && npm run build`。
  - 启动后端 `mvn spring-boot:run`，启动前端 `npm run dev -- --host 127.0.0.1`。
  - 使用 in-app browser 打开 `http://127.0.0.1:5174/`，检查初始状态、点击 Run、等待 timeline 出现 `Run finished`，读取 console warnings/errors 与页面宽度。
- 结果：
  - `npm run build` 通过，`vue-tsc -b` 与 `vite build` 均成功。
  - 初始页面健康状态为 `ok`，Run 按钮可用。
  - 点击 Run 后页面显示 10 条事件，包含 `Tool finished: list_files` 与 `Run finished`。
  - 右侧文件列表从工具结果显示 `README.md` 和 `src`。
  - 浏览器 console 无 warning/error。
  - 1280 视口下 `documentElement.scrollWidth` 等于 `clientWidth`，没有横向溢出。
- 观察：5173 被旧 Vite 进程占用，当前验证使用 Vite 自动分配的 `http://127.0.0.1:5174/`。前端构建仍出现 Homebrew shellenv 的 `/bin/ps: Operation not permitted`，不影响构建。
- 关联：ADR-0008。
- 代码版本/运行 ID：`6bddeed feat: wire mock run api and event stream`。
- 限制：只验证 mock run UI 闭环；未验证真实模型、多轮任务、写入/diff、取消、审批或移动端响应式。

## DOC-007：Run API 与 SSE 子任务后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0008 索引、STATUS 对 ADR-0008、RUNAPI-001 和 UI-002 的引用。
- 结果：通过。检查 20 个 Markdown 文件。
- 关联：ADR-0008。
- 代码版本/运行 ID：`6bddeed feat: wire mock run api and event stream`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。

## WRITE-001：Workspace 写入与文本编辑工具测试

- 日期：2026-08-27。
- 类型：后端核心单元测试与 Spring 上下文测试。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/workspace/`、`backend/src/main/java/com/zhumeiyuan/codingagent/agent/tool/workspace/`。
- 方法：实现写入目标路径解析、`WorkspaceWriteTools`、`write_file`/`replace_text` 工具适配后执行 `cd backend && mvn test`。
- 结果：通过。64 tests, 0 failures, 0 errors。
- 覆盖：
  - 缺失目标文件可在已存在父目录内解析为写入目标。
  - 缺失父目录和敏感 `.env` 写入被拒绝。
  - `writeFile` 可创建 UTF-8 文件并返回 SHA-256。
  - 已存在文件在 `overwrite=false` 时被拒绝。
  - `writeFile` 支持 `expected_sha256`，hash 不匹配返回冲突。
  - 过大写入、敏感路径和已有非法 UTF-8 文件被拒绝。
  - `replaceText` 可做精确文本替换，也支持空 replacement 删除文本。
  - `replaceText` 对缺失文本、hash 冲突、目录、非法 UTF-8 和过多替换上限进行拒绝。
  - `write_file`、`replace_text` 经注册表返回 JSON 文本结果。
  - 文件已存在/内容冲突映射为 `WORKSPACE_CONFLICT`；替换文本缺失映射为 `WORKSPACE_EDIT_MISS`。
  - Spring Boot 上下文能加载五个 workspace 工具：`list_files`、`read_file`、`search_text`、`write_file`、`replace_text`。
- 修正记录：
  - 首次新增 resolver 写目标测试时，预期路径未使用 realpath，macOS `/var` 与 `/private/var` 差异导致断言失败；改为用 `root.toRealPath()` 构造预期。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；Mockito/ByteBuddy 动态 agent 仍有 JDK 未来兼容警告，目前不影响测试。
- 关联：ADR-0009。
- 代码版本/运行 ID：`8cc17f9 feat: add workspace write and edit tools`。
- 限制：不包含真实模型 API、真实 Agent loop 写入决策、前端 diff 渲染、用户审批、取消或命令工具验证。

## DOC-008：写入/编辑工具子任务后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0009 索引、STATUS 对 ADR-0009 和 WRITE-001 的引用。
- 结果：通过。检查 21 个 Markdown 文件。
- 关联：ADR-0009。
- 代码版本/运行 ID：`8cc17f9 feat: add workspace write and edit tools`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。


## MODEL-001：模型边界与响应解析器测试

- 日期：2026-08-27。
- 类型：后端核心单元测试与 Spring 上下文测试。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/`、`backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java`。
- 方法：实现模型请求/响应边界、JSON 响应解析器、mock 模型客户端和 runner 接入后执行 `cd backend && mvn test`。
- 结果：通过。78 tests, 0 failures, 0 errors。
- 覆盖：
  - `ModelRequest` 拒绝空消息，并能取得最后一条 user message。
  - `ModelResponseParser` 可解析 `stop` 响应和 `tool_calls` 响应。
  - 非 JSON、非对象根、未知 finish reason、`stop` 携带工具调用、缺失或非对象 arguments、重复 tool call id、过多 tool calls 均被拒绝。
  - `HeuristicMockModelClient` 对 README、搜索和默认 prompt 生成确定性的工具调用，并经过 parser。
  - `MockAgentRunner` 通过 `ModelClient` 执行工具，工具失败以 `TOOL_ERROR` 结束，模型解析失败以 `MODEL_PARSE_ERROR` 结束。
  - Spring Boot 上下文能加载 `ModelClient`、`ModelResponseParser`、`ToolRegistry` 和 run API 相关 bean。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；Mockito/ByteBuddy 动态 agent 仍有 JDK 未来兼容警告，目前不影响测试。
- 关联：ADR-0010。
- 代码版本/运行 ID：`a58e31b feat: add model response parsing boundary`。
- 限制：不包含真实模型 API、真实 provider-native tool calling、多轮 Agent loop、上下文裁剪、预算、取消、审批或命令工具验证。


## DOC-009：模型边界子任务后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0010 索引、STATUS 对 ADR-0010 和 MODEL-001 的引用、MODEL-001 代码版本、DEVLOG 子任务 6 记录。
- 结果：通过。检查 22 个 Markdown 文件。
- 关联：ADR-0010。
- 代码版本/运行 ID：`a58e31b feat: add model response parsing boundary`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。


## LOOP-001：多轮 Agent loop 与预算验证

- 日期：2026-08-27。
- 类型：后端核心单元测试、Spring 上下文测试、前端构建、本地 HTTP 集成测试。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunBudget.java`、`backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java`、`backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/HeuristicMockModelClient.java`、`frontend/src/App.vue`。
- 方法：实现多轮模型/工具循环、预算限制、上下文消息窗口和 mock 模型 stop 行为后执行以下检查：
  - `cd backend && mvn test`。
  - `cd frontend && npm run build`。
  - `cd backend && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=18080` 启动后端。
  - 使用本地 HTTP 请求 `POST /api/runs` 创建 run，并查询 `GET /api/runs/{runId}` 与 `GET /api/runs/{runId}/events`。
- 结果：通过。
  - 后端测试：85 tests, 0 failures, 0 errors。
  - 前端构建：`vue-tsc -b` 与 `vite build` 均成功。
  - 本地 HTTP run `2bbde4a4-237e-45ab-828a-4568ec636e43` 最终状态 `SUCCEEDED`，stopReason 为 `COMPLETED`。
  - 事件共 11 条，其中 `MODEL_REQUESTED` 2 条、`TOOL_CALL_FINISHED` 1 条，`RUN_FINISHED` payload 包含 `roundsUsed=2`、`toolCallsUsed=1`。
- 覆盖：
  - 多轮成功：第一轮模型请求工具，工具结果回填后第二轮模型返回 `STOP`。
  - 工具失败以 `TOOL_ERROR` 结束。
  - 模型解析失败以 `MODEL_PARSE_ERROR` 结束。
  - 模型 `LENGTH` finish reason 以 `TOKEN_BUDGET_LIMIT` 结束。
  - 模型持续请求工具时以 `ROUND_LIMIT` 结束。
  - 工具调用数超过预算时以 `TOOL_CALL_LIMIT` 结束，并在执行前停止。
  - 上下文窗口保留 system prompt 和最近消息。
- 观察：首次本地 HTTP 请求在沙箱内因 `Operation not permitted` 被拒绝；按权限规则使用提升权限访问本机 `127.0.0.1:18080` 后验证通过。Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；前端构建仍出现 Homebrew shellenv 的 `/bin/ps: Operation not permitted`，均不影响本次结果。
- 关联：ADR-0011。
- 代码版本/运行 ID：`2b06ac8 feat: add bounded agent loop`。
- 限制：仍未验证真实模型 API、真实 token 计数、取消、超时、审批、命令执行或浏览器交互。


## DOC-010：多轮 loop 子任务后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0011 索引、STATUS 对 ADR-0011 和 LOOP-001 的引用、LOOP-001 验证记录、DEVLOG 子任务 7 记录。
- 结果：通过。检查 23 个 Markdown 文件。
- 关联：ADR-0011。
- 代码版本/运行 ID：`2b06ac8 feat: add bounded agent loop`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。


## LIFE-001：取消、超时与后台任务生命周期验证

- 日期：2026-08-27。
- 类型：后端核心单元测试、Spring MVC 测试、前端构建、本地 HTTP 集成测试。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/RunTaskManager.java`、`AgentRunService`、`MockAgentRunner`、`RunBudget`、`RunController`、`frontend/src/App.vue`、`frontend/src/api/runs.ts`。
- 方法：实现 run task 管理、cancel endpoint、runner 取消检查、工具 timeout 和前端 Cancel 按钮后执行以下检查：
  - `cd backend && mvn test`。
  - `cd frontend && npm run build`。
  - `cd backend && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=18080` 启动后端。
  - 使用本地 HTTP 请求 `POST /api/runs` 创建 run，随后请求 `POST /api/runs/{runId}/cancel`，再查询 run 状态和事件回看。
- 结果：通过。
  - 后端测试：94 tests, 0 failures, 0 errors。
  - 前端构建：`vue-tsc -b` 与 `vite build` 均成功。
  - 本地 HTTP run `844f0b97-1a46-4955-bf44-2bc47e8b2802` 的 cancel response status 为 `CANCELLED`，最终状态 `CANCELLED`，stopReason 为 `USER_CANCELLED`。
  - 事件共 9 条，其中 `RUN_CANCELLING` 1 条、`RUN_FINISHED` 1 条；`RUN_FINISHED` payload 包含 `interruptRequested=true`。
- 覆盖：
  - `RunTaskManager` 可跟踪 active task，完成后清理，并用 interrupt 取消 active task。
  - `AgentRunService.cancelRun` 可将非终态 run 转为 `CANCELLED / USER_CANCELLED` 并发事件；终态取消保持幂等。
  - `POST /api/runs/{runId}/cancel` endpoint 可返回 run state。
  - `MockAgentRunner` 启动前遇到 `CANCELLING` 会跳过模型并完成取消。
  - 工具执行超时会返回 `TOOL_TIMEOUT` metadata，并让 run 以 `TIME_LIMIT` 失败。
  - 前端 TypeScript 构建覆盖 cancel API 封装、Cancel 按钮和 `run_cancelling` 事件订阅。
- 修正记录：
  - 首轮 runner 取消测试发现启动前 `CANCELLING` 状态只返回、不落到 `CANCELLED`；已修复为完成取消。
  - 首轮任务管理器测试等待 future inactive 太早，future 已标记取消但线程尚未处理 interrupt；测试改为等待 interrupt 标记。
  - 一次复核命令因工作目录写错导致 Python 补丁未执行，随后 Maven 在沙箱中触发 Mockito/ByteBuddy attach 错误；修正路径后普通 `mvn test` 通过。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；前端构建仍出现 Homebrew shellenv 的 `/bin/ps: Operation not permitted`，均不影响本次结果。
- 关联：ADR-0012。
- 代码版本/运行 ID：`e9fdc0c feat: add run cancellation and tool timeouts`。
- 限制：取消当前依赖 Java 线程中断；不证明未来 shell 命令子进程会被杀掉。仍未验证真实模型 API、审批策略、命令执行或浏览器交互。


## DOC-011：取消与超时子任务后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0012 索引、STATUS 对 ADR-0012 和 LIFE-001 的引用、LIFE-001 验证记录、DEVLOG 子任务 8 记录。
- 结果：通过。检查 24 个 Markdown 文件。
- 关联：ADR-0012。
- 代码版本/运行 ID：`e9fdc0c feat: add run cancellation and tool timeouts`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。

## CHANGE-001：审批策略与 diff/变更展示验证

- 日期：2026-08-27。
- 类型：后端核心单元测试、Spring MVC 测试、前端构建、本地 HTTP 集成测试。
- 范围：`ToolApprovalPolicy`、`MockAgentRunner` 审批拦截、`WorkspaceUnifiedDiff`、`WorkspaceWriteTools` diff 返回、`HeuristicMockModelClient` 写入触发、`frontend/src/App.vue` 和 `frontend/src/style.css`。
- 方法：实现审批策略、审批事件、写入/替换 diff 元数据和前端 Diff 面板后执行以下检查：
  - `cd backend && mvn test`。
  - `cd frontend && npm run build`。
  - `cd backend && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=18080` 启动后端。
  - 使用本地 HTTP 请求 `POST /api/runs`，prompt 为 `please write a note`，随后查询 run 状态与事件回看。
- 结果：通过。
  - 后端测试：98 tests, 0 failures, 0 errors。
  - 前端构建：`vue-tsc -b` 与 `vite build` 均成功。
  - 本地 HTTP run `68bb2c0d-b17f-4d5a-8219-d725451f1f68` 最终状态 `FAILED`，stopReason 为 `APPROVAL_REJECTED`。
  - 事件序列为 `RUN_CREATED`、`USER_MESSAGE_ACCEPTED`、`RUN_STARTED`、`MODEL_REQUESTED`、`MODEL_MESSAGE_RECEIVED`、`TOOL_CALL_REQUESTED`、`APPROVAL_REQUIRED`、`APPROVAL_RESOLVED`、`RUN_FINISHED`。
  - 事件中没有 `TOOL_CALL_STARTED`，说明需审批的 `write_file` 未被执行。
- 覆盖：
  - 审批策略对 `read_file` 自动通过，对 `write_file`、`replace_text` 和 `run_command` 要求用户审批。
  - Runner 遇到可变更工具时进入审批事件路径，并安全失败，不执行工具 handler。
  - `write_file` 创建文件时返回 unified diff。
  - `replace_text` 编辑文件时返回包含删除行、添加行和上下文行的 unified diff。
  - `write_file`、`replace_text` 经工具注册表返回的 JSON content 包含 `unifiedDiff`。
  - Mock 模型可根据写入类 prompt 触发 `write_file`，用于演示审批拦截。
  - 前端 TypeScript 构建覆盖审批事件订阅、事件标题和 Diff 面板解析展示逻辑。
- 修正记录：
  - 一次补丁命令以 `backend/` 为工作目录时误带 `backend/` 路径前缀，导致补丁未写入；随后从仓库根目录重新执行并验证。
  - 首次 Maven 复核在受限沙箱中触发 Mockito/ByteBuddy self-attach 错误；修正路径后普通 `mvn test` 通过。
  - 本地 HTTP 请求在沙箱内因 `Operation not permitted` 被拒绝；按权限规则使用提升权限访问 `localhost:18080` 后验证通过。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；前端构建仍出现 Homebrew shellenv 的 `/bin/ps: Operation not permitted`，均不影响本次结果。
- 关联：ADR-0013。
- 代码版本/运行 ID：`afe23a3 feat: add approval policy and diff display`；HTTP run `68bb2c0d-b17f-4d5a-8219-d725451f1f68`。
- 限制：当前审批路径是安全拦截，不是完整 approve/reject/resume 工作流；未验证真实浏览器视觉交互、真实模型 API、命令工具或进程级命令取消。

## DOC-012：审批与 diff 子任务后的文档一致性检查

- 日期：2026-08-27。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0013 索引、STATUS 对 ADR-0013 和 CHANGE-001 的引用、CHANGE-001 验证记录、DEVLOG 子任务 9 记录。
- 结果：通过。检查 25 个 Markdown 文件；未发现本地链接缺失、代码围栏未闭合、ADR-0013/CHANGE-001/DEVLOG 子任务 9 引用缺失。
- 关联：ADR-0013。
- 代码版本/运行 ID：`afe23a3 feat: add approval policy and diff display`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。

## APPROVAL-001：完整审批恢复工作流验证

- 日期：2026-08-28。
- 类型：后端核心单元测试、Spring MVC 测试、前端构建、本地 HTTP 集成测试。
- 范围：`PendingToolApproval`、`AgentRunStore` pending approval 管理、`AgentRunService` approve/reject、`MockAgentRunner` 审批后恢复、`RunController` approve/reject endpoint、`RunTaskManager` 已完成任务替换、`frontend/src/App.vue` 和 `frontend/src/api/runs.ts`。
- 方法：实现完整 approve/reject/resume 工作流后执行以下检查：
  - `cd backend && mvn test`。
  - `cd frontend && npm run build`。
  - `cd backend && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=18080` 启动后端。
  - 使用本地 HTTP 请求 `POST /api/runs`，prompt 为 `please write a note`，等待 run 到 `WAITING_FOR_APPROVAL`。
  - 调用 `POST /api/runs/{runId}/approvals/mock-call-1/approve`，再查询 run 状态和事件回看。
  - 删除验证过程中创建的 `workspaces/demo/src/mock-note.txt`。
- 结果：通过。
  - 后端测试：101 tests, 0 failures, 0 errors。
  - 前端构建：`vue-tsc -b` 与 `vite build` 均成功。
  - 本地 HTTP run `fd830860-d275-4484-8fbc-249a51142722` 先进入 `WAITING_FOR_APPROVAL`，approve response 返回 `RUNNING`，最终状态 `SUCCEEDED`，stopReason 为 `COMPLETED`。
  - 事件序列为 `RUN_CREATED`、`USER_MESSAGE_ACCEPTED`、`RUN_STARTED`、`MODEL_REQUESTED`、`MODEL_MESSAGE_RECEIVED`、`TOOL_CALL_REQUESTED`、`APPROVAL_REQUIRED`、`APPROVAL_RESOLVED`、`TOOL_CALL_STARTED`、`TOOL_CALL_FINISHED`、`MODEL_REQUESTED`、`MODEL_MESSAGE_RECEIVED`、`RUN_FINISHED`。
  - `TOOL_CALL_FINISHED` 的 content 包含 `unifiedDiff`，其中有 `+mock note`。
  - 验证创建的 `workspaces/demo/src/mock-note.txt` 已删除。
- 覆盖：
  - Runner 遇到可变更工具会停在 `WAITING_FOR_APPROVAL`，审批前不执行工具。
  - Approve 会恢复同一 run，执行 pending tool，把工具结果回填上下文，并继续下一轮模型请求。
  - Reject 会以 `FAILED / APPROVAL_REJECTED` 结束，且不执行 pending tool。
  - Controller 暴露 reject endpoint，并能返回失败后的 run state。
  - 前端 TypeScript 构建覆盖 approve/reject API 封装、pending approval 面板、按钮状态和审批后 diff 展示路径。
- 修正记录：
  - 首轮实现后补充了 `RunTaskManager.start` 对已完成旧 task 的替换逻辑，降低审批恢复时旧 task 清理尚未完成的竞态风险。
  - 一次复核 Maven 在受限沙箱触发 Mockito/ByteBuddy self-attach 错误；后续普通 `mvn test` 通过。
  - 本地 HTTP 请求需要访问 `localhost:18080`，按权限规则使用提升权限执行。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；前端构建仍出现 Homebrew shellenv 的 `/bin/ps: Operation not permitted`，均不影响本次结果。
- 关联：ADR-0014。
- 代码版本/运行 ID：`736aa7e feat: add approval resume workflow`；HTTP run `fd830860-d275-4484-8fbc-249a51142722`。
- 限制：pending approval 仍保存在进程内存，后端重启后无法恢复等待审批的 run；当前只覆盖串行工具模型下的单个 pending approval；未验证真实浏览器点击、真实模型 API、命令工具或进程级命令取消。

## DOC-013：完整审批恢复子任务后的文档一致性检查

- 日期：2026-08-28。
- 类型：文档链接与状态一致性检查。
- 范围：README.md、decisions/、memory/、前后端生成文档中的 Markdown 文件。
- 方法：使用 Python 标准库内联脚本检查本地 Markdown 链接、代码围栏配对、ADR-0014 索引、ADR-0013 superseded 状态、STATUS 对 ADR-0014 和 APPROVAL-001 的引用、APPROVAL-001 验证记录、DEVLOG 子任务 10 记录。
- 结果：通过。检查 26 个 Markdown 文件；未发现本地链接缺失、代码围栏未闭合、ADR-0014 索引缺失、ADR-0013 superseded 状态缺失、STATUS/DEVLOG/VERIFICATION 关键记录缺失。
- 关联：ADR-0014。
- 代码版本/运行 ID：`736aa7e feat: add approval resume workflow`；本条哈希信息由后续文档同步提交补充。
- 限制：只检查文档结构和状态关键词，不证明应用功能。

## MODELAPI-001：OpenAI-compatible DeepSeek adapter 测试

- 日期：2026-08-28。
- 类型：后端模型适配器单元测试、Spring 上下文测试、前端构建检查。
- 范围：`backend/src/main/java/com/zhumeiyuan/codingagent/agent/model/`、`backend/src/main/java/com/zhumeiyuan/codingagent/agent/execution/MockAgentRunner.java`、`backend/src/main/resources/application.properties`、`frontend/src/App.vue`。
- 方法：
  - 新增 OpenAI-compatible 模型客户端与配置后执行 `cd backend && mvn test`。
  - 使用替身 `ModelHttpTransport` 捕获请求体和 headers，模拟 provider 返回 Chat Completions JSON。
  - 执行 `cd frontend && npm run build`，确认前端文案调整不破坏构建。
  - 启动 `cd backend && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=18080 --agent.model.provider=openai-compatible --agent.model.name=deepseek-v4-flash"`，确认后端可用 DeepSeek 配置启动。
- 结果：
  - 后端 `mvn test` 通过。106 tests, 0 failures, 0 errors。
  - 新增模型适配器测试覆盖：请求发送到 `https://api.deepseek.com/chat/completions`、Authorization header 使用环境变量值、请求 model 为 `deepseek-v4-flash`、启用 `response_format=json_object`、工具定义进入 system message、tool observation 被映射为普通 user message、缺失 API key 明确失败、provider HTTP error 明确失败、provider 响应缺少 content 明确失败。
  - 前端 `npm run build` 通过，`vue-tsc -b` 与 `vite build` 均成功。
  - 后端以 `openai-compatible` 和 `deepseek-v4-flash` 配置启动成功，Tomcat started on port 18080。
- 未执行：真实 DeepSeek 端到端 run 未执行。尝试发起本地 run 验证时，安全审查指出该请求会把 workspace 查询、工具定义和后续工具结果发送给 DeepSeek 外部服务；用户尚未明确授权这些数据出境，因此停止验证。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；前端构建仍出现 Homebrew shellenv 的 `/bin/ps: Operation not permitted`，均未阻止验证。
- 关联：ADR-0015。
- 代码版本/运行 ID：`60aeba7 feat: add openai compatible model adapter`。
- 限制：替身 HTTP 测试证明适配器请求/解析边界，不证明 DeepSeek 真实服务已可用；启动成功不证明真实模型返回可被项目解析。


## REALMODEL-001：DeepSeek V4 Flash 真实只读 run 验证

- 日期：2026-08-28。
- 类型：真实模型 API 端到端验证、后端回归测试。
- 用户授权：用户明确授权向 DeepSeek 发送 demo workspace 测试上下文，执行一次真实模型验证。
- 范围：`OpenAiCompatibleModelClient`、`ModelResponseParser`、`MockAgentRunner`、run API、工具注册表、`list_files`、`read_file`、demo workspace。
- 方法：
  - 使用 `source ~/.zshrc` 读取本机 `DEEPSEEK_API_KEY`，未打印 key。
  - 启动后端：`cd backend && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=18080 --agent.model.provider=openai-compatible --agent.model.name=deepseek-v4-flash"`。
  - 通过本地 HTTP `POST /api/runs` 创建只读任务，请求模型查看 demo workspace 文件并用中文总结。
  - 轮询 `GET /api/runs/{runId}`，并读取 `GET /api/runs/{runId}/events` 检查事件链。
  - 根据验证中暴露的问题，收紧模型 message 校验、增加 `MODEL_ERROR` 分类、关闭 DeepSeek thinking mode 后重新执行后端测试与真实 run。
- 结果：
  - 后端 `mvn test` 通过。108 tests, 0 failures, 0 errors。
  - 成功 run：`eade6508-edad-476f-986c-5d48ef18458a`，最终状态 `SUCCEEDED`，结束原因 `COMPLETED`。
  - 事件链包含三轮 `MODEL_REQUESTED`，provider 均为 `openai-compatible:deepseek-v4-flash`。
  - 工具调用成功：第一轮 `list_files` 返回 `README.md` 与 `src`；第二轮 `read_file` 读取 `README.md`。
  - 最终模型消息为中文总结：该 demo workspace 是用于开发本地编码代理的小型安全工作区，包含 `README.md` 和 `src`，后端将其作为开发期间 agent 工具唯一可检查的文件区域；未执行写入或修改。
- 修正记录：
  - run `7e68ea76-0b04-4381-bf9c-90506eb6fb0b` 初次完成真实模型与工具闭环，但最终 `STOP` message 为空，不能作为完整用户体验验收。
  - run `02719d2a-1c5a-4254-8479-77d73a2b68d6` 在收紧 message 校验后失败为 `INTERNAL_ERROR`，暴露 provider 错误分类不足。
  - run `bc19391d-aa97-427b-a924-40fbac1d5ebc` 在新增 `MODEL_ERROR` 分类后失败原因变为 `Model provider response does not contain choices[0].message.content`。
  - 查阅 DeepSeek 官方 thinking mode 文档后，发现 V4 默认开启 thinking mode；当前项目不保存 `reasoning_content`，因此显式关闭 thinking mode。
- 安全记录：本验证只使用 demo workspace；未读取或发送题目 PDF、`.zshrc` 内容、私有 workspace 或真实 API key。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；不影响验证。
- 关联：ADR-0015。
- 代码版本/运行 ID：`8f3dd6f feat: verify deepseek flash model loop`。
- 限制：真实写入审批、取消、超时、命令工具和前端浏览器 UI 下的 DeepSeek run 尚未验证；不证明 DeepSeek V4 Pro 或其他 provider 可用。

## COMMAND-001：Workspace 命令执行工具验证

- 日期：2026-08-28。
- 类型：后端核心单元测试、Spring 上下文测试、本地 HTTP 审批集成测试。
- 范围：`WorkspaceCommandTools`、`CommandExecutionResult`、`WorkspaceToolFactory` 的 `run_command` 适配、`ToolArgumentReader` 字符串数组校验、`ToolApprovalPolicy`、`HeuristicMockModelClient`、`AgentRunService` 取消幂等修复。
- 方法：实现命令工具后执行以下检查：
  - `cd backend && mvn test`。
  - `cd backend && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=18080` 启动后端。
  - 使用本地 HTTP 请求 `POST /api/runs`，prompt 为 `please run tests`，等待 run 到 `WAITING_FOR_APPROVAL`。
  - 读取事件，确认审批工具为 `run_command`，参数为 `{"command":["/bin/echo","mock command"],"cwd":"."}`。
  - 调用 `POST /api/runs/{runId}/approvals/{toolCallId}/approve`，再查询 run 状态和事件回看。
- 结果：通过。
  - 后端测试：118 tests, 0 failures, 0 errors。
  - 本地 HTTP run `88cb4eef-61a2-4acc-92af-a0d13eda0d19` 先进入 `WAITING_FOR_APPROVAL`，approve 后最终状态 `SUCCEEDED`，stopReason 为 `COMPLETED`。
  - `TOOL_CALL_FINISHED` content 包含 `exitCode=0`、`stdout="mock command\n"`、空 `stderr`、`stdoutTruncated=false`、`stderrTruncated=false` 和命令耗时。
- 覆盖：
  - `runCommand` 可在 workspace 子目录执行命令并返回 cwd、退出码和 stdout。
  - 非零退出码作为命令观察返回，不被归为工具失败。
  - stdout 超过上限时被截断并标记。
  - `..` cwd 逃逸被拒绝，非目录 cwd 被拒绝。
  - 空命令、空白命令参数、过多/过长参数被拒绝。
  - `run_command` 经工具注册表返回 JSON 文本结果。
  - Spring Boot 上下文能加载六个 workspace 工具：`list_files`、`read_file`、`search_text`、`write_file`、`replace_text`、`run_command`。
  - Mock 模型可根据命令类 prompt 触发 `run_command`，并走 approve/reject/resume 审批路径。
  - 取消幂等修复覆盖：终态 run 不会被 `completeCancellation` 二次转换。
- 修正记录：
  - 首次增加命令工具测试后，`RunControllerTests.cancelRunEndpointReturnsRunState` 暴露取消与 runner 结束之间的竞态，报错为 `Terminal run cannot transition`；已修复 `AgentRunService.completeCancellation`，对已终态 run 直接返回。
- 补充最小环境变量测试时，macOS 临时目录在父进程中表现为 `/var/...`，子进程 PWD 为 realpath `/private/var/...`；测试改为比较 `toRealPath()`。
  - `ToolApprovalPolicy` 中 `run_command` 的说明从 shell command 改为 local command，因为当前实现直接使用 argv 数组和 `ProcessBuilder`，不通过 shell。
- 安全记录：本验证只执行 `/bin/pwd`、`/bin/echo`、`/usr/bin/false`、`/usr/bin/printf` 等固定测试命令；未调用真实模型；未读取或发送 API key、题目 PDF、`.zshrc` 或私有 workspace。
- 观察：Maven 仍提示用户全局 settings 中 `repositories` 标签位置警告；Mockito/ByteBuddy 动态 agent 仍有 JDK 未来兼容警告，目前不影响测试。
- 关联：ADR-0016。
- 代码版本/运行 ID：`941015c feat: add workspace command tool`；HTTP run `88cb4eef-61a2-4acc-92af-a0d13eda0d19`。
- 限制：当前不是 OS 级沙箱；命令 cwd 被限制在 workspace 内，但进程权限仍可访问主机上自身权限允许的路径；命令中断只显式销毁直接子进程，不证明完整进程树取消；真实 DeepSeek 命令审批 run 尚未验证。


## UI-003：前端工具卡片与命令输出展示验证

- 日期：2026-08-28。
- 类型：前端构建、本地浏览器交互验证。
- 范围：`frontend/src/App.vue`、`frontend/src/run/toolCards.ts`、`frontend/src/style.css`、后端 mock run/SSE/approval API。
- 方法：
  - 执行 `cd frontend && npm run build`。
  - 启动后端：`cd backend && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080`。
  - 启动前端：`cd frontend && npm run dev -- --host 127.0.0.1`，访问 `http://127.0.0.1:5173/`。
  - 使用 in-app browser 点击默认命令任务的 Run，等待 `Approval required: run_command`，确认 Approve 按钮可用。
  - 点击 Approve and run，等待 Run finished，并检查命令卡片、Command tab、页面宽度和 console。
- 结果：通过。
  - 前端构建：`vue-tsc -b` 与 `vite build` 均成功。
  - 浏览器初始健康状态为 `ok`，没有横向溢出。
  - 审批前页面显示 `run_command`、`Waiting approval` 和 `/bin/echo mock command`，Approve 按钮可点击。
  - 审批后页面显示 `run_command` 工具卡片 `Finished`，包含 `$ /bin/echo mock command`、`cwd: .`、`exit: 0`、stdout `mock command`、stderr 空输出和 duration。
  - 右侧详情栏存在 Command tab；浏览器 console 无 warning/error；最终 run 短 id 为 `ec6cabf0`，状态显示 `succeeded`。
- 修正记录：
  - 首次浏览器验收发现审批面板出现但 Approve 按钮 disabled，因为前端未在 `APPROVAL_REQUIRED` 事件后同步刷新 `activeRun.status`；已调整禁用条件为存在 pending approval 且未在处理审批即可点击，状态合法性由后端 endpoint 兜底。
  - 一次前端 patch 和 CSS 写入命令使用了错误工作目录，导致脚本未修改目标文件；随后从仓库根目录重新执行并验证。
- 安全记录：本验证只访问本地 `localhost`，使用 mock 模型和固定命令 `/bin/echo mock command`；未调用真实模型，未读取或发送 API key、题目 PDF、`.zshrc` 或私有 workspace。
- 关联：ADR-0004、ADR-0008、ADR-0014、ADR-0016。
- 代码版本/运行 ID：`f8e5fa1 feat: show command tool cards in workbench`；浏览器 run 短 id `ec6cabf0`。
- 限制：不证明真实 DeepSeek 命令审批 run；未做多浏览器、多视口或移动端视觉验收。


## DEMO-001：真实 demo 编程任务基线验证

- 日期：2026-08-28。
- 类型：demo workspace 基线测试、前端构建检查。
- 范围：`workspaces/demo/README.md`、`workspaces/demo/src/price_calculator.py`、`workspaces/demo/src/__init__.py`、`workspaces/demo/tests/test_price_calculator.py`、`frontend/src/App.vue`。
- 方法：
  - 在 `workspaces/demo` 执行 `python3 -m unittest discover -s tests -v`。
  - 执行 `cd frontend && npm run build`，确认默认 prompt 调整不破坏前端构建。
  - 执行 `cd backend && mvn test`，确认新增 demo workspace 文件不影响后端回归。
  - 执行 `git check-ignore -v workspaces/demo/src/__pycache__/x.pyc workspaces/demo/src/x.pyc`，确认 Python 缓存会被忽略。
- 结果：符合预期。
  - Demo unittest 共 4 个测试，2 个通过，2 个失败。
  - 失败测试为 `test_discount_is_applied_before_tax` 和 `test_full_discount_leaves_only_zero_taxable_amount`。
  - 失败输出显示当前实现返回 `71.28 != 58.32`、`43.18 != 0.0`，说明折扣被错误地加到了 subtotal 上。
  - 前端构建通过：`vue-tsc -b` 与 `vite build` 均成功。
  - 后端回归通过：118 tests, 0 failures, 0 errors。
  - `.gitignore` 覆盖 Python 缓存：`__pycache__/` 与 `*.py[cod]` 均生效。
- 设计说明：demo 故意保持失败状态，用于后续真实模型 run 展示“读文件/读测试 → 修改代码 → 审批 diff → 运行命令 → 测试通过 → 总结”的完整链路。
- 安全记录：demo 使用 Python 标准库 `unittest`，不安装第三方依赖，不访问网络，不读取凭据。
- 关联：ADR-0004、ADR-0006、ADR-0014、ADR-0016。
- 代码版本/运行 ID：`d66e3f6 feat: add failing pricing demo workspace`。
- 限制：本条只证明 demo 任务基线按预期失败；不证明真实模型已经修复或命令工具已在真实模型 run 中执行。

## MODELAPI-002：真实模型 demo 前置 loop 策略调整回归

- 日期：2026-08-28（北京时间）
- 范围：`RunBudget` 默认预算、OpenAI-compatible 模型系统提示、provider 缺少 content 时的安全诊断。
- 命令：`cd backend && mvn test`
- 结果：通过。Maven Surefire 报告 118 tests, 0 failures, 0 errors, 0 skipped。
- 说明：这验证了预算/提示/诊断调整没有破坏现有后端逻辑；不证明真实模型 demo 修复闭环已成功。

## MODELAPI-003：OpenAI-compatible 空 content 重试回归

- 日期：2026-08-28（北京时间）
- 范围：OpenAI-compatible provider 在 HTTP 2xx 但 `choices[0].message.content` 为空时的单次重试。
- 命令：`cd backend && mvn test`
- 结果：通过。Maven Surefire 报告 119 tests, 0 failures, 0 errors, 0 skipped。
- 说明：新增测试覆盖第一次空 content、第二次请求追加协议修复提醒并返回有效 JSON 协议内容的路径；不对 HTTP 错误、非 JSON、协议解析失败进行重试。

## LOOP-002：Agent transcript 工具动作回填验证

- 日期：2026-08-28（北京时间）
- 范围：多轮 Agent loop 中，上一轮模型工具调用动作是否进入下一轮模型上下文。
- 命令：`cd backend && mvn test`
- 结果：通过。Maven Surefire 报告 119 tests, 0 failures, 0 errors, 0 skipped。
- 说明：新增测试确认第二轮 `ModelRequest` 中包含 `Requested tool calls`、`tool_call_id` 和工具名；这验证上下文可追踪性修复，不等同于真实模型 demo 已完成。

## MODEL-002：工具调用响应空 message 降级验证

- 日期：2026-08-28（北京时间）
- 范围：`ModelResponseParser` 对空 message + 合法 tool_calls 的兼容解析。
- 命令：`cd backend && mvn test`
- 结果：通过。Maven Surefire 报告 120 tests, 0 failures, 0 errors, 0 skipped。
- 说明：降级只适用于可执行工具动作合法的响应；最终回答空 message、非字符串 message 和无效 tool_calls 仍按解析错误处理。

## MODEL-003：模型 JSON 外壳容错提取验证

- 日期：2026-08-28（北京时间）
- 范围：`ModelResponseParser` 对 Markdown 代码块和前后夹杂文字中的 JSON object 提取。
- 命令：`cd backend && mvn test`
- 结果：通过。Maven Surefire 报告 121 tests, 0 failures, 0 errors, 0 skipped。
- 说明：提取后仍执行原有协议字段、工具调用和参数校验；提取不到完整 JSON object 时仍失败。

## MODELAPI-004：模型协议修复重试验证

- 日期：2026-08-28（北京时间）
- 范围：OpenAI-compatible 适配器对可恢复协议问题的单次修复重试。
- 命令：`cd backend && mvn test`
- 结果：通过。Maven Surefire 报告 122 tests, 0 failures, 0 errors, 0 skipped。
- 说明：重试发生在模型边界，尚未执行工具动作；工具结构错误、HTTP 错误、审批、预算和工具执行错误不重试。

## REALDEMO-001：真实模型 demo 修复闭环

- 日期：2026-08-28（北京时间）
- 范围：真实模型调用、本地工具调用、写入审批、命令审批、命令输出回填、最终总结。
- V4 Flash 结果：未通过。最后一次强约束 run `5016a759-aa62-44f9-86e7-dbc5dcc8180b` 成功读取 `tests/test_price_calculator.py` 和 `src/price_calculator.py`，但在应提出代码修改时以 `MODEL_PARSE_ERROR: Model response is not valid JSON` 失败。
- 成功模型：DeepSeek `deepseek-chat`，run `546c7fe9-8b05-447b-a25d-87c0ad7dd601`。
- 审批 1：`replace_text`，toolCallId `fix_discount_bug`，path `src/price_calculator.py`，只替换折扣计算一行。
- 审批 2：`run_command`，toolCallId `run_tests_after_fix`，command `python3 -m unittest discover -s tests -v`，cwd `.`。
- Agent 工具结果：`run_command` exitCode 0；stderr 显示 4 个测试 `ok`，`Ran 4 tests ... OK`。
- 外部复核命令：`cd workspaces/demo && python3 -m unittest discover -s tests -v`
- 外部复核结果：通过。4 tests, 0 failures, 0 errors。
- 限制：成功 run 使用强指令提示直接要求 first action 为 `replace_text`，用于验证真实闭环链路；自然语言自主定位 bug 的 DeepSeek 系列 run 尚未稳定通过。

## MODELAPI-005 OpenAI-compatible 原生 tool calling 单元验证

- 时间：2026-08-28 20:08 CST。
- 命令：`cd backend && mvn test`。
- 范围：OpenAI-compatible 模型适配器、结构化 `ModelMessage`、runner 上下文回填和既有后端回归。
- 结果：通过，124 tests passed。
- 说明：测试覆盖 native `tools` 请求体、native `tool_calls` 响应解析、assistant tool_calls 与 tool result 消息回填、legacy JSON content fallback；这证明本地协议转换逻辑通过替身 HTTP 验证，不等同于真实 DeepSeek native run 已完成。

## REALMODEL-002 DeepSeek V4 Flash 原生 tool calling 只读验证

- 时间：2026-08-28 20:10-20:11 CST。
- 后端启动：`mvn spring-boot:run`，参数包括 `--server.port=18080 --agent.model.provider=openai-compatible --agent.model.name=deepseek-v4-flash --agent.model.tool-protocol=native-tools`。
- Run ID：`0edd0f1a-cc84-484e-b436-0888a85a30a5`。
- 范围：真实 DeepSeek V4 Flash provider、原生 `tools` / `tool_calls`、本地只读工具执行、工具结果回填、多轮 run 完成。
- 结果：通过。Run 以 `SUCCEEDED` / `COMPLETED` 结束，`roundsUsed=4`，`toolCallsUsed=7`。
- 证据：事件流显示模型以原生 tool calling 请求 `list_files`、`read_file`，本地工具返回 workspace 目录、`README.md`、源码和测试文件内容；最终模型输出中文总结。
- 限制：该验证是只读任务，未覆盖 native 模式下的写入审批和命令审批；模型在部分轮次一次返回多个只读工具调用，说明“每轮最多一个工具”的提示不是 provider 硬约束，本地预算和审批仍需作为最终控制。

## REALDEMO-002：DeepSeek V4 Flash 原生 tool calling 真实修复闭环

- 日期：2026-08-28（北京时间）。
- 前置恢复：执行 `git checkout -- workspaces/demo/src/price_calculator.py` 恢复 failing baseline。
- 基线验证：执行 `cd workspaces/demo && python3 -m unittest discover -s tests -v`，结果为 4 tests，2 failures；失败用例是 `test_discount_is_applied_before_tax` 和 `test_full_discount_leaves_only_zero_taxable_amount`。
- 后端启动：`mvn spring-boot:run`，参数包括 `--server.port=18080 --agent.model.provider=openai-compatible --agent.model.name=deepseek-v4-flash --agent.model.tool-protocol=native-tools`。
- Run ID：`d2f4ab1e-5f4c-4458-9248-c38383aa31fa`。
- 模型链路：DeepSeek V4 Flash 使用原生 `tool_calls` 调用 `list_files`、`read_file` 读取 README、源码和测试，随后提出 `replace_text`。
- 审批 1：`replace_text`，toolCallId `call_00_FU3cYlwyC56Z8eLM90kj0628`，path `src/price_calculator.py`，只把 `discounted = base * (1 + discount_percent / 100)` 改为 `discounted = base * (1 - discount_percent / 100)`。
- 审批 2：`run_command`，toolCallId `call_00_GoOtpsAG8oCP55uAuqj50792`，command `python3 -m unittest discover -s tests -v`，cwd `.`。
- Agent 工具结果：`run_command` exitCode 0；stderr 显示 4 个 unittest 全部 `ok`，`Ran 4 tests ... OK`。
- 外部复核命令：`cd workspaces/demo && python3 -m unittest discover -s tests -v`。
- 外部复核结果：通过，4 tests, 0 failures, 0 errors。
- Run 结果：`SUCCEEDED` / `COMPLETED`。
- 限制：当前 `workspaces/demo/src/price_calculator.py` 保留真实模型修复后的未提交修改；如需重新录制从失败到修复，需要再次恢复 failing baseline。

## UI-004 Codex-like 工作台布局与事件投影验证

- 时间：2026-08-28 21:02 CST。
- 命令：`cd frontend && npm run build`。
- 结果：通过，Vite production build 成功。
- 浏览器检查：启动 Vite dev server，实际端口为 `http://localhost:5174/`；页面展示左侧 Project/Runs、中间 Chat Timeline、右侧 Inspector、底部 Terminal。
- 交互检查：在浏览器点击 Run 后，界面渐进展示 assistant 消息、工具卡片和 `run_command` 权限审批卡；右侧显示已发现文件，底部 Terminal 显示待审批命令。
- 限制：该阶段使用已有 SSE 事件实现消息级渐进渲染，不是 provider token-level streaming；浏览器检查中未批准命令，未验证完整前端审批后终态。
- 目视证据：本地临时截图 `tmp/codex-like-ui-check.png`，不纳入提交。

## UI-005 可调工作台面板构建验证

- 时间：2026-08-28 21:24 CST。
- 命令：`cd frontend && npm run build`。
- 范围：Vue 工作台布局、Inspector 开合 props、Terminal 开合 props、拖拽状态的 TypeScript 编译和生产构建。
- 结果：通过，`vue-tsc -b` 和 Vite production build 成功。
- 覆盖：确认前端新增的可收起/可展开面板状态、组件事件和模板绑定通过类型检查与打包。
- 限制：未做浏览器实际拖拽验收；未验证完整真实模型 run 的审批后 UI 终态。

## UI-006 侧栏精简与底部命令栏移除构建验证

- 时间：2026-08-28 22:09 CST。
- 命令：`cd frontend && npm run build`。
- 范围：移除底部 `BottomTerminal` 页面路径、精简 Inspector tabs、清理 checks/terminal 投影类型后的前端类型检查和生产构建。
- 结果：通过，`vue-tsc -b` 和 Vite production build 成功。
- 覆盖：确认右侧面板只保留审查/文件入口后的组件绑定、类型导出和打包链路有效。
- 限制：未做浏览器目视验收；未验证完整审批后 UI 终态。

## UI-007 Codex-like 空状态视觉验收

- 时间：2026-08-28 22:18 CST。
- 命令：`cd frontend && npm run build`。
- 构建结果：通过，`vue-tsc -b` 和 Vite production build 成功。
- 浏览器检查：in-app browser 打开并刷新 `http://localhost:5173/`。
- 检查结果：左侧栏 computed width 为 260px；右侧审查/文件入口高度约 34px；右侧 Inspector 不再显示 backend chip；底部 `.bottom-terminal` DOM 不存在；浏览器 console 无 warning/error。
- 限制：只覆盖空状态页面和基础布局，不覆盖完整审批后终态。

## UI-008 — Codex-like 语义叙事 UI 优化验证

- 日期：2026-08-29（北京时间）。
- 范围：`DESIGN.md`、`frontend/src/run/display.ts`、`frontend/src/run/timeline.ts`、`frontend/src/components/ActionRow.vue`、`ChangeSummaryCard.vue`、`ApprovalCard.vue`、`ChatTimeline.vue`、`InspectorPane.vue`、`ProjectSidebar.vue`、`ComposerBox.vue`、`frontend/src/style.css`。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`python3 /Users/zhumeiyuan/.codex/plugins/cache/openai-curated-remote/frontend-design-premium/1.4.0/skills/frontend-design-premium/scripts/audit_project.py /Users/zhumeiyuan/Desktop/CodingAgent --mode strict`。结果：通过，0 findings。
- 命令：`rg -n "(?:window\.)?(?:alert|confirm|prompt)\s*\(|href=\"#\"|v-html|innerHTML|cursor:\s*pointer|resize:\s*vertical" frontend/src DESIGN.md || true`。结果：仅命中全局 `button { cursor: pointer; }`，未发现 native dialog、假链接、`v-html`、`innerHTML` 或 textarea vertical resize。
- 浏览器检查：打开 `http://localhost:5173/`，确认右侧 tabs 为“审查/文件”、无 `.bottom-terminal` DOM、composer textarea `resize: none`、控制台 warning/error 为空。
- 未覆盖：未在本次验证中真实点击 Run 执行 DeepSeek 任务，因此 Approve/Reject 后的完整真实模型终态目视验收仍需在录屏前补。

## UI-009 — 审批、Markdown 与审查滚动修正验证

- 日期：2026-08-29（北京时间）。
- 范围：`frontend/src/components/ApprovalCard.vue`、`MarkdownBlock.vue`、`ChatTimeline.vue`、`ComposerBox.vue`、`InspectorPane.vue`、`frontend/src/run/display.ts`、`frontend/src/run/timeline.ts`、`frontend/src/style.css`、`DESIGN.md`。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`python3 /Users/zhumeiyuan/.codex/plugins/cache/openai-curated-remote/frontend-design-premium/1.4.0/skills/frontend-design-premium/scripts/audit_project.py /Users/zhumeiyuan/Desktop/CodingAgent --mode strict`。结果：通过，0 findings。
- 命令：`git diff --check`。结果：通过。
- 反模式搜索：未发现 native dialog、假链接、`v-html`、`innerHTML`、textarea vertical resize；仅命中全局原生 button cursor 样式。
- 浏览器空状态检查：打开 `http://localhost:5173/`，确认 console warning/error 为空、composer textarea `resize: none`、页面无横向溢出。
- 未覆盖：本次未新建真实模型 run，因此审批卡真实待批准状态、Markdown 终态和右侧 diff 终态仍需录屏前做一次动态目视验收。

## UI-010 — 审批空引用错误修复与前端流式 reveal 验证

- 日期：2026-08-29（北京时间）。
- 范围：`frontend/src/App.vue`、`frontend/src/components/ChatTimeline.vue`、`frontend/src/style.css`、`DESIGN.md`。
- 问题定位：审批成功后 `pendingApproval` 可能已被 SSE 事件刷新为 `null`，继续读取 `.name` 会触发 `Cannot read properties of null (reading 'name')`。
- 修复：批准/拒绝前保存 approval 快照；成功完成 run 时清理 `runError`；活动 run 中 assistant 文本按块 reveal。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`python3 /Users/zhumeiyuan/.codex/plugins/cache/openai-curated-remote/frontend-design-premium/1.4.0/skills/frontend-design-premium/scripts/audit_project.py /Users/zhumeiyuan/Desktop/CodingAgent --mode strict`。结果：通过，0 findings。
- 命令：`git diff --check`。结果：通过。
- 浏览器空状态检查：打开 `http://localhost:5173/`，确认初始页面无 `.run-error`、console warning/error 为空、页面无横向溢出。
- 限制：本次未触发真实模型 run；流式 reveal 基于前端收到的完整模型消息事件，不是 provider token-level streaming。

## UI-011 — Codex-like 前端视觉 polish 验证

- 日期：2026-08-29（北京时间）。
- 范围：`frontend/src/App.vue`、`frontend/src/components/ComposerBox.vue`、`frontend/src/components/ProjectSidebar.vue`、`frontend/src/components/InspectorPane.vue`、`frontend/src/style.css`。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`python3 /Users/zhumeiyuan/.codex/plugins/cache/openai-curated-remote/frontend-design-premium/1.4.0/skills/frontend-design-premium/scripts/audit_project.py /Users/zhumeiyuan/Desktop/CodingAgent --mode strict`。结果：通过，0 findings。
- 命令：`git diff --check`。结果：通过。
- 覆盖：确认本轮 UI polish 通过类型检查、生产构建、严格设计审计和 diff 空白检查。
- 限制：本环境未成功加载 Playwright 做自动截图；仍建议你在 `http://127.0.0.1:5173/` 手动刷新查看动态界面，尤其是真实审批后的审查面板和最终回答。

## UNDO-001 — Agent 文件变更撤销验证

- 日期：2026-08-29（北京时间）。
- 范围：`WorkspaceWriteTools` 撤销执行、`WorkspaceChangeJournal` 撤销快照、工具私有 metadata、`POST /api/runs/{runId}/changes/{toolCallId}/undo`、前端变更卡和右侧审查面板撤销入口。
- 命令：`cd backend && mvn test`。结果：通过，128 tests, 0 failures, 0 errors。
- 后端覆盖：
  - `write_file` 新建文件后，撤销会在当前 hash 匹配时删除该文件。
  - `replace_text` 修改文件后，撤销会在当前 hash 匹配时恢复旧内容。
  - 撤销前若文件已被用户或后续工具改动，返回内容冲突而不覆盖。
  - 工具结果 JSON 不包含 `previousContent`，但 `ToolResult.privateMetadata` 保留 `WorkspaceChangeUndoSnapshot` 供后端 journal 使用。
  - Mock run 写入审批通过并成功结束后，undo endpoint 返回 `UNDONE`，事件回看包含 `CHANGE_UNDONE`。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`python3 /Users/zhumeiyuan/.codex/plugins/cache/openai-curated-remote/frontend-design-premium/1.4.0/skills/frontend-design-premium/scripts/audit_project.py /Users/zhumeiyuan/Desktop/CodingAgent --mode strict --no-write`。结果：通过，0 findings。
- 命令：`git diff --check`。结果：通过。
- 限制：撤销记录目前只保存在后端进程内存，后端重启后不能撤销旧 run 的变更；本次未启动浏览器手动点击撤销按钮做目视验收。

## UI-012 — Codex-like 前端体验升级验证

- 日期：2026-08-29（北京时间）。
- 范围：`frontend/src/components/UiIcon.vue`、`ComposerBox.vue`、`ActionRow.vue`、`ChangeSummaryCard.vue`、`ChatTimeline.vue`、`InspectorPane.vue`、`ProjectSidebar.vue`、`frontend/src/App.vue`、`frontend/src/run/timeline.ts`、`frontend/src/run/toolCards.ts`、`frontend/src/style.css`、`DESIGN.md`。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`python3 /Users/zhumeiyuan/.codex/plugins/cache/openai-curated-remote/frontend-design-premium/1.4.0/skills/frontend-design-premium/scripts/audit_project.py /Users/zhumeiyuan/Desktop/CodingAgent --mode strict --no-write`。结果：通过，0 findings。
- 命令：`git diff --check`。结果：通过。
- 反模式搜索：`rg -n "Unknown backend error|Failed to create run|Failed to cancel|Failed to undo|Event stream closed|Workspace panel|Workspace panel tabs|Conversation workspace|Task composer|Task input|Projects and runs|Drag to resize|Permission needed|Waiting approval|Proposed|Finished|Running|Rejected|coding task|letter-spacing:\s*-|background:\s*linear-gradient|hero|orb|bokeh" frontend/src DESIGN.md`。结果：仅命中 `DESIGN.md` 中的 Codex 参考和 `runFinishedContent` 函数名，未发现界面文案泄漏或样式反模式。
- 覆盖：确认共享图标组件、composer 键盘交互、撤销状态展示、timeline 滚动策略、Inspector 语言徽标和样式 token 覆盖通过类型检查、生产构建和严格设计审计。
- 限制：本次没有启动浏览器做截图/点击验收，也没有新建真实模型 run；真实审批和撤销点击仍需在录屏前动态检查。

## UI-013 — Composer 对话框专项优化验证

- 日期：2026-08-29（北京时间）。
- 范围：`frontend/src/App.vue`、`frontend/src/components/ComposerBox.vue`、`frontend/src/style.css`。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`python3 /Users/zhumeiyuan/.codex/plugins/cache/openai-curated-remote/frontend-design-premium/1.4.0/skills/frontend-design-premium/scripts/audit_project.py /Users/zhumeiyuan/Desktop/CodingAgent --mode strict --no-write`。结果：通过，0 findings。
- 命令：`git diff --check`。结果：通过。
- 反模式搜索：未发现默认英文 demo prompt、英文 run-event fallback、负字距或装饰渐变；旧全局 `textarea:focus-visible` 规则仍存在，但 composer 专项规则已覆盖为无 textarea outline，焦点反馈迁移到外层 composer。
- 覆盖：确认空白默认 composer、快捷建议 chip、焦点样式覆盖和中文 fallback 文案通过前端类型检查、生产构建和严格设计审计。
- 限制：本次未用浏览器截图复核视觉结果；已启动的 Vite dev server 会热更新展示最新 composer。

## SELFTEST-001 — 本地后端启动与 mock 功能闭环自测

- 日期：2026-08-29（北京时间）。
- 启动：`cd backend && mvn spring-boot:run`。结果：通过，Tomcat 在 `http://localhost:8080` 启动成功。
- 前端：既有 Vite dev server 仍运行在 `http://127.0.0.1:5173/`，`/api` 代理到后端。
- 健康检查：
  - `curl -fsS http://127.0.0.1:8080/api/health`：通过，返回 `status=ok`、`service=coding-agent-backend`、`javaVersion=21`。
  - `curl -fsS http://127.0.0.1:5173/api/health`：通过，确认前端 dev proxy 可访问后端。
- 写入审批闭环：
  - 创建 run：`d2b9a74e-f3b2-41df-9d37-3dfb03ccef9f`，prompt 为创建 mock note 文件。
  - 事件回看：模型请求 `write_file`，toolCallId `mock-call-1`，后端产生 `APPROVAL_REQUIRED`。
  - 批准：`POST /api/runs/d2b9a74e-f3b2-41df-9d37-3dfb03ccef9f/approvals/mock-call-1/approve`。
  - 结果：`TOOL_CALL_FINISHED` 返回 `undoable=true`，content 含 `src/mock-note.txt` 的 unified diff；run 以 `SUCCEEDED / COMPLETED` 结束。
  - 文件检查：`workspaces/demo/src/mock-note.txt` 创建成功，内容为 `mock note`。
- 撤销闭环：
  - 调用：`POST /api/runs/d2b9a74e-f3b2-41df-9d37-3dfb03ccef9f/changes/mock-call-1/undo`。
  - 结果：返回 `state=UNDONE`、`deleted=true`、`restored=false`；文件 `workspaces/demo/src/mock-note.txt` 已不存在；事件回看包含 `CHANGE_UNDONE`。
- 命令审批闭环：
  - 创建 run：`8f35c119-168e-4567-a974-d44fac993180`，prompt 为运行测试命令。
  - 事件回看：模型请求 `run_command`，arguments 为 `["/bin/echo","mock command"]`，cwd 为 `.`，后端产生 `APPROVAL_REQUIRED`。
  - 批准：`POST /api/runs/8f35c119-168e-4567-a974-d44fac993180/approvals/mock-call-1/approve`。
  - 结果：`TOOL_CALL_FINISHED` 返回 `exitCode=0`、`stdout="mock command\n"`、`stderr=""`、`durationMillis=11`；run 以 `SUCCEEDED / COMPLETED` 结束。
- 拒绝审批闭环：
  - 创建 run：`0bfe2f58-2048-4794-8c63-6ecc30c7e7ed`，prompt 为创建会被拒绝的 mock 文件。
  - 拒绝：`POST /api/runs/0bfe2f58-2048-4794-8c63-6ecc30c7e7ed/approvals/mock-call-1/reject`。
  - 结果：run 返回 `FAILED`，`stopReason=APPROVAL_REJECTED`，事件回看包含 `APPROVAL_RESOLVED approved=false` 和 `RUN_FINISHED`；未继续执行工具。
- 观察：普通 sandbox 中 `curl` 无法连接本机 dev server；使用已批准的 localhost `curl` 提权完成自测。一次读取拒绝事件时自动审批审查超时，重试后成功。
- 限制：本次是 HTTP/API 层 mock 功能自测，没有通过浏览器实际点击 Approve/Reject/Undo，也没有使用真实 DeepSeek 模型跑完整前端动态流程。

## PERSIST-001 — H2 持久化测试与跨重启验证

- 日期：2026-08-29（北京时间）。
- 范围：H2 file datasource、`schema.sql`、`JdbcAgentRunPersistence`、`JdbcWorkspaceChangePersistence`、`AgentRunStore` 启动加载、`WorkspaceChangeJournal` undo snapshot 持久化、`GET /api/runs`、前端启动历史加载。
- 命令：`cd backend && mvn test`。结果：通过，131 tests, 0 failures, 0 errors。
- 后端测试覆盖：
  - `JdbcPersistenceTests.runStoreReloadsRunsEventsAndPendingApproval`：同一 H2 memory DB 中，第二个 `AgentRunStore` 实例可恢复 run、事件和 pending approval continuation。
  - `JdbcPersistenceTests.workspaceChangePersistenceReloadsUndoSnapshotAndResult`：undo snapshot、`UNDOABLE`/`UNDONE` 状态和撤销结果可从 DB 恢复。
  - `RunControllerTests.listRunsReturnsCreatedRuns`：`GET /api/runs` 返回已创建 run。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`git diff --check`。结果：通过。
- HTTP 跨重启验证：
  - 第一次启动：`cd backend && mvn spring-boot:run`，Hikari 连接 `jdbc:h2:file:./data/coding-agent`。
  - 创建只读 run：`205548a7-4244-451b-a135-036ac9ee87de`，prompt 为查看 workspace 文件列表。
  - 重启前事件回看：11 条事件，包含 `TOOL_CALL_REQUESTED list_files`、`TOOL_CALL_FINISHED` 和 `RUN_FINISHED status=SUCCEEDED`。
  - 停止后端并第二次启动。
  - `GET /api/runs`：通过，重启后列表包含 `205548a7-4244-451b-a135-036ac9ee87de`。
  - `GET /api/runs/205548a7-4244-451b-a135-036ac9ee87de/events`：通过，仍返回 11 条事件，第一条 `RUN_CREATED`，最后一条 `RUN_FINISHED / SUCCEEDED`。
  - `GET http://127.0.0.1:5173/api/runs`：通过，前端 Vite proxy 能访问 run 列表，返回 12 条历史 run。
- 忽略规则验证：`git check-ignore -v backend/data/coding-agent.mv.db` 命中 `.gitignore` 的 `/backend/data/`。
- 限制：非终态 `RUNNING`/`CANCELLING` run 在后端崩溃后不会自动恢复后台任务；持久化主要覆盖历史回看、pending approval continuation 和 undo snapshot。H2 数据是本地运行数据，不提交仓库。

## RUNTIME-001 — 工具失败作为可恢复 Observation 验证

- 日期：2026-08-30（北京时间）。
- 范围：`MockAgentRunner` 普通 loop、审批恢复后的 loop、tool timeout observation、运行终止语义。
- 命令：`cd backend && mvn test`。结果：通过，131 tests, 0 failures, 0 errors。
- 覆盖：
  - `MockAgentRunnerTests.failedToolResultIsReturnedToModelAsObservation`：工具返回 `success=false` 后，run 不直接 failed；下一轮 `ModelRequest` 包含 `ModelRole.TOOL` 消息，内容含 `tool_call_id`、`tool_name`、`success=false` 和失败文本；随后模型 STOP，run 以 `SUCCEEDED / COMPLETED` 结束。
  - `MockAgentRunnerTests.toolTimeoutIsReturnedToModelAsObservation`：工具超时产生 `TOOL_TIMEOUT` metadata 和 `success=false` observation；runner 继续向模型发起下一轮请求，模型 STOP 后 run 成功结束。
- 说明：工具执行失败现在被视为可恢复 observation；不可恢复失败仍包括 `ModelClientException`、`ModelParseException` 和 runner 内部异常；系统强制结束仍包括 round limit、tool call limit、length/token limit 和用户取消。
- 限制：本次验证使用替身模型和单元测试，未启动真实 DeepSeek run 专门验证“工具失败后自修”；context window 仍是简单尾部裁剪，尚未按 tool-call/tool-result pair 做成组裁剪。

## TOOL-OBS-001 — Tool System 结构化 observation 与 edit_file 验证

- 日期：2026-08-30（北京时间）。
- 范围：`WorkspaceToolFactory`、`WorkspaceToolConfiguration`、`ToolRegistry`、`ToolApprovalPolicy`、`WorkspaceChangeJournal`、`WorkspaceReadTools`、`WorkspaceWriteTools`、`WorkspaceCommandTools`、前端 `display.ts` 和 `timeline.ts`。
- 命令：`cd backend && mvn test`。结果：通过，132 tests, 0 failures, 0 errors。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 覆盖：
  - Spring 工具注册表包含 `edit_file`、`list_files`、`read_file`、`replace_text`、`run_command`、`search_text`、`write_file`。
  - `edit_file` 复用 exact replacement 逻辑，返回 path、replacement count、unified diff，并携带 undo snapshot 私有 metadata。
  - `ToolApprovalPolicy` 对 `edit_file` 要求用户审批。
  - `list_files`、`read_file`、`search_text`、`write_file`、`replace_text` 和 `run_command` 成功 JSON 均包含 `success=true` 和 `message`。
  - `run_command` 非 0 exit code 在命令结果 JSON 中表现为 `success=false`，同时保留 stdout/stderr/exitCode/timedOut/duration。
  - `ToolRegistry` 对参数错误和运行时异常返回结构化失败 JSON，包含 `success=false`、`message` 和 `errorCode`，且不泄漏内部异常细节。
- 限制：本次未启动真实模型验证模型是否更偏好 `edit_file`；`edit_file` 和 `replace_text` 暂时作为等价工具共同暴露。

## CONTEXT-001 — Context window 配对感知裁剪验证

- 日期：2026-08-30（北京时间）。
- 范围：`MockAgentRunner.contextWindow`、assistant tool_calls 与 tool result 的上下文配对、运行预算下的历史消息裁剪。
- 命令：`cd backend && mvn test`。结果：通过，133 tests, 0 failures, 0 errors。
- 命令：`git diff --check`。结果：通过。
- 覆盖：
  - `MockAgentRunnerTests.contextWindowKeepsSystemPromptAndRecentMessages`：裁剪后仍保留 system prompt 和初始 user task，且保留的 tool result 前一条是包含对应 tool call id 的 assistant 消息。
  - `MockAgentRunnerTests.contextWindowDoesNotKeepOrphanToolMessagesWhenTrimming`：小上下文窗口下只保留完整的最近 assistant/tool pair，不会留下孤立 `ModelRole.TOOL` 消息。
- 说明：当前仍是 message-count 窗口，不是真实 token-aware compression；但 provider-facing context 已避免拆散 native tool calling 所需的 assistant/tool 对应关系。
- 清理：测试生成的 `backend/data/coding-agent.mv.db` 和 `backend/data/coding-agent.trace.db` 已删除。

## FAILURE-001 — Failure Recovery 可恢复工具错误验证

- 日期：2026-08-30（北京时间）。
- 范围：`ToolRegistry` 失败 observation schema、workspace error code 映射、`MockAgentRunner` 工具失败后继续 loop、system prompt 恢复策略。
- 命令：`cd backend && mvn test`。结果：通过，135 tests, 0 failures, 0 errors。
- 命令：`git diff --check`。结果：通过。
- 覆盖：
  - `MockAgentRunnerTests.recoverableWorkspaceErrorCanDriveTheNextToolAttempt`：模型先调用 `read_file("src/foo.py")`，工具返回 `WORKSPACE_NOT_FOUND`、`failureKind=RECOVERABLE_TOOL_ERROR`、`recoverable=true` 和包含 `list_files` 的 `recoveryHint`；runner 没有失败，而是继续下一轮 `list_files(".")`，再 `read_file("README.md")`，最终 `SUCCEEDED / COMPLETED`。
  - `WorkspaceToolFactoryTests.missingWorkspacePathReturnsRecoverableNotFoundFailure`：缺失文件映射为 `WORKSPACE_NOT_FOUND`，失败 JSON 包含可恢复标记和恢复提示。
  - `ToolRegistryTests.toolExecutionExceptionReturnsStructuredFailureResult`：参数错误等 `ToolExecutionException` 返回结构化 recoverable failure，不抛出到 runner。
  - 既有 `toolTimeoutIsReturnedToModelAsObservation` 继续验证 timeout 作为失败 observation 回填，而不是直接终止。
- 说明：resource/policy termination 仍由 round/tool-call/token/cancel/approval rejection 控制；模型 provider 错误、模型响应解析错误和 runner 内部异常仍归为 failed。
- 清理：测试生成的 `backend/data/coding-agent.mv.db` 已删除；未发现残留 H2 trace 文件。
- 限制：本次验证使用替身模型和真实本地 workspace 工具，没有启动真实 DeepSeek 专门跑“读错文件后自恢复”的动态 demo；重复同类失败目前由 budget 兜底，尚未实现 no-progress detector。

## PROMPT-001 — Agent Operating Policy Prompt 验证

- 日期：2026-08-30（北京时间）。
- 范围：`MockAgentRunner.SYSTEM_PROMPT`、OpenAI-compatible JSON protocol instruction formatting、模型请求中的 system message。
- 命令：`cd backend && mvn test`。结果：通过，136 tests, 0 failures, 0 errors。
- 命令：`git diff --check`。结果：通过。
- 覆盖：
  - `MockAgentRunnerTests.systemPromptDefinesAgentOperatingPolicy`：首轮 `ModelRequest` 的 system message 包含 autonomous coding agent、本地 workspace、无直接 filesystem/terminal 访问、修改前检查、工具错误作为 observation、避免重复失败动作、合理验证后再完成等关键 operating policy。
- 说明：runner prompt 负责 Agent 行为策略；OpenAI-compatible adapter 继续负责 JSON/native tool calling provider 协议说明。
- 清理：测试生成的 `backend/data/coding-agent.mv.db` 已删除；未发现残留 H2 trace 文件。
- 限制：本次没有跑真实 DeepSeek 专项验证 prompt 对模型行为的影响；真实录屏前仍需做一次端到端 demo 检查。

## COMMAND-CLEANUP-001 — run_command 进程树清理验证

- 日期：2026-08-30（北京时间）。
- 范围：`WorkspaceCommandTools` 命令等待 interrupt 响应、`ProcessHandle.descendants()` 进程树清理、受限环境 fallback。
- 命令：`mvn -Dtest=WorkspaceCommandToolsTests test`。结果：普通 sandbox 下通过，8 tests, 0 failures, 0 errors, 1 skipped；跳过原因为 sandbox 禁止 `ProcessHandle.descendants()` 枚举，表现为 `/bin/ps: Operation not permitted`。
- 命令：提权后 `mvn -Dtest=WorkspaceCommandToolsTests test`。结果：通过，8 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd backend && mvn test`。结果：通过，137 tests, 0 failures, 0 errors, 0 skipped。
- 覆盖：
  - `WorkspaceCommandToolsTests.interruptedCommandDestroysChildProcessTree`：启动会派生后台子进程并持续写文件的命令；中断命令线程后，工具线程退出为 `WorkspaceAccessException`，后台子进程不再继续写输出。
  - `WorkspaceCommandTools.destroyProcessTree`：中断时按 descendants → parent 顺序强制销毁，并在 parent 退出后再次 best-effort 补扫 descendants。
  - 受限环境下 descendants 枚举异常会被捕获，至少清理 parent，避免 cleanup 抛出原始 runtime exception。
- 说明：这降低 timeout/cancel 后残留子进程风险，但不是 OS 级沙箱；daemon 化或脱离父子关系的进程仍不保证覆盖。

## STREAM-001 — Provider token-level streaming 验证

- 日期：2026-08-30（北京时间）。
- 范围：OpenAI-compatible native tools streaming request、provider SSE chunk 解析、fragmented tool call arguments 累计、runner delta event、前端 timeline 拼接。
- 命令：`cd backend && mvn -Dtest=OpenAiCompatibleModelClientTests,MockAgentRunnerTests test`。结果：通过，26 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd backend && mvn test`。结果：通过，140 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`git diff --check`。结果：通过。
- 覆盖：
  - `OpenAiCompatibleModelClientTests.streamsNativeTextDeltasAndReturnsFinalResponse`：native tools 请求包含 `stream=true`，provider SSE 中的 `"Hel"`、`"lo"` 被逐段回调，并最终组装为完整 `ModelResponse("Hello")`。
  - `OpenAiCompatibleModelClientTests.parsesFragmentedNativeToolCallsFromStream`：streamed `delta.tool_calls` 的 id、name 和 arguments 跨 chunk 拼接后，仍能生成 `ToolCall("call-1", "list_files", {"path":"."})`。
  - `MockAgentRunnerTests.emitsModelMessageDeltaEventsForStreamingClients`：runner 对 streaming client 发出两个 `MODEL_MESSAGE_DELTA` 事件，并保留最终 `MODEL_MESSAGE_RECEIVED`。
  - 前端 `npm run build` 覆盖 `model_message_delta` SSE 监听和 timeline delta 拼接类型检查。
- 说明：当前 JSON content fallback 不做 token-level streaming；真实 provider stream 尚未在浏览器录屏路径中专项验证。

## FULLCHECK-001 — 全项目提交前检查

- 日期：2026-08-30（北京时间）。
- 范围：题目 PDF 要求对照、后端全量测试、前端构建、demo workspace 测试、禁用 Agent 框架扫描、敏感信息扫描、提交物与忽略规则检查。
- 命令：`cd backend && mvn test`。结果：通过，140 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`cd workspaces/demo && python3 -m unittest discover -s tests -v`。结果：通过，4 tests, 0 failures, 0 errors。
- 命令：`rg -n "LangChain|LlamaIndex|OpenAI Agents SDK|Claude Agent SDK|AutoGen|CrewAI|Spring AI|langchain|llamaindex|autogen|crewai|spring-ai|openai-agents" backend frontend backend/pom.xml frontend/package.json -S`。结果：无命中；未发现禁用 Agent 框架依赖或代码引用。
- 命令：`rg -n "sk-[A-Za-z0-9]|Bearer [A-Za-z0-9_\\-]{12,}|api[_-]?key\\s*[:=]\\s*['\\\"][^'\\\"]+|DEEPSEEK_API_KEY\\s*=\"" -S .`。结果：未发现真实 API key；命中项为正常 UI/CSS token 命名、环境变量名或测试假数据。
- 命令：`git check-ignore -v 推免考核题目学生版.pdf backend/data/coding-agent.mv.db workspaces/private-demo/foo.txt`。结果：题目 PDF、H2 本地数据库和非 demo 私有 workspace 均被 `.gitignore` 覆盖。
- 命令：`find . -maxdepth 3 -iname 'README.txt' -o -iname '*.mp4' -o -iname '*.zip'`。结果：检查前无提交版 README、视频或 zip；随后已新增根目录 `README.txt` 草稿。
- 命令：`wc -m README.txt`。结果：883 个字符，低于题目要求的 1000 汉字限制。
- 清理：全量测试生成的 `backend/data/coding-agent.mv.db` 已删除；复查 `find backend/data -maxdepth 1 -type f -print` 无输出。
- 结论：从功能实现和验证证据看，项目已满足简化版 Coding Agent 的核心要求；提交前还需整理提交历史、推送公开仓库、录制 2 分钟内 mp4，并制作包含 `README.txt` 和视频的姓名 zip。
- 风险：当前 Git 工作区有大量未提交改动；`workspaces/demo` 下存在若干 untracked C++ 临时文件和测试产生的 `__pycache__`，公开前建议清理或保持不暂存。

## BUGFIX-001 — 真实命令 run 的 model_error 定位与修复验证

- 日期：2026-09-01（北京时间）。
- 范围：`run_command` 参数归一化、OpenAI-compatible native tools 空 stream 降级、前端工具卡命令展示、Spring 集成测试数据源隔离。
- 现场复现：
  - `cd workspaces/demo && g++ --version`：失败，exit code 69，stderr/stdout 提示未同意 Xcode license，需要在 Terminal 中执行 `sudo xcodebuild -license`。
  - `cd backend && mvn test` 初次失败：SpringBootTest 连接真实 `backend/data/coding-agent.mv.db`，H2 报 `Database may be already in use` 和 `The file is locked`。
- 修复覆盖：
  - `WorkspaceToolFactoryTests.runCommandToolAcceptsJsonStringArrayFromModel`：`command` 为字符串 `"[\"/bin/echo\",\"hello\"]"` 时归一化为 argv array 并成功执行。
  - `WorkspaceToolFactoryTests.runCommandToolStillRejectsShellCommandString`：`command` 为 `"which g++"` 时继续返回 `INVALID_ARGUMENTS`，不引入 shell 解析。
  - `OpenAiCompatibleModelClientTests.fallsBackToNonStreamingNativeRequestWhenProviderStreamIsEmpty`：provider stream 只返回 `[DONE]` 时，客户端第二次发送 `stream=false` native request 并返回非 streaming 响应。
- 命令：`cd backend && mvn -Dtest=WorkspaceToolFactoryTests,OpenAiCompatibleModelClientTests test`。结果：通过，26 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd backend && mvn test`。结果：通过，143 tests, 0 failures, 0 errors, 0 skipped；Spring 集成测试使用 `jdbc:h2:mem:coding-agent-test`，不再争抢真实运行数据库。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`git diff --check`。结果：通过。
- 说明：本次修复降低模型轻微参数格式错误和 provider 空 stream 对 run 的影响；它不修复本机 Xcode license 环境问题。C++ demo 需要先同意 license，或录屏继续使用已验证的 Python demo。

## BUGFIX-002 — 多 tool calls native history 400 定位与修复验证

- 日期：2026-09-01（北京时间）。
- 范围：`MockAgentRunner` 单工具调用强制、native tools transcript 稳定性、失败 run 错误详情展示。
- 现场定位：
  - 从 `backend/data/coding-agent.mv.db` 复制快照后读取最新 run：`d2771827-317b-4906-b2d4-3f285c83754e`。
  - 该 run 在 `which clang++` 成功后进入下一轮模型请求，随后 `RUN_FINISHED` 为 `FAILED / MODEL_ERROR`。
  - 完整错误：`Model provider returned HTTP 400: {"error":{"message":"An assistant message with 'tool_calls' must be followed by tool messages responding to each 'tool_call_id'. (insufficient tool messages following tool_calls message)"...}}`。
  - 事件序列显示第 2 轮模型一次返回 4 个只读 tool calls，说明仅靠 prompt 的“一次最多一个工具”不足以约束真实 provider。
- 修复覆盖：
  - `MockAgentRunner.acceptedToolCalls` 在 `TOOL_CALLS` 响应中只接收第一个 tool call，进入 transcript 的 assistant message 不再携带多个 tool calls。
  - `MockAgentRunnerTests.acceptsOnlyFirstToolCallFromEachModelResponse`：模型一次返回 `call-1` 与 `call-2` 时，runner 只执行 `call-1`，下一轮上下文只包含 `assistant(call-1)` 与 `tool(call-1)`。
  - `toolCallLimitStopsBeforeExecutingTooManyCalls` 更新为新语义：已用完工具预算后，下一轮工具请求才触发 `TOOL_CALL_LIMIT`。
  - 前端 `runFinishedContent` 展示 `errorMessage` 的精简版本，方便从 UI 直接看到 provider 错误。
- 命令：`cd backend && mvn -Dtest=MockAgentRunnerTests,OpenAiCompatibleModelClientTests,WorkspaceToolFactoryTests test`。结果：通过，42 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd backend && mvn test`。结果：通过，144 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`git diff --check`。结果：通过。
- 说明：本修复解决的是 native tool calling history 形态导致的 provider HTTP 400；本机 C++ 编译链仍需处理 Xcode/CommandLineTools 环境问题。

## BUGFIX-003 — Streaming 传输异常 fallback 验证

- 日期：2026-09-01（北京时间）。
- 范围：OpenAI-compatible native tools streaming、transport IO failure fallback、真实 run 中 `Model HTTP streaming request failed` 的恢复策略。
- 现场定位：
  - 真实 C++ run `c85ded0b-ddac-4ab7-a34d-1e22da149fda` 已完成写入 `selftest_rotate.cpp`、根据编译失败 observation 修改 include、继续验证等多轮动作。
  - 后续模型请求以 `FAILED / MODEL_ERROR` 结束，错误信息为 `Model HTTP streaming request failed`，属于 Java HTTP streaming 传输层 IO 异常。
- 修复覆盖：
  - `OpenAiCompatibleModelClient.completeNativeToolStream` 在 native tools streaming 抛出由 `IOException` 引起的 `ModelClientException` 时，降级为同请求的非 streaming native completion。
  - HTTP 非 2xx、provider 协议错误、鉴权/额度错误和非 IO 类异常不走该 fallback，仍暴露为真实 model/provider failure。
  - `OpenAiCompatibleModelClientTests.fallsBackToNonStreamingNativeRequestWhenProviderStreamTransportFails` 验证第一次 streaming 请求失败后，会发送第二次 `stream=false` native request 并返回模型最终消息。
- 命令：`cd backend && mvn -Dtest=OpenAiCompatibleModelClientTests,MockAgentRunnerTests test`。结果：通过，29 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd backend && mvn test`。结果：通过，145 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`git diff --check`。结果：通过。
- 说明：fallback 后该轮不再产生 token delta，但能保留完整 Agent loop；如果非 streaming retry 也失败，run 仍会按 infrastructure failure 结束。

## REALFLOW-001 — DeepSeek native tools 真实 Python 端到端流程

- 日期：2026-09-01（北京时间）。
- 范围：真实模型、native tool calling、多轮 Agent loop、写文件审批、命令执行审批、命令 observation 回填、最终回答、H2 持久化。
- 后端启动：`cd backend && mvn spring-boot:run -Dspring-boot.run.arguments="--agent.model.provider=openai-compatible --agent.model.name=deepseek-v4-flash --agent.model.tool-protocol=native-tools"`。结果：启动成功，健康检查 `GET /api/health` 返回成功。
- 真实 run：`2c110940-e4d7-4e42-b376-4ec0529046c3`。
- 用户任务：创建 `selftest_factorial.py`，读取非负整数并打印阶乘，然后用输入 `5` 验证输出 `120`。
- 结果：run 状态 `SUCCEEDED / COMPLETED`，共 3 个模型 round、2 次工具调用。
- 工具链路：
  - 第 1 轮模型请求 `write_file` 写入 `selftest_factorial.py`，审批后工具执行成功。
  - 第 2 轮模型请求 `run_command`，命令为 `["sh","-c","echo 5 | python3 selftest_factorial.py"]`，审批后执行成功。
  - 命令 observation：`exitCode=0`、`stdout="120\n"`、`stderr=""`、`success=true`。
  - 第 3 轮模型返回最终总结，确认已创建文件并验证输出为 120。
- 本地复核：在 `workspaces/demo` 执行 `python3` subprocess，向 `selftest_factorial.py` 输入 `5\n`，结果 `exit 0`、`stdout 120`、`stderr` 为空。
- 全量验证：`cd backend && mvn test` 通过 145 tests；`cd frontend && npm run build` 通过；`git diff --check` 通过。
- 限制：
  - 因当前命令工具没有 stdin 字段，模型用 `sh -c "echo 5 | python3 ..."` 完成输入重定向；这是后续可优化的 tool schema 设计点。
  - C++ 完整编译运行仍受本机 Xcode/CommandLineTools linker 影响：Homebrew clang 可 compile-only，但 link 阶段失败。这不是 Agent loop 的失败；C++ 录屏前仍建议修好本机 linker 或继续使用 Python demo。

## REALFLOW-002 — Command Line Tools 修复后真实 C++ 端到端流程

- 日期：2026-09-01（北京时间）。
- 范围：本机 C++ toolchain、真实模型、native tool calling、多轮 Agent loop、写文件审批、C++ 编译命令审批、程序运行验证、最终回答。
- 环境确认：
  - `xcrun --find ld`：通过，输出 `/Library/Developer/CommandLineTools/usr/bin/ld`。
  - `g++ --version`：通过，Apple clang 21 可响应。
  - `clang++ -std=c++17 workspaces/demo/hello.cpp -o /private/tmp/coding-agent-hello`：通过；运行 `/private/tmp/coding-agent-hello` 输出 `Hello, world!`。
- 真实 run：`019de8c5-f6f9-4af1-b308-52b33bc995e0`。
- 用户任务：创建 C++17 程序 `selftest_sum_gcc.cpp`，读取两个整数并打印和；使用 `/opt/homebrew/bin/g++-15 -std=c++17 -O2 -o selftest_sum_gcc selftest_sum_gcc.cpp` 编译；用输入 `7 35` 验证输出 `42`。
- 结果：run 状态 `SUCCEEDED / COMPLETED`，共 4 个模型 round、3 次工具调用。
- 工具链路：
  - 第 1 轮模型请求 `write_file` 写入 `selftest_sum_gcc.cpp`，审批后工具执行成功。
  - 第 2 轮模型请求 `run_command` 编译，命令 exit code 0，无 stdout/stderr 错误。
  - 第 3 轮模型请求 `run_command` 运行，命令为 `["sh","-c","echo '7 35' | ./selftest_sum_gcc"]`，exit code 0，stdout 为 `42\n`。
  - 第 4 轮模型返回最终总结，确认编译和运行验证均成功。
- 相关失败样例：run `30e87b32-9f8d-42e5-8591-076683f847a7` 使用裸 `clang++` 时，在命令工具的最小环境下出现 `fatal error: 'iostream' file not found`；模型继续排查 include 路径但最终达到 `ROUND_LIMIT`。这说明 C++ demo 若依赖本机 toolchain，prompt 最好显式指定已验证的 `/opt/homebrew/bin/g++-15`。
- 限制：命令工具仍没有 stdin 字段，因此模型用 `sh -c` 管道输入完成交互式验证；这是后续安全与易用性可优化点。

## BUGFIX-004 — run_command macOS C++ 环境修复验证

- 日期：2026-09-01（北京时间）。
- 范围：`WorkspaceCommandTools` 最小命令环境、PATH 优先级、`TMPDIR` 保留、`g++`/`gcc` Homebrew GCC alias、真实 C++ Agent flow。
- 问题：
  - 用户截图中的真实 run 使用普通 `g++ -std=c++17 ...` 时，在 Agent 的干净命令环境中落到 `/usr/bin/g++`，随后 Apple clang 找不到 `iostream`。
  - Agent 因持续尝试诊断编译器环境，最终达到 `ROUND_LIMIT`，这是预算保护生效，不是 Runtime 崩溃。
- 修复覆盖：
  - `WorkspaceCommandTools` 默认 PATH 改为优先包含 `/opt/homebrew/opt/llvm/bin:/opt/homebrew/bin:/usr/local/bin`，再合并继承 PATH。
  - `TMPDIR` 从后端环境白名单继承，避免 macOS 编译器在极简环境下创建临时文件失败。
  - 当命令首项为 `g++` 或 `gcc`，且本机存在可执行的 `/opt/homebrew/bin/g++-15` 或 `/opt/homebrew/bin/gcc-15` 时，工具执行前解析到 Homebrew GCC。
  - 最小环境仍不传递 `HOME`、模型 API key 或任意完整用户环境。
  - 默认 `RunBudget.maxRounds` 从 8 调高到 1000，仅保留为极端兜底；正常防无限循环主要依赖 `maxToolCalls=64`、用户取消和模型/provider failure。
- 单元测试：
  - `WorkspaceCommandToolsTests.usesMinimalEnvironment`：验证 PATH/TMPDIR 存在，且不包含 `HOME`/`DEEPSEEK_API_KEY`。
  - `WorkspaceCommandToolsTests.resolvesHomebrewGccAliasWhenAvailable`：验证存在 Homebrew GCC 时，`g++ --version` 实际解析为 `/opt/homebrew/bin/g++-15`。
- 命令：`cd backend && mvn -Dtest=WorkspaceCommandToolsTests test`。结果：通过，9 tests, 0 failures, 0 errors。
- 命令：`cd backend && mvn test`。结果：通过，146 tests, 0 failures, 0 errors, 0 skipped；调整默认 round 上限后重新执行仍通过。
- 真实 run：`9718dc9f-2a36-4111-a22f-b62c56b85b1d`。
  - 用户任务要求使用普通 `g++ -std=c++17 -O2 -o selftest_plain_gpp selftest_plain_gpp.cpp`。
  - 工具执行 observation 中的实际 command 为 `["/opt/homebrew/bin/g++-15","-std=c++17","-O2","-o","selftest_plain_gpp","selftest_plain_gpp.cpp"]`。
  - 编译 exit code 0。
  - 运行命令 `echo '7 35' | ./selftest_plain_gpp` exit code 0，stdout 为 `42\n`。
  - run 最终状态 `SUCCEEDED / COMPLETED`，共 4 rounds、3 tool calls。
- 说明：该修复是 macOS/Homebrew toolchain 兼容策略，不改变 Agent loop、审批、安全边界或工具 schema。

## UI-014 — Codex-like 审查聚合与 workspace 文件树验证

- 日期：2026-09-01（北京时间）。
- 范围：右侧 Inspector 审查/文件面板、workspace 只读 API、diff 横向滚动、文件内容预览。
- 实现检查：
  - 审查 tab 现在以当前 run 内所有成功修改过的文件为主列表，不再把最近一次工具调用或命令详情当作默认审查内容。
  - 点击某个修改文件会展开该文件对应的 unified diff；同一文件多次修改会按工具调用分别展示。
  - diff 行使用 `max-content` 代码列和 `overflow-x: auto`，长行可在侧栏内横向滚动查看。
  - 文件 tab 通过 `/api/workspace/files` 读取 workspace 目录，并以目录树显示；点击文件后通过 `/api/workspace/file` 读取内容并在右侧预览。
  - workspace API 复用既有 path resolver/read tools，路径穿越等非法路径返回 HTTP 400。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`cd backend && mvn -Dtest=WorkspaceControllerTests,WorkspaceCommandToolsTests test`。结果：通过，12 tests, 0 failures, 0 errors。
- 命令：`cd backend && mvn test`。结果：通过，149 tests, 0 failures, 0 errors, 0 skipped。
- 命令：`git diff --check`。结果：通过。
- 说明：本轮完成构建和 API/单元验证；最终录屏前仍建议用真实模型 run 再做一次浏览器目视验收。

## UI-015 — 默认真实模型与文件面板空态修正验证

- 日期：2026-09-01（北京时间）。
- 范围：默认模型 provider、Spring Boot 测试覆盖、右侧文件面板未选中文件布局。
- 问题定位：
  - 用户看到 “Mock model observed the tool result and finished.” 的原因是后端普通启动仍使用 `agent.model.provider=mock` 默认配置。
  - 文件 tab 在未选中文件时固定渲染预览列，导致目录树只占左半边，右边出现空白提示块。
- 修复覆盖：
  - `backend/src/main/resources/application.properties` 默认改为 `agent.model.provider=openai-compatible`，继续使用 DeepSeek V4 Flash native tools 和 `DEEPSEEK_API_KEY` 环境变量。
  - 4 个 `@SpringBootTest` 入口显式设置 `agent.model.provider=mock`，保证测试离线确定。
  - 文件面板仅在 `selection.kind === 'file'` 时渲染预览列；未选中文件时目录树单列占满侧栏。
  - 新增 ADR-0037，并更新 README/README.txt/STATUS 中的默认 provider 说明。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`cd backend && mvn test`。结果：通过，149 tests, 0 failures, 0 errors, 0 skipped。
- 运行检查：后端已重新执行 `cd backend && mvn spring-boot:run`，在默认配置下启动到 8080；前端 Vite 保持 `http://localhost:5173/`。
- 说明：本轮未主动创建真实模型 run，避免额外发送用户任务到外部模型服务；刷新页面后新建任务会使用默认 DeepSeek provider。

## UI-016 — Codex-like 高级视觉升级验证

- 日期：2026-09-01（北京时间）。
- 范围：前端工作台视觉系统、三栏尺度、sidebar、workspace header、timeline、composer、审查 diff、文件预览和设计上下文。
- 实现检查：
  - 左侧栏从 260px 调整为 288px，中间主列最小宽度从 520px 调整为 560px，提升任务列表和对话区稳定性。
  - `frontend/src/style.css` 增加 Codex-like token 覆盖层：浅色技术网格背景、Geist-like 字体栈、玻璃 header/composer、统一滚动条、克制阴影和状态色。
  - 中间 timeline 收敛为 820px 阅读宽度，用户气泡使用深色高对比，assistant 输出保持轻量叙事面。
  - 右侧 Inspector 的审查文件行、语言徽标、diff block、横向滚动代码区和文件预览面板完成视觉统一。
  - `DESIGN.md` 已同步记录本轮视觉方向，避免后续漂回普通后台模板。
- 追加修正：
  - 用户消息气泡从深色改为 Codex 参考中的右侧浅灰气泡，避免通用段落色覆盖后出现深底深字不可读。
  - 对话区进一步按 Codex 参考收敛：用户消息靠右，assistant 正文和工具动作保持中央阅读流，工具动作降低视觉权重。
  - 顶部左侧 workspace 图标改为共享 folder icon，移除异常空白方块来源。
  - 顶部右侧移除在线/运行状态 chip，避免和审查入口重叠；折叠审查面板时只显示小型图标按钮。
  - Composer 去掉预设任务 chip，收窄为 Codex-like 输入框，并使用圆形 icon 发送按钮。
  - Composer 宽度恢复为随页面可用宽度自适应的 `min(760px, calc(100% - 56px))`，接近空状态说明卡宽度。
  - Composer 底部去掉 `+` 和 `本地 workspace`，只保留“帮我批准”模式开关和运行控制；发送按钮改为向上箭头 icon。
  - 前端新增自动批准模式：开启后检测到 `pendingApproval` 会自动调用现有 approve API；在已有待审批状态下打开也会立即批准。
  - “帮我批准”已改为 Codex-like 权限模式选择器：点击后弹出菜单，可选择“请求批准”或“帮我批准”，并显示当前选中状态。
  - 修复权限菜单显示异常：最终 CSS 覆盖 `composer-box { overflow: visible; }`，避免 popover 被输入框裁剪；模板确认包含“请求批准”和“帮我批准”两个 menuitem。
  - 修复发送箭头和权限图标异常：发送按钮改用稳定文本箭头，权限模式按钮改用局部 `approval-glyph`，避免全局 `.ui-icon` 伪元素叠加。
  - 权限模式选择器已改为纯文字：模板和 CSS 中不再使用 `approval-glyph`、`mode-icon`、`ui-icon-arrow-up` 或 `UiIcon name="arrow-up"`。
  - “变更待审查”提示位置修正为贴近 composer 上沿：最终 CSS 覆盖 `floating-change-chip bottom: 10px`。
  - Timeline 中央悬浮的“变更待审查”提示已移除，审查入口只保留在右侧 Inspector。
  - 右侧 Inspector 通过 `.inspector-body { overflow-y: auto; }` 接管纵向滚动，长审查列表和展开 diff 不再撑出面板。
  - 追加修正：Inspector 显式跨满主布局全部 grid rows，并使用 `height: 100vh` + flex column 固定高度链路；header/tabs 不滚动，body 独立滚动。
  - 源码中已确认不再存在 `floating-change-chip` 或“变更待审查”时间线提示。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`git diff --check`。结果：通过。
- 说明：本轮为视觉与前端构建验证；最终提交前仍建议在浏览器中刷新 `http://localhost:5173/` 做一次人工目视验收。

## UI-017 — 项目切换、任务固定排序与删除验证

- 日期：2026-09-01（北京时间）。
- 范围：左侧任务列表排序、项目添加/切换、任务删除、workspace root 运行时切换。
- 实现检查：
  - 前端 `upsertRun` 改为按 `createdAt` 倒序排序，点击历史任务只选中，不再改变任务列表顺序。
  - 前端项目区支持输入本地路径添加项目，勾选“新建文件夹”时由后端创建目录；切换项目后重置文件树和新任务 composer。
  - 后端新增 `workspace_projects` 持久化表，默认项目启动时自动注册，当前项目切换会调用 `WorkspacePathResolver.switchRoot`。
  - 后端新增 `DELETE /api/runs/{runId}`；删除非终态任务前会请求取消后台任务，并清理 run/event/pending approval/undo snapshot。
  - 前端每条任务行拆分为选择区域和删除按钮，避免按钮嵌套导致点击事件混乱。
- 命令：`cd backend && mvn -Dtest=RunControllerTests,WorkspaceControllerTests,WorkspacePathResolverTests test`。结果：通过，19 tests, 0 failures, 0 errors。
- 命令：`cd backend && mvn test`。结果：通过，151 tests, 0 failures, 0 errors。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 命令：`git diff --check`。结果：通过。
- 说明：Spring/Mockito 测试在普通受限命令下触发 ByteBuddy self-attach 限制；使用已批准的 Maven 测试权限重新运行后通过。

## UI-018 — 本机文件夹选择与用户消息操作验证

- 日期：2026-09-02（北京时间）。
- 范围：左侧添加项目入口、后端本机文件夹选择 API、用户消息复制/修改操作、composer 聚焦。
- 实现检查：
  - 后端新增 `FolderChooserService`，在 macOS 本地通过 `osascript` 打开系统文件夹选择器；超时或中断时会清理 chooser 进程树。
  - `POST /api/workspace/projects/choose-folder` 返回 `{ path, cancelled }`，用户取消选择不会被前端误报为失败。
  - 前端左侧项目区默认显示“选择文件夹”主按钮；手动路径输入移动到折叠兜底区域，仍支持“新建文件夹”。
  - 用户消息气泡新增 `复制` 和 `修改` 操作；修改会把历史用户 prompt 回填到 composer 并聚焦，复制使用浏览器 clipboard API。
- 命令：`cd backend && mvn test`。结果：通过，151 tests, 0 failures, 0 errors。
- 命令：`cd frontend && npm run build`。结果：通过，`vue-tsc -b && vite build` 成功。
- 说明：自动化测试验证编译、Spring 上下文和前端类型构建；系统文件夹选择器涉及 macOS GUI，最终交互仍建议在浏览器里点击“选择文件夹”做一次目视确认。
