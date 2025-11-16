package tech.yang_zhang.polynote.notes.model;

import java.time.Instant;

public record Note(String id, String title, String body, Instant updatedAt, String updatedBy) {
}
