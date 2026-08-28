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
