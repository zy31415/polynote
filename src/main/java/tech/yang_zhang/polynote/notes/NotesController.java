package tech.yang_zhang.polynote.notes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notes")
public class NotesController {

    private static final Logger log = LoggerFactory.getLogger(NotesController.class);

    @PostMapping
    public ResponseEntity<Void> createNote() {
        log.info("POST /notes invoked");
        return ResponseEntity.accepted().build();
    }
}
