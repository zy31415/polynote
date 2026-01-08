package tech.yang_zhang.polynote.dto;

import java.util.List;

import tech.yang_zhang.polynote.model.ReplicationLogEntry;

public record ReplicationLogResponse(long lamportTimestamp, List<ReplicationLogEntry> logs) {
}
