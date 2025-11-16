package tech.yang_zhang.polynote.notes.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import tech.yang_zhang.polynote.config.AppEnvironmentProperties;
import tech.yang_zhang.polynote.notes.dao.NotesDao;
import tech.yang_zhang.polynote.notes.dto.CreateNoteRequest;
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
}
