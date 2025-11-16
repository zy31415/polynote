package tech.yang_zhang.polynote.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import tech.yang_zhang.polynote.config.AppEnvironmentProperties;
import tech.yang_zhang.polynote.dao.NotesDao;
import tech.yang_zhang.polynote.dto.CreateNoteRequest;
import tech.yang_zhang.polynote.dto.UpdateNoteRequest;
import tech.yang_zhang.polynote.model.Note;

@Service
public class NotesService {

    private final NotesDao notesDao;
    private final AppEnvironmentProperties properties;
    private final ReplicationLogService replicationLogService;

    public NotesService(NotesDao notesDao,
                        AppEnvironmentProperties properties,
                        ReplicationLogService replicationLogService) {
        this.notesDao = notesDao;
        this.properties = properties;
        this.replicationLogService = replicationLogService;
    }

    public Note createNote(CreateNoteRequest request) {
        Note note = new Note(
                UUID.randomUUID().toString(),
                request.title(),
                request.body(),
                Instant.now(),
                properties.podName()
        );
        notesDao.insert(note);
        replicationLogService.recordCreate(note);
        return note;
    }

    public Note updateNote(String id, UpdateNoteRequest request) {
        Note note = new Note(
                id,
                request.title(),
                request.body(),
                Instant.now(),
                properties.podName()
        );
        boolean updated = notesDao.update(note);
        if (!updated) {
            throw new NoteNotFoundException(id);
        }
        replicationLogService.recordUpdate(note);
        return note;
    }

    public java.util.List<Note> listNotes() {
        return notesDao.findAll();
    }

    public void deleteNote(String id) {
        boolean deleted = notesDao.delete(id);
        if (!deleted) {
            throw new NoteNotFoundException(id);
        }
        replicationLogService.recordDelete(id);
    }
}
