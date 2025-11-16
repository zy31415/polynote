package tech.yang_zhang.polynote.notes.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import tech.yang_zhang.polynote.config.AppEnvironmentProperties;
import tech.yang_zhang.polynote.notes.dao.NotesDao;
import tech.yang_zhang.polynote.notes.dto.CreateNoteRequest;
import tech.yang_zhang.polynote.notes.dto.UpdateNoteRequest;
import tech.yang_zhang.polynote.notes.model.Note;

@Service
public class NotesService {

    private final NotesDao notesDao;
    private final AppEnvironmentProperties properties;

    public NotesService(NotesDao notesDao, AppEnvironmentProperties properties) {
        this.notesDao = notesDao;
        this.properties = properties;
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
        return note;
    }

    public Note getNote(String id) {
        return notesDao.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    public java.util.List<Note> listNotes() {
        return notesDao.findAll();
    }

    public void deleteNote(String id) {
        boolean deleted = notesDao.delete(id);
        if (!deleted) {
            throw new NoteNotFoundException(id);
        }
    }
}
