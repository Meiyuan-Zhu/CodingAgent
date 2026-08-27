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
