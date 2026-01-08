package tech.yang_zhang.polynote.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import tech.yang_zhang.polynote.dto.ReplicationLogResponse;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;
import tech.yang_zhang.polynote.service.LamportClockService;
import tech.yang_zhang.polynote.service.ReplicationLogService;

@RestController
@RequestMapping("/replication")
public class ReplicationLogController {

    private static final Logger log = LoggerFactory.getLogger(ReplicationLogController.class);

    private final ReplicationLogService replicationLogService;
    private final LamportClockService lamportClockService;

    public ReplicationLogController(ReplicationLogService replicationLogService,
                                    LamportClockService lamportClockService) {
        this.replicationLogService = replicationLogService;
        this.lamportClockService = lamportClockService;
    }

    @GetMapping("/log")
    public ResponseEntity<ReplicationLogResponse> getReplicationLog(@RequestParam(name = "since", required = false) Long since) {
        log.info("GET /replication/log invoked with since={}", since);
        try {
            ReplicationLogResponse response = replicationLogService.getReplicationLog(since);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    // todo: what happens if this is called multiple times concurrently?
    @PutMapping("/sync/{nodeId}")
    public ResponseEntity<Void> triggerReplicationSync(@PathVariable String nodeId) {
        log.info("PUT /replication/sync/{} invoked. Sync with node {}", nodeId, nodeId);
        replicationLogService.replicationSync(nodeId);
        return ResponseEntity.accepted().build();
    }
}
