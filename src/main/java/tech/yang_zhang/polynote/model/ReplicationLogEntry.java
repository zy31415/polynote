package tech.yang_zhang.polynote.model;


public record ReplicationLogEntry(
        Long seq,
        String opId,
        String ts,
        String nodeId,
        OperationType type,
        String noteId,
        String payload,
        Long updatedAt
) {}
