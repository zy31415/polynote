package tech.yang_zhang.polynote.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.yang_zhang.polynote.config.AppEnvironmentProperties;
import tech.yang_zhang.polynote.dao.NotesDao;
import tech.yang_zhang.polynote.dto.CreateNoteRequest;
import tech.yang_zhang.polynote.dto.UpdateNoteRequest;
import tech.yang_zhang.polynote.model.Note;

import java.util.UUID;

@Service
public class NotesService {

    private final NotesDao notesDao;
    private final AppEnvironmentProperties properties;
    private final ReplicationLogService replicationLogService;
    private final LamportClockService lamportClockService;

    public NotesService(NotesDao notesDao,
                        AppEnvironmentProperties properties,
                        ReplicationLogService replicationLogService,
                        LamportClockService lamportClockService) {
        this.notesDao = notesDao;
        this.properties = properties;
        this.replicationLogService = replicationLogService;
        this.lamportClockService = lamportClockService;
    }

    /**
     * Create a new note.
     *  It's important this method is transactional to ensure consistency between note creation and replication log entry.
     *
     * @param request the request containing note details
     * @return the created note
     */
    @Transactional
    public Note createNote(CreateNoteRequest request) {
        long time = lamportClockService.getTime();
        Note note = new Note(
                UUID.randomUUID().toString(),
                request.title(),
                request.body(),
                time,
                properties.podName()
        );
        notesDao.insert(note);
        replicationLogService.recordCreate(note);
        return note;
    }

    @Transactional
    public Note updateNote(String id, UpdateNoteRequest request) {
        long time = lamportClockService.getTime();
        Note note = new Note(
                id,
                request.title(),
                request.body(),
                time,
                properties.podName()
        );

        boolean updated = notesDao.update(note);
        if (!updated) {
            throw new NoteNotFoundException(id);
        }
        replicationLogService.recordUpdate(note);
        return note;
    }

    @Transactional
    public Note updateNoteAtTs(long ts, String id, UpdateNoteRequest request) {
        long time = lamportClockService.getTime();
        Note note = new Note(
                id,
                request.title(),
                request.body(),
                time,
                properties.podName()
        );

        boolean updated = notesDao.updateAtTs(ts, note);
        if (!updated) {
            throw new NoteNotFoundException(id);
        }
        replicationLogService.recordUpdate(note);
        return note;
    }

    @Transactional
    public void deleteNoteAtTs(long ts, String id) {
        long time = lamportClockService.getTime();
        Note note = notesDao.deleteAtTsAndReturn(id, ts);
        if (note == null) {
            throw new NoteNotFoundException(id);
        }
        replicationLogService.recordDelete(note, time);
    }

    @Transactional
    public void deleteNote(String id) {
        long time = lamportClockService.getTime();
        Note note = notesDao.deleteAndReturn(id);
        if (note == null) {
            throw new NoteNotFoundException(id);
        }
        replicationLogService.recordDelete(note, time);
    }

    @Transactional(readOnly = true)
    public java.util.List<Note> listNotes() {
        return notesDao.findAll();
    }
}
