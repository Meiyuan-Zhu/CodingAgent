package com.zhumeiyuan.codingagent.agent.tool;

import java.util.Map;

@FunctionalInterface
public interface ToolHandler {

	ToolExecutionResult execute(Map<String, Object> arguments);
}
