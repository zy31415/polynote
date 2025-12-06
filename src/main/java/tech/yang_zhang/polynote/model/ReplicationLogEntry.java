package tech.yang_zhang.polynote.model;


public record ReplicationLogEntry(
        String opId,
        Long timestamp,
        String nodeId,
        OperationType type,
        String noteId,
        String payload
) {}
