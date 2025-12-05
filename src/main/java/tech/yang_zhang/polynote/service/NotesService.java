package tech.yang_zhang.polynote.service;

import org.springframework.stereotype.Service;
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

    public Note createNote(CreateNoteRequest request) {
        // todo: not thread safe. Need to lock current time first
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

    public Note updateNote(String id, UpdateNoteRequest request) {
        // todo: not thread safe. Need to lock current time first
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

    public void deleteNote(String id) {
        Note note = notesDao.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));

        // todo: not thread safe. Need to lock current time first
        long time = lamportClockService.getTime();
        boolean deleted = notesDao.delete(id);
        if (!deleted) {
            throw new IllegalStateException("Failed to delete note with id=" + id);
        }

        replicationLogService.recordDelete(note, time);
    }

    public java.util.List<Note> listNotes() {
        return notesDao.findAll();
    }
}
