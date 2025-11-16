package tech.yang_zhang.polynote.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import tech.yang_zhang.polynote.model.ReplicationLogEntry;
import tech.yang_zhang.polynote.service.ReplicationLogService;

@RestController
@RequestMapping("/replication")
public class ReplicationLogController {

    private static final Logger log = LoggerFactory.getLogger(ReplicationLogController.class);

    private final ReplicationLogService replicationLogService;

    public ReplicationLogController(ReplicationLogService replicationLogService) {
        this.replicationLogService = replicationLogService;
    }

    @GetMapping("/log")
    public ResponseEntity<List<ReplicationLogEntry>> getReplicationLog(@RequestParam(name = "since", required = false) String since) {
        log.info("GET /replication/log invoked with since={}", since);
        try {
            List<ReplicationLogEntry> entries = replicationLogService.getReplicationLog(since);
            return ResponseEntity.ok(entries);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<Void> triggerReplicationSync() {
        log.info("POST /replication/sync invoked");
        replicationLogService.replicationSync();
        return ResponseEntity.accepted().build();
    }
}
