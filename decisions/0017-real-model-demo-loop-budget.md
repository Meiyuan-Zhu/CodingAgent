# ADR-0017：真实模型 demo 的单工具轮次策略与预算调整

- 状态：accepted
- 日期：2026-08-28
- 取代：部分取代 [ADR-0011](0011-agent-loop-budget.md) 中默认预算和真实模型工具调用节奏的设定。

## 背景

真实 DeepSeek V4 Flash demo 修复任务首次运行时，模型能按协议读取 workspace，但在一轮内连续请求多个工具，较快消耗预算，并在后续 provider 响应中出现缺少 `choices[0].message.content` 的失败。原先 `4` 轮、`12` 次工具调用的预算足够 mock 流程，但对真实编程 demo 偏紧。

录屏展示也需要清楚呈现“模型提出一个动作 → 本地执行/审批 → 观察回填 → 下一步”的链路。如果模型单轮批量提出多个动作，前端虽能处理，但讲解难度更高，也不利于人工审批。

## 决策

1. 默认运行预算调整为 `maxRounds=8`、`maxToolCalls=16`、`maxContextMessages=30`、`toolTimeout=5s`。
2. OpenAI-compatible 模型系统提示中明确要求：一次最多调用一个工具，等待工具观察后再决定下一步。
3. 系统提示中给出 `run_command` 的 argv 示例，强调不使用 shell 字符串。
4. 当 provider 响应缺少可解析 content 时，错误信息只记录安全的响应形状：`finish_reason` 和 message 字段名，不记录完整原始响应，避免泄露模型输出中的潜在敏感内容。

## 备选方案

- 保持原预算：实现更少，但真实任务容易在读文件阶段耗尽轮次。
- 允许模型单轮批量工具调用：吞吐更高，但审批和录屏解释更混乱。
- 记录完整 provider 响应：排障更直接，但不符合最小日志原则。

## 影响

- 真实 demo 任务有更多轮次完成“读测试、读代码、修改、运行测试、总结”。
- 成本会略微增加，但 demo workspace 很小，可接受。
- mock 测试仍能验证预算边界；真实模型成功与否需要单独记录，不能用 mock 替代。

## 验证

- 待执行：后端 `mvn test`。
- 待执行：真实 DeepSeek V4 Flash demo 修复闭环。
