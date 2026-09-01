Git 仓库：https://github.com/Meiyuan-Zhu/CodingAgent

运行方式：
1. 后端：cd backend && mvn spring-boot:run
2. 前端：cd frontend && npm run dev
3. 打开 Vite 本地地址。默认使用 DeepSeek V4 Flash native tools；启动前需在本地 shell 设置 DEEPSEEK_API_KEY。测试会覆盖为 mock，不依赖外部模型。

项目说明：
这是一个简化版 Coding Agent，前端 Vue 3，后端 Java 21 + Spring Boot。核心 Agent Runtime 自行实现，没有使用 LangChain、AutoGen、OpenAI Agents SDK、Spring AI 等 Agent 框架。后端维护 conversation history，调用 LLM，解析 native tool calls，执行本地工具，将 observation 回填给模型，并在预算内循环直到最终回答、取消或失败。

工具包括 list_files、read_file、search_text、write_file、edit_file、replace_text、run_command。文件和命令限制在 workspace 内；写入、编辑和命令执行需要审批；命令有 timeout、输出截断和进程树清理；工具错误作为可恢复 observation 返回模型。界面参考 Codex，展示对话、工具调用、diff、命令输出、审批、撤销和历史任务。native tools 路径支持 token-level streaming。

验证方式：
后端：cd backend && mvn test
前端：cd frontend && npm run build
Demo workspace：cd workspaces/demo && python3 -m unittest discover -s tests -v
