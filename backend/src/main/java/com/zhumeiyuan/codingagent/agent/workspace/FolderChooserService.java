package com.zhumeiyuan.codingagent.agent.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FolderChooserService {

	private static final Duration CHOOSER_TIMEOUT = Duration.ofMinutes(2);

	public Optional<String> chooseFolder() {
		if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
			throw new IllegalStateException("Folder chooser is only available on macOS in this local demo.");
		}
		Process process = startChooser();
		try {
			boolean finished = process.waitFor(CHOOSER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
			if (!finished) {
				destroyProcessTree(process);
				throw new IllegalStateException("Folder chooser timed out.");
			}
			String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			if (process.exitValue() == 0 && !stdout.isBlank()) {
				return Optional.of(stdout);
			}
			if (stderr.toLowerCase().contains("user canceled")) {
				return Optional.empty();
			}
			throw new IllegalStateException(stderr.isBlank() ? "Folder chooser did not return a folder." : stderr);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			destroyProcessTree(process);
			throw new IllegalStateException("Folder chooser was interrupted.", ex);
		} catch (IOException ex) {
			destroyProcessTree(process);
			throw new IllegalStateException("Cannot read folder chooser output.", ex);
		}
	}

	private Process startChooser() {
		try {
			return new ProcessBuilder("osascript", "-e",
					"POSIX path of (choose folder with prompt \"Choose a local project folder\")").start();
		} catch (IOException ex) {
			throw new IllegalStateException("Cannot start macOS folder chooser.", ex);
		}
	}

	private void destroyProcessTree(Process process) {
		ProcessHandle handle = process.toHandle();
		handle.descendants().forEach(ProcessHandle::destroyForcibly);
		handle.destroyForcibly();
	}
}
