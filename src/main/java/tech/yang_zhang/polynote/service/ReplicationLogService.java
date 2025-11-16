package tech.yang_zhang.polynote.service;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

    public ReplicationLogService(ReplicationLogDao replicationLogDao,
                                 AppEnvironmentProperties properties,
                                 ObjectMapper objectMapper) {
        this.replicationLogDao = replicationLogDao;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void recordCreate(Note note) {
        writeEntry(OperationType.CREATE, note);
    }

    private void writeEntry(OperationType type, Note note) {
        String payload = serialize(note);
        ReplicationLogEntry entry = new ReplicationLogEntry(
                UUID.randomUUID().toString(),
                Instant.now(),
                properties.podName(),
                type,
                note.id(),
                payload
        );
        replicationLogDao.insert(entry);
        log.info("Replication log entry recorded: type={} noteId={} opId={}", type, note.id(), entry.opId());
    }

    private String serialize(Note note) {
        try {
            return objectMapper.writeValueAsString(note);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize note for replication log", e);
        }
    }
}
