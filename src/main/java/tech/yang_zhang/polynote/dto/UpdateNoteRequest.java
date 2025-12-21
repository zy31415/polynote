package tech.yang_zhang.polynote.dto;

import jakarta.validation.constraints.NotBlank;

// todo: should the update request be more specific?
//  e.g. update title only, update body only, etc.
public record UpdateNoteRequest(
        @NotBlank(message = "title is required") String title,
        String body
) {}
