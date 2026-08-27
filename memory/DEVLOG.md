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
