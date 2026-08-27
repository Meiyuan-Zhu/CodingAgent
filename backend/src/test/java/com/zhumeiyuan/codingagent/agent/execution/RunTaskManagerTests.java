package com.zhumeiyuan.codingagent.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.zhumeiyuan.codingagent.agent.run.RunId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RunTaskManagerTests {

	private final java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
	private final RunTaskManager manager = new RunTaskManager(this.executor);

	@AfterEach
	void shutdown() {
		this.executor.shutdownNow();
	}

	@Test
	void tracksActiveTaskAndCleansUpAfterCompletion() throws Exception {
		RunId runId = RunId.newId();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		this.manager.start(runId, () -> {
			started.countDown();
			await(release);
		});

		assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
		assertThat(this.manager.isActive(runId)).isTrue();
		release.countDown();
		awaitInactive(runId);
		assertThat(this.manager.isActive(runId)).isFalse();
	}

	@Test
	void cancelsActiveTaskWithInterrupt() throws Exception {
		RunId runId = RunId.newId();
		CountDownLatch started = new CountDownLatch(1);
		AtomicBoolean interrupted = new AtomicBoolean(false);

		this.manager.start(runId, () -> {
			started.countDown();
			try {
				Thread.sleep(Duration.ofSeconds(10));
			} catch (InterruptedException ex) {
				interrupted.set(true);
				Thread.currentThread().interrupt();
			}
		});

		assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
		assertThat(this.manager.cancel(runId)).isTrue();
		awaitInterrupted(interrupted);
		awaitInactive(runId);
		assertThat(interrupted).isTrue();
	}

	@Test
	void rejectsDuplicateActiveTaskForRun() {
		RunId runId = RunId.newId();
		CountDownLatch release = new CountDownLatch(1);
		this.manager.start(runId, () -> await(release));

		assertThatThrownBy(() -> this.manager.start(runId, () -> { }))
				.isInstanceOf(IllegalStateException.class);
		release.countDown();
	}

	private void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private void awaitInactive(RunId runId) throws InterruptedException {
		for (int index = 0; index < 20 && this.manager.isActive(runId); index++) {
			Thread.sleep(25);
		}
	}

	private void awaitInterrupted(AtomicBoolean interrupted) throws InterruptedException {
		for (int index = 0; index < 20 && !interrupted.get(); index++) {
			Thread.sleep(25);
		}
	}
}
