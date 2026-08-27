# 当前开发状态

更新日期：2026-08-27（北京时间）。

**当前阶段：工程骨架已建立。Git 仓库已初始化，Vue 3 前端与 Spring Boot 后端已接入，并完成基础构建验证。**

## 已确认

- Vue 3 前端，Java + Spring Boot 后端，前后端分离。见 [ADR-0001](../decisions/0001-frontend-backend-stack.md)。
- 当前框架基线：Vue 3 + TypeScript + Vite，Java 21 + Maven + Spring Boot 3.5.16。见 [ADR-0003](../decisions/0003-framework-baseline.md)。
- 产品形态：Web 界面作为本地 workspace 工作台，不采用上传文件作为主要工作流；界面方向简洁、清晰、Codex-like。见 [ADR-0004](../decisions/0004-local-workbench-ui.md)。
- Agent 运行协议领域模型已建立，核心仍未接入模型、工具执行或 API。见 [ADR-0005](../decisions/0005-run-protocol-domain.md)。
- Agent 关键逻辑自行实现，项目不使用 Agent 框架/SDK 或 Spring AI。
- decisions/ 记录决策，memory/ 记录开发状态与文档。见 [ADR-0002](../decisions/0002-development-records.md)。
- 正式截止：北京时间 2026-09-03 00:00；此后不推送新提交。

## 已完成的准备工作

- 读取题目 PDF 的全部 2 页，整理要求与提交限制。
- 建立并迁移总体方案、技术选型决策。
- 建立开发约定、状态、阶段日志和验证记录。
- 已执行文档核验：见 [DOC-001](VERIFICATION.md)。这不构成应用功能测试。
- 初始化本地 Git 仓库，分支为 `main`。
- 建立 `frontend/` Vue 3 项目和 `backend/` Spring Boot 项目。
- 建立后端 `GET /api/health` 与前端健康状态展示，用于验证前后端开发连接基础。
- 已执行应用骨架验证：见 [APP-001](VERIFICATION.md)。

## 功能状态

状态含义：未开始 / 进行中 / 已实现未验证 / 已验证 / 阻塞。只有存在实现和对应有效证据才能写已验证。

| 模块 | 当前状态 | 实现位置 | 验证证据 | 决策/说明 |
| --- | --- | --- | --- | --- |
| 前后端工程骨架及独立启动 | 已验证 | [frontend](../frontend)、[backend](../backend) | [APP-001](VERIFICATION.md) | ADR-0001、ADR-0003；验证覆盖构建和后端健康接口 |
| 任务接口与 Web 页面 | 进行中 | [frontend/src/App.vue](../frontend/src/App.vue)、[frontend/src/style.css](../frontend/src/style.css)、[backend/src/main/java/com/zhumeiyuan/codingagent/health/HealthController.java](../backend/src/main/java/com/zhumeiyuan/codingagent/health/HealthController.java) | [APP-001](VERIFICATION.md)、[UI-001](VERIFICATION.md) | 当前是 Codex-like 工作台壳和健康状态，还不是可执行任务界面 |
| 模型适配器及 Agent 循环 | 进行中 | [backend/src/main/java/com/zhumeiyuan/codingagent/agent/run](../backend/src/main/java/com/zhumeiyuan/codingagent/agent/run) | [CORE-001](VERIFICATION.md) | 已完成运行协议领域模型；尚未实现模型适配器、循环调度或响应解析 |
| 文件工具、搜索与编辑 | 未开始 | 尚无 | 尚无 | 需要目录与编辑冲突校验 |
| 命令执行、审批、取消 | 未开始 | 尚无 | 尚无 | 需要真实进程与权限测试 |
| 对话上下文及运行预算 | 未开始 | 尚无 | 尚无 | 裁剪与终止规则待定 |
| SSE、工具卡片、Diff、输出 | 未开始 | 尚无 | 尚无 | 基于后端真实事件展示 |
| 运行记录与历史回看 | 未开始 | 尚无 | 尚无 | 回看不能重复执行工具 |
| 真实模型任务及回归测试 | 未开始 | 尚无 | 尚无 | 与模拟模型测试分开 |
| 提交说明、视频与面试材料 | 未开始 | 尚无 | 尚无 | 依据真实能力编写 |

## 下一步

1. 实现本地 workspace 边界与只读文件工具：`list_files`、`read_file`、`search_text`。
2. 定义第一条任务提交接口、运行状态查询接口和 SSE 事件协议。
3. 建立后端 Agent Core 的工具注册表和模型适配边界。
4. 确认可用模型 API、工具调用支持和测试预算；密钥由用户在本地环境中配置，不写进聊天或文档。
5. 确认公开仓库的账户、名称及可公开文件；首次公开推送前复核题目 PDF、日志、密钥和演示材料。

## 风险与待定项

- 尚未做模型连通性验证，不能声称可调用模型。
- 公开远程仓库尚未建立；当前只有本地 Git 历史。
- 演示案例尚未最终确定，排期属于建议。
- 当前没有运行中的应用，也没有已验证的执行隔离能力。
