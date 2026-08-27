# 决策索引

这里只记录“为什么这样设计”，当前进度查看 [开发状态](../memory/STATUS.md)。

| 编号 | 主题 | 状态 | 实现情况 |
| --- | --- | --- | --- |
| [ADR-0001](0001-frontend-backend-stack.md) | Vue 3 + Java/Spring Boot，前后端分离 | accepted | 工程骨架已建立，Agent 核心未实现 |
| [ADR-0002](0002-development-records.md) | 决策与开发记录分开维护 | accepted | 目录和初始记录已建立 |
| [ADR-0003](0003-framework-baseline.md) | Vue 3 与 Spring Boot 工程基线 | accepted | 前后端骨架及健康接口已验证 |
| [ADR-0004](0004-local-workbench-ui.md) | 本地 workspace 工作台式 Web 界面 | accepted | Vue 工作台壳已实现，Agent 执行未实现 |
| [ADR-0005](0005-run-protocol-domain.md) | Agent 运行协议领域模型 | accepted | 后端领域模型和状态转换测试已验证 |
| [ADR-0006](0006-workspace-boundary-read-tools.md) | Workspace 边界与只读文件工具 | accepted | 后端路径边界和 list/read/search 已验证 |
| [ADR-0007](0007-tool-registry.md) | 工具注册表边界 | accepted | 后端工具定义、参数校验、执行入口和 workspace 只读工具注册已验证 |

## 规则

- 使用四位递增编号和简短主题，按 [模板](TEMPLATE.md) 新建文件，不重用旧编号。
- 状态：`proposed` 为待确认，`accepted` 为已接受，`superseded` 为已被新决策取代，`rejected` 为明确不采用。
- 决策被接受不代表功能已经实现或验证。
- 重要决策记录需求、备选、理由、代价、代码与验证证据。没有证据就写尚无，不填虚构路径或提交。
- 常规重构和样式修改不必新增 ADR；改变已有架构、权限或接口语义时才需要。
- 修改已接受的决定须保留旧理由，并链接到新的取代记录。目录搬迁、链接修复可直接维护并注明。
