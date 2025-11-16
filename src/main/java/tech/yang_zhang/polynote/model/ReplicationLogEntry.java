package tech.yang_zhang.polynote.model;

import java.time.Instant;

public record ReplicationLogEntry(
        String opId,
        Instant timestamp,
        String nodeId,
        String type,
        String noteId,
        String payload
) {}
