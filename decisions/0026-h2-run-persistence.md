# ADR-0026: H2 持久化 run、事件与撤销记录

- 日期：2026-08-29
- 状态：accepted
- 决策依据/确认来源：用户要求补持久化，并询问数据库选择；当前产品边界是本地单用户 coding agent。
- 实现状态：已实现并验证
- 取代/被取代：无

## 问题与约束

当前 `AgentRunStore`、pending approval 和 `WorkspaceChangeJournal` 都是进程内存。后端重启后，历史 run、事件回看和撤销快照会丢失，前端左侧任务历史也无法恢复。

本项目不需要多人并发数据库服务，也不适合为了作业演示引入 Postgres/MySQL 运维成本。持久化层应保持本地可运行、易清理、可测试，并且不把模型上下文、旧文件内容或运行日志暴露到提交材料中。

## 备选方案

1. JSONL/文件存储。依赖少、最轻，但查询、并发写、schema 演进和测试可信度较弱。
2. SQLite。单文件、可靠，但 Java/Spring 生态需要额外 JDBC driver，和 Spring Boot 默认测试/初始化路径不如 H2 顺滑。
3. H2 file mode + Spring JDBC。单文件本地数据库，Spring Boot 支持成熟，测试可用 H2 memory mode，避免 JPA/ORM。
4. Postgres/MySQL。生产感更强，但需要外部服务，超出本地单用户作业边界。

## 决定与理由

采用方案 3：H2 file mode + Spring JDBC。

运行时数据库位于 `backend/data/coding-agent.*`，通过 `schema.sql` 初始化。代码使用显式 JDBC persistence adapter，不引入 JPA。`AgentRunStore` 仍保持核心内存状态，启动时从数据库加载历史，运行中同步保存 run 状态、事件和 pending approval。`WorkspaceChangeJournal` 同步保存 undo snapshot、撤销状态和撤销结果。

同时新增 `GET /api/runs` 返回最近 run 列表，前端启动后加载历史 run，并通过事件回看恢复左侧任务标题。

## 代价与限制

- H2 file DB 是本地开发数据，不提交仓库。
- RUNNING/CANCELLING 等非终态 run 在进程崩溃后不会自动恢复后台任务；当前持久化主要恢复历史、事件回看、等待审批的 continuation 和撤销快照。
- Undo snapshot 中包含旧文件内容，仅保存在本地数据库，不进入模型观察、SSE payload 或前端日志；公开仓库前仍需检查 `backend/data/` 未入库。

## 验证

- `mvn test` 覆盖 JDBC persistence adapter 的 run/event/pending approval reload 和 workspace undo snapshot reload。
- 前端 `npm run build` 覆盖 `GET /api/runs` API 与启动加载历史 run 的类型检查。
- 本地启动后通过 HTTP 检查 `GET /api/runs` 能返回历史 run。
- 2026-08-29 已完成跨后端重启 HTTP 验证：run `205548a7-4244-451b-a135-036ac9ee87de` 在重启后仍出现在 `GET /api/runs`，事件回看保留 11 条并以 `RUN_FINISHED / SUCCEEDED` 结束。
