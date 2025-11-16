package tech.yang_zhang.polynote.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/replication")
public class ReplicationLogController {

    private static final Logger log = LoggerFactory.getLogger(ReplicationLogController.class);

    @GetMapping("/log")
    public ResponseEntity<Void> getReplicationLog(@RequestParam(name = "since", required = false) String since) {
        log.info("GET /replication/log invoked with since={}", since);
        return ResponseEntity.accepted().build();
    }
}
