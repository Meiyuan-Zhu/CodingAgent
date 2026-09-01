package com.zhumeiyuan.codingagent.agent.execution;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.zhumeiyuan.codingagent.agent.run.RunId;
import com.zhumeiyuan.codingagent.agent.run.ToolResult;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceChangeUndoResult;
import com.zhumeiyuan.codingagent.agent.workspace.WorkspaceWriteTools;

public class WorkspaceChangeJournal {

	public static final String UNDO_SNAPSHOT_KEY = "workspaceChangeUndo";

	private final WorkspaceWriteTools workspaceWriteTools;
	private final WorkspaceChangePersistence persistence;
	private final Map<ChangeKey, Entry> entries = new ConcurrentHashMap<>();

	public WorkspaceChangeJournal(WorkspaceWriteTools workspaceWriteTools) {
		this(workspaceWriteTools, NoOpWorkspaceChangePersistence.INSTANCE);
	}

	WorkspaceChangeJournal(WorkspaceWriteTools workspaceWriteTools, WorkspaceChangePersistence persistence) {
		this.workspaceWriteTools = Objects.requireNonNull(workspaceWriteTools, "workspaceWriteTools");
		this.persistence = Objects.requireNonNull(persistence, "persistence");
		for (PersistedWorkspaceChange change : this.persistence.loadChanges()) {
			this.entries.put(new ChangeKey(change.runId(), change.toolCallId()),
					new Entry(change.snapshot(), change.state(), change.result()));
		}
	}

	public void recordIfUndoable(RunId runId, String toolName, ToolResult result) {
		Objects.requireNonNull(runId, "runId");
		Objects.requireNonNull(result, "result");
		if (!result.success() || !isChangeTool(toolName)) {
			return;
		}
		Object snapshot = result.privateMetadata().get(UNDO_SNAPSHOT_KEY);
		if (!(snapshot instanceof WorkspaceChangeUndoSnapshot undoSnapshot)) {
			return;
		}
		Entry entry = new Entry(undoSnapshot);
		this.entries.put(new ChangeKey(runId, result.toolCallId()), entry);
		this.persistence.saveChange(runId, result.toolCallId(), entry.snapshot, entry.state, entry.result);
	}

	public WorkspaceChangeUndo undo(RunId runId, String toolCallId) {
		ChangeKey key = new ChangeKey(runId, toolCallId);
		Entry entry = this.entries.get(key);
		if (entry == null) {
			throw new IllegalArgumentException("No undoable workspace change for tool call: " + toolCallId);
		}
		synchronized (entry) {
			if (entry.state == WorkspaceChangeUndoState.UNDONE) {
				throw new IllegalArgumentException("Workspace change has already been undone: " + toolCallId);
			}
			WorkspaceChangeUndoSnapshot snapshot = entry.snapshot;
			WorkspaceChangeUndoResult result = this.workspaceWriteTools.undoChange(snapshot.path(), snapshot.created(),
					snapshot.previousContent(), snapshot.expectedCurrentSha256());
			entry.state = WorkspaceChangeUndoState.UNDONE;
			entry.result = result;
			this.persistence.saveChange(runId, toolCallId, entry.snapshot, entry.state, entry.result);
			return new WorkspaceChangeUndo(toolCallId, entry.state, result);
		}
	}

	private boolean isChangeTool(String toolName) {
		return "write_file".equals(toolName) || "replace_text".equals(toolName) || "edit_file".equals(toolName);
	}

	private record ChangeKey(RunId runId, String toolCallId) {
		private ChangeKey {
			Objects.requireNonNull(runId, "runId");
			if (toolCallId == null || toolCallId.isBlank()) {
				throw new IllegalArgumentException("toolCallId must not be blank");
			}
		}
	}

	private static final class Entry {

		private final WorkspaceChangeUndoSnapshot snapshot;
		private WorkspaceChangeUndoState state = WorkspaceChangeUndoState.UNDOABLE;
		private WorkspaceChangeUndoResult result;

		private Entry(WorkspaceChangeUndoSnapshot snapshot) {
			this(snapshot, WorkspaceChangeUndoState.UNDOABLE, null);
		}

		private Entry(WorkspaceChangeUndoSnapshot snapshot, WorkspaceChangeUndoState state,
				WorkspaceChangeUndoResult result) {
			this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
			this.state = Objects.requireNonNull(state, "state");
			this.result = result;
		}
	}
}
