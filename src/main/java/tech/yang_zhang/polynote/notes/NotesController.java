package tech.yang_zhang.polynote.notes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notes")
public class NotesController {

    private static final Logger log = LoggerFactory.getLogger(NotesController.class);

    @PostMapping
    public ResponseEntity<Void> createNote() {
        log.info("POST /notes invoked");
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<Void> listNotes() {
        log.info("GET /notes invoked");
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateNote(@PathVariable String id) {
        log.info("PUT /notes/{} invoked", id);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable String id) {
        log.info("DELETE /notes/{} invoked", id);
        return ResponseEntity.accepted().build();
    }


}
