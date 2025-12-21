package tech.yang_zhang.polynote.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;
import tech.yang_zhang.polynote.dto.CreateNoteRequest;
import tech.yang_zhang.polynote.dto.NoteResponse;
import tech.yang_zhang.polynote.dto.UpdateNoteRequest;
import tech.yang_zhang.polynote.model.Note;
import tech.yang_zhang.polynote.service.NotesService;

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
        log.info("POST /notes invoked - testing deployment 2");
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

    /**
     * Update note at specific timestamp or force update.
     * @param id
     * @param ts
     * @param force
     * @param request
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable String id,
                                                   @RequestParam(value = "ts", required = false) Long ts,
                                                   @RequestParam(value = "force", defaultValue = "false") boolean force,
                                                   @Valid @RequestBody UpdateNoteRequest request) {
        log.info("PUT /notes/{}?ts={} & force={} invoked", id, ts, force);
        Note note;
        if (ts != null) {
            note = notesService.updateNoteAtTs(ts, id, request);
            return ResponseEntity.ok(NoteResponse.from(note));
        }
        if (force) {
            note = notesService.updateNote(id, request);
            log.warn("Force UPDATE note is called");
            return ResponseEntity.ok(NoteResponse.from(note));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    // todo: tomestone instead of actuall deletion from database. This way, deletion can be treated as update.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable String id,
                                           @RequestParam(value = "ts", required = false) Long ts,
                                           @RequestParam(value = "force", defaultValue = "false") boolean force) {
        log.info("DELETE /notes/{}?ts={} & force={} invoked", id, ts, force);
        if (ts != null) {
            notesService.deleteNoteAtTs(id, ts);
            return ResponseEntity.noContent().build();
        }
        if (force) {
            notesService.deleteNote(id);
            log.warn("Force DELETE note is called");
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
