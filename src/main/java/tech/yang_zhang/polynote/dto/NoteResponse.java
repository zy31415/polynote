package tech.yang_zhang.polynote.dto;

import java.time.Instant;

import tech.yang_zhang.polynote.model.Note;

public record NoteResponse(String id, String title, String body, long updatedAt, String updatedBy) {

    public static NoteResponse from(Note note) {
        return new NoteResponse(note.id(), note.title(), note.body(), note.updatedAt(), note.updatedBy());
    }
}
