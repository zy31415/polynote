package tech.yang_zhang.polynote.notes.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(
        @NotBlank(message = "title is required") String title,
        String body,
        @NotBlank(message = "updatedBy is required") String updatedBy
) {}
