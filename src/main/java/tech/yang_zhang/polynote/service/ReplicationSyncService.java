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
    public void processReplicationLog(ReplicationLogEntry entry) {
        Note note;
        try {
            note = objectMapper.readValue(entry.payload(), Note.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize note from replication log entry", e);
        }

        switch (entry.type()) {
            case CREATE -> {
                // todo: insert log first. If insert is ignored, then skip applying the mutation.
                appendLog(entry);
                notesDao.insertOrIgnore(note);
            }
            case UPDATE -> {

                appendLog(entry);
                // todo: handle update conflicts?
                notesDao.updateAtTs(entry.ts(), note);

            }
            case DELETE -> {
                // todo: ensure that arguments are correct
                appendLog(entry);
                notesDao.deleteAndReturn(entry.noteId(), entry.ts(), note.updatedAt(), entry.nodeId());

            }
            default -> throw new IllegalArgumentException("Unknown operation type: " + entry.type());
        }
        replicationSyncStateDao.updateLastSyncedSeq(entry.nodeId(), entry.seq());
    }

    private void appendLog(ReplicationLogEntry entry) {
        replicationLogDao.insertOrIgnore(entry);
    }

    // todo: use this method
    private void applyMutation(ReplicationLogEntry entry) {
        // todo: add implementation
    }
}
