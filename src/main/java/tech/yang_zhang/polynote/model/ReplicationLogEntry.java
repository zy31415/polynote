package tech.yang_zhang.polynote.model;


public record ReplicationLogEntry(
        Long seq,
        String opId,
        Long ts,
        String nodeId,
        OperationType type,
        String noteId,
        String payload
) {}
