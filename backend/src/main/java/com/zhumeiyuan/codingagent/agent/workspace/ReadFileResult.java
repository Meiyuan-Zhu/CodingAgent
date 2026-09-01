package com.zhumeiyuan.codingagent.agent.workspace;

public record ReadFileResult(boolean success, String message, String path, String content, long sizeBytes) {
}
