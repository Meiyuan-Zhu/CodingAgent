package com.zhumeiyuan.codingagent.agent.execution;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.zhumeiyuan.codingagent.agent.run.RunId;

public class RunTaskManager {

	private final ExecutorService executor;
	private final Map<RunId, Future<?>> activeTasks = new ConcurrentHashMap<>();

	public RunTaskManager(ExecutorService executor) {
		this.executor = Objects.requireNonNull(executor, "executor");
	}

	public void start(RunId runId, Runnable task) {
		Objects.requireNonNull(runId, "runId");
		Objects.requireNonNull(task, "task");
		FutureTask<Void> futureTask = new FutureTask<>(() -> {
			task.run();
			return null;
		});
		while (true) {
			Future<?> previous = this.activeTasks.putIfAbsent(runId, futureTask);
			if (previous == null) {
				break;
			}
			if (previous.isDone()) {
				this.activeTasks.remove(runId, previous);
				continue;
			}
			throw new IllegalStateException("Run already has an active task: " + runId.value());
		}
		this.executor.execute(() -> {
			try {
				futureTask.run();
			} finally {
				this.activeTasks.remove(runId, futureTask);
			}
		});
	}

	public boolean cancel(RunId runId) {
		Objects.requireNonNull(runId, "runId");
		Future<?> task = this.activeTasks.get(runId);
		return task != null && task.cancel(true);
	}

	public boolean isActive(RunId runId) {
		Objects.requireNonNull(runId, "runId");
		Future<?> task = this.activeTasks.get(runId);
		return task != null && !task.isDone();
	}
}
