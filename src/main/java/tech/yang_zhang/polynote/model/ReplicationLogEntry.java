package tech.yang_zhang.polynote.model;

import java.time.Instant;

public record ReplicationLogEntry(
        String opId,
        Instant timestamp,
        String nodeId,
        OperationType type,
        String noteId,
        String payload
) {}
