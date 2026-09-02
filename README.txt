Git 仓库：https://github.com/Meiyuan-Zhu/CodingAgent

运行方式：
1. 准备 Java 21、Maven、Node.js、npm。
2. 在本地 shell 设置 DEEPSEEK_API_KEY；项目只读取环境变量，不把密钥写入仓库、README 或视频。
3. 启动后端：cd backend && mvn spring-boot:run
4. 启动前端：cd frontend && npm run dev
5. 打开 http://localhost:5173/。Vite 会把 /api 代理到 Spring Boot 后端。

项目说明：
本项目是一个本地单用户 Coding Agent 工作台，目标是让大模型通过受控的本地工具完成真实编程任务。前端使用 Vue 3 + TypeScript + Vite，后端使用 Java 21 + Spring Boot。Spring 只负责 HTTP、配置、持久化和依赖组织；Agent 的对话历史、上下文裁剪、工具协议、模型响应解析、循环终止、错误恢复和本地执行均自行实现。不使用 LangChain、LlamaIndex、AutoGen、CrewAI、OpenAI Agents SDK、Spring AI 等 Agent 框架，也不依赖模型服务端托管的文件或代码执行工具。

特色功能：
1. 自研 Agent Runtime。用户输入任务后，后端创建 run，写入事件流并调用 OpenAI-compatible 模型。模型返回最终回答或原生 tool calling；AgentRunner 解析响应、校验工具名和参数、执行本地工具，再把 JSON observation 放回上下文进入下一轮。运行终止原因明确，包括完成、取消、工具/轮次预算触顶、模型长度限制和不可恢复错误。
2. 本地工具注册表。工具包括 list_files、read_file、search_text、write_file、edit_file、replace_text、run_command。模型不能直接访问文件系统，只能调用注册表中的工具。每个工具都有结构化参数校验和结构化返回值，便于模型根据结果继续读代码、改文件、运行测试和总结。
3. Workspace 安全边界。所有文件路径必须位于当前 workspace 内，拒绝绝对路径、.. 逃逸、symlink 逃逸和 .env 等敏感文件。写入、编辑和命令执行都需要用户审批；文件变更先生成 unified diff，可在右侧审查面板查看，并能按单次工具调用撤销。
4. 命令执行约束。run_command 使用 argv 数组而不是 shell 字符串，cwd 也限制在 workspace 内。执行前会清理敏感环境变量，并提供 timeout、stdout/stderr 截断、exit code 展示和进程树清理，避免命令失控或把密钥带入日志。
5. 可恢复错误处理。工具失败不会简单中断，而是返回 success=false、errorCode、failureKind 和 recoveryHint。例如文件不存在时，引导模型先 list_files 再选择正确路径；参数错误、权限拒绝、命令失败也会以可读 observation 回填，让模型有机会自我修正。
6. 稳定上下文管理。后端会保留 system prompt、原始用户任务和关键事件；上下文过长时裁剪历史，但避免拆散 assistant tool call 与对应 tool result，降低模型在长任务中丢失工具语义的风险。
7. Codex-like 工作台界面。界面为三栏结构：左侧项目切换和任务历史，中间对话时间线，右侧文件与审查面板。时间线展示 assistant 回复、工具卡片、审批卡片和 token 级流式输出；右侧支持文件树、文件预览、diff 审查、命令 stdout/stderr/exit code 和撤销状态。用户消息支持复制和修改，composer 提供“请求批准/帮我批准”模式，既方便安全审查，也方便录制 demo。
8. 本地持久化与 demo。H2 数据库保存 run 状态、事件历史、pending approval 和 undo snapshot，重启后仍可回看任务。demo workspace 提供 Python pricing 示例，可展示 Agent 读取文件、定位测试失败、申请修改、运行测试并总结结果的完整闭环。

验证：
后端：cd backend && mvn test
前端：cd frontend && npm run build
Demo：cd workspaces/demo && python3 -m unittest discover -s tests -v
