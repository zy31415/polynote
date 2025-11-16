package tech.yang_zhang.polynote.notes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;
import tech.yang_zhang.polynote.notes.dto.CreateNoteRequest;
import tech.yang_zhang.polynote.notes.dto.NoteResponse;
import tech.yang_zhang.polynote.notes.dto.UpdateNoteRequest;
import tech.yang_zhang.polynote.notes.model.Note;
import tech.yang_zhang.polynote.notes.service.NotesService;

@RestController
@RequestMapping("/notes")
public class NotesController {

    private static final Logger log = LoggerFactory.getLogger(NotesController.class);

    private final NotesService notesService;

    public NotesController(NotesService notesService) {
        this.notesService = notesService;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(@Valid @RequestBody CreateNoteRequest request) {
        log.info("POST /notes invoked");
        Note note = notesService.createNote(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(note));
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> listNotes() {
        log.info("GET /notes invoked");
        List<NoteResponse> notes = notesService.listNotes().stream()
                .map(NoteResponse::from)
                .toList();
        return ResponseEntity.ok(notes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable String id,
                                                   @Valid @RequestBody UpdateNoteRequest request) {
        log.info("PUT /notes/{} invoked", id);
        Note note = notesService.updateNote(id, request);
        return ResponseEntity.ok(NoteResponse.from(note));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable String id) {
        log.info("DELETE /notes/{} invoked", id);
        notesService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }


}
