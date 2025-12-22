package tech.yang_zhang.polynote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.yang_zhang.polynote.dao.NotesDao;
import tech.yang_zhang.polynote.dao.ReplicationLogDao;
import tech.yang_zhang.polynote.dao.ReplicationSyncStateDao;
import tech.yang_zhang.polynote.model.Note;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;

@Service
public class ReplicationSyncService {

    private static final Logger log = LoggerFactory.getLogger(ReplicationSyncService.class);

    private final ReplicationSyncStateDao replicationSyncStateDao;
    private final ObjectMapper objectMapper;
    private final ReplicationLogDao replicationLogDao;
    private final NotesDao notesDao;

    public ReplicationSyncService(ReplicationSyncStateDao replicationSyncStateDao,
                                  ObjectMapper objectMapper,
                                  ReplicationLogDao replicationLogDao,
                                  NotesDao notesDao) {
        this.notesDao = notesDao;
        this.replicationLogDao = replicationLogDao;
        this.objectMapper = objectMapper;
        this.replicationSyncStateDao = replicationSyncStateDao;

    }

    // todo: think about if this is truly service code or domain logic code?
    //  For now, the class exists for the only purpose of transaction management.
    @Transactional
    public long processReplicationLog(ReplicationLogEntry entry) {
        if (appendLog(entry)) {
            log.debug("Appended new replication log entry: {}", entry);
            applyMutation(entry);
        } else {
            log.debug("Replication log entry already exists: {}", entry);
        }
        replicationSyncStateDao.updateLastSyncedSeq(entry.nodeId(), entry.seq());
        return entry.seq();
    }

    private void applyMutation(ReplicationLogEntry entry) {
        switch (entry.type()) {
            case CREATE ->
                applyCreation(entry);
            case UPDATE, DELETE -> // DELETE is treated the same as UPDATE
                applyUpdate(entry);
            default ->
                    throw new IllegalArgumentException("Unknown operation type: " + entry.type());
        }
    }

    private boolean appendLog(ReplicationLogEntry entry) {
        return replicationLogDao.insertOrIgnore(entry);
    }

    private void applyCreation(ReplicationLogEntry entry) {
        Note note = parsePayload(entry);
        if (!notesDao.insertOrIgnore(note)) {
            log.error("When syncing logs, failed to apply CREATE operation for note {} as it already exists", note.id());
            throw new IllegalStateException("Log syncing error: Failed to apply CREATE operation for note " + note.id());
        }
    }

    // todo: need to be extensively unit tested
    private void applyUpdate(ReplicationLogEntry entry) {
        String noteId = entry.noteId();
        Long newTs = entry.ts();

        Note existingNote = notesDao.findById(noteId);
        if  (existingNote == null) {
            log.error("When syncing logs, failed to apply UPDATE operation for note {} because we cannot find the note.", noteId);
            throw new IllegalStateException("Log syncing error: Failed to apply UPDATE operation for note " + noteId + " because we cannot find the note.");
        }

        Long oldTs = existingNote.updatedAt();

        if (oldTs > newTs) {
            log.info("Update log entry opId={} is not applied, as the Lamport time of existing row ts={} is greater then log ts={}", entry.opId(), oldTs, newTs);
            return;
        }

        String oldNodeId = existingNote.updatedBy();
        String newNodeId = entry.nodeId();

        if (oldNodeId.equals(newNodeId)) {
            throw new IllegalStateException("Log syncing error: Failed to apply UPDATE operation for note as node IDs are the same and this should not happen.");
        }

        if (oldTs.equals(newTs) && oldNodeId.compareTo(newNodeId) < 0) {
            log.info("Update log entry opId={} is not applied, as the Lamport time of existing row ts={} is equal to log ts={} but existing nodeId={} is lexicographically smaller than log nodeId={} (tie-breaker)", entry.opId(), oldTs, newTs, oldNodeId, newNodeId);
            return;
        }
        // Now we can apply the update
        Note note = parsePayload(entry);
        if (!notesDao.update(note)) {
            log.error("When syncing logs, failed to apply UPDATE operation for note {} as it does not exist", note.id());
            throw new IllegalStateException("Log syncing error: Failed to apply UPDATE operation for note " + note.id());
        }
    }

    private Note parsePayload(ReplicationLogEntry entry) {
        Note note;
        try {
            note = objectMapper.readValue(entry.payload(), Note.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize note from replication log entry", e);
        }
        return note;
    }
}
