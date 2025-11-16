package tech.yang_zhang.polynote.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(
        @NotBlank(message = "title is required") String title,
        String body
) {}
