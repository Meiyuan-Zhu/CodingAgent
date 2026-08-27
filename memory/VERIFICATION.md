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
