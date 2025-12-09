package tech.yang_zhang.polynote.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tech.yang_zhang.polynote.config.AppEnvironmentProperties;
import tech.yang_zhang.polynote.dao.ReplicationLogDao;
import tech.yang_zhang.polynote.model.Note;
import tech.yang_zhang.polynote.model.OperationType;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;

@Service
public class ReplicationLogService {

    private static final Logger log = LoggerFactory.getLogger(ReplicationLogService.class);

    private final ReplicationLogDao replicationLogDao;
    private final AppEnvironmentProperties properties;
    private final ObjectMapper objectMapper;
    private final ReplicationSyncService replicationSyncService;

    public ReplicationLogService(ReplicationLogDao replicationLogDao,
                                 AppEnvironmentProperties properties,
                                 ObjectMapper objectMapper,
                                 LamportClockService lamportClockService,
                                 ReplicationSyncService replicationSyncService) {
        this.replicationLogDao = replicationLogDao;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.replicationSyncService = replicationSyncService;
    }

    public void recordCreate(Note note) {
        writeEntry(OperationType.CREATE, note);
    }

    public void recordUpdate(Note note) {
        writeEntry(OperationType.UPDATE, note);
    }

    public List<ReplicationLogEntry> getReplicationLog(@Nullable String since) {
        if (since == null || since.isBlank()) {
            return replicationLogDao.findSince(null);
        }

        if (replicationLogDao.findByOpId(since).isEmpty()) {
            throw new IllegalArgumentException("Unknown replication op_id: " + since);
        }

        return replicationLogDao.findSince(since);
    }

    public void replicationSync(String nodeId) {
        log.info("Replication sync triggered for nodeId={}", nodeId);
        replicationSyncService.sync(nodeId);
    }

    public void recordDelete(Note note, long time) {
        writeEntry(OperationType.DELETE, note.id(), serialize(note), time);
    }

    private void writeEntry(OperationType type, Note note) {
        writeEntry(type, note.id(), serialize(note), note.updatedAt());
    }

    private void writeEntry(OperationType type, String noteId, String payload, Long time) {
        ReplicationLogEntry entry = new ReplicationLogEntry(
                UUID.randomUUID().toString(),
                time,
                properties.podName(),
                type,
                noteId,
                payload
        );
        replicationLogDao.insert(entry);

        // Register afterCommit synchronization to log after transaction commits
        // This ensures that the log is only recorded if the surrounding transaction is successful
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Replication log entry recorded: type={} noteId={} opId={} time={}", type, noteId, entry.opId(), time);
                    }
                }
        );
    }

    private String serialize(Note note) {
        try {
            return objectMapper.writeValueAsString(note);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize note for replication log", e);
        }
    }
}
