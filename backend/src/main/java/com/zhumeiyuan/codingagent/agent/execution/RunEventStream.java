package com.zhumeiyuan.codingagent.agent.execution;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.zhumeiyuan.codingagent.agent.run.RunEvent;
import com.zhumeiyuan.codingagent.agent.run.RunId;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class RunEventStream {

	private static final long FIFTEEN_MINUTES = 15 * 60 * 1000L;

	private final Map<RunId, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter subscribe(RunId runId, List<RunEvent> replayEvents, boolean alreadyTerminal) {
		SseEmitter emitter = new SseEmitter(FIFTEEN_MINUTES);
		this.emitters.computeIfAbsent(runId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
		emitter.onCompletion(() -> remove(runId, emitter));
		emitter.onTimeout(() -> remove(runId, emitter));
		emitter.onError(error -> remove(runId, emitter));

		for (RunEvent event : replayEvents) {
			send(runId, emitter, event);
		}
		if (alreadyTerminal) {
			emitter.complete();
			remove(runId, emitter);
		}
		return emitter;
	}

	public void publish(RunEvent event, boolean terminal) {
		List<SseEmitter> subscribers = this.emitters.getOrDefault(event.runId(), List.of());
		for (SseEmitter emitter : List.copyOf(subscribers)) {
			send(event.runId(), emitter, event);
			if (terminal) {
				emitter.complete();
				remove(event.runId(), emitter);
			}
		}
	}

	private void send(RunId runId, SseEmitter emitter, RunEvent event) {
		try {
			emitter.send(SseEmitter.event()
					.id(Long.toString(event.sequence()))
					.name(event.type().name().toLowerCase(Locale.ROOT))
					.data(event));
		} catch (IOException | IllegalStateException ex) {
			remove(runId, emitter);
		}
	}

	private void remove(RunId runId, SseEmitter emitter) {
		List<SseEmitter> subscribers = this.emitters.get(runId);
		if (subscribers != null) {
			subscribers.remove(emitter);
			if (subscribers.isEmpty()) {
				this.emitters.remove(runId, subscribers);
			}
		}
	}
}
