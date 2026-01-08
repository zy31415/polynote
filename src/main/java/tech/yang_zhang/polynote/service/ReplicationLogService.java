package tech.yang_zhang.polynote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tech.yang_zhang.polynote.config.AppEnvironmentProperties;
import tech.yang_zhang.polynote.dao.ReplicationLogDao;
import tech.yang_zhang.polynote.dao.ReplicationSyncStateDao;
import tech.yang_zhang.polynote.dto.ReplicationLogResponse;
import tech.yang_zhang.polynote.model.Note;
import tech.yang_zhang.polynote.model.OperationType;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReplicationLogService {

    private static final Logger log = LoggerFactory.getLogger(ReplicationLogService.class);

    private final ReplicationLogDao replicationLogDao;
    private final AppEnvironmentProperties properties;
    private final ObjectMapper objectMapper;
    private final ReplicationSyncStateDao replicationSyncStateDao;
    private final LamportClockService lamportClockService;
    private final ReplicationSyncService replicationSyncService;
    private final RestTemplate restTemplate;

    public ReplicationLogService(ReplicationLogDao replicationLogDao,
                                 AppEnvironmentProperties properties,
                                 ObjectMapper objectMapper,
                                 ReplicationSyncStateDao replicationSyncStateDao,
                                 LamportClockService lamportClockService,
                                 ReplicationSyncService replicationSyncService,
                                 RestTemplateBuilder restTemplateBuilder) {
        this.replicationSyncService = replicationSyncService;
        this.replicationSyncStateDao = replicationSyncStateDao;
        this.replicationLogDao = replicationLogDao;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.lamportClockService = lamportClockService;
        this.restTemplate = restTemplateBuilder.build();
    }

    public void recordCreate(Note note) {
        writeEntry(OperationType.CREATE, note);
    }

    public void recordUpdate(Note note) {
        writeEntry(OperationType.UPDATE, note);
    }

    public ReplicationLogResponse getReplicationLog(@Nullable Long since) {
        List<ReplicationLogEntry> entries = replicationLogDao.findSince(since);

        // Before returning the response, we need to tick the Lamport clock to ensure that the timestamp is updated for this event.
        // todo: This can be moved to middleware layer where Lamport clock is ticked, attached and synced for all time syncing messages.
        long lamportTimestamp = lamportClockService.tick();
        return new ReplicationLogResponse(lamportTimestamp, entries);
    }

    public void replicationSync(String nodeId) {
        // todo: should the whole sync operation in a transaction? Probably not - why?
        log.info("Starting replication sync with nodeId={}", nodeId);

        Optional<Long> lastSyncedSeq = replicationSyncStateDao.findLastSyncedSeq(nodeId);
        log.debug("lastSyncedSeq={}", lastSyncedSeq);
        URI remoteLogUri = buildReplicationLogUri(nodeId, lastSyncedSeq.orElse(null));

        log.info("Fetching replication log from nodeId={} at URI={}", nodeId, remoteLogUri);
        ReplicationLogResponse remoteResponse = fetchRemoteLog(remoteLogUri);

        // Sync the Lamport clock with the remote timestamp before processing the logs
        // todo: This can be moved to middleware layer where Lamport clock is ticked, attached and synced for all time syncing messages.
        long remoteLamportTimestamp = remoteResponse.lamportTimestamp();
        log.debug("remoteLamportTimestamp={}", remoteLamportTimestamp);

        long receivedAt = lamportClockService.syncAndTick(remoteLamportTimestamp);
        log.info("Replication log response from nodeId={} received at time={}", nodeId, receivedAt);

        // Process the logs in order
        List<ReplicationLogEntry> remoteEntries = remoteResponse.logs();
        if (remoteEntries.isEmpty()) {
            log.info("No new replication entries from nodeId={}", nodeId);
            return;
        }

        Long latestSeq = null;

        for (ReplicationLogEntry entry : remoteEntries) {
            log.info("Fetched remote seq={} opId={} ts={} noteId={} type={} payload={}",
                    entry.seq(), entry.opId(), entry.ts(), entry.noteId(), entry.type(), entry.payload());
            // Process each entry in its own transaction
            latestSeq = replicationSyncService.processReplicationLog(nodeId, entry);
        }

        log.info("Replication sync completed for nodeId={} with {} entries. lastSyncedSeq={}.",
                nodeId, remoteEntries.size(), latestSeq);
    }

    public void recordDelete(Note note, long time) {
        writeEntry(OperationType.DELETE, note.id(), serialize(note), time);
    }

    private void writeEntry(OperationType type, Note note) {
        writeEntry(type, note.id(), serialize(note), note.updatedAt());
    }

    private void writeEntry(OperationType type, String noteId, String payload, Long time) {
        ReplicationLogEntry entry = new ReplicationLogEntry(
                null,
                UUID.randomUUID().toString(),
                time,
                properties.podName(),
                type,
                noteId,
                payload
        );

        if (!replicationLogDao.insertOrIgnore(entry)) {
            // this should never happen as opId is generated as UUID
            log.error("Replication log entry already exists: opId={}", entry.opId());
            throw new IllegalStateException("Replication log entry already exists: opId=" + entry.opId());
        }

        // Register afterCommit synchronization to log after transaction commits
        // This ensures that the log is only recorded if the surrounding transaction is successful
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Replication log entry recorded: type={} noteId={} opId={} time={}", type, noteId, entry.opId(), time);
                    }
                }
        );
    }

    private String serialize(Note note) {
        try {
            return objectMapper.writeValueAsString(note);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize note for replication log", e);
        }
    }

    // todo: consider grpc call instead http
    private URI buildReplicationLogUri(String nodeId, @Nullable Long seq) {
        // todo: need a better way to manage peer addresses
        String baseUrl = "http://polynote-" + nodeId.toLowerCase() + ":8080";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/replication/log");
        if (seq != null) {
            builder.queryParam("since", seq);
        }
        return builder.build().toUri();
    }

    private ReplicationLogResponse fetchRemoteLog(URI requestUri) {
        try {
            ReplicationLogResponse response = restTemplate.getForObject(requestUri, ReplicationLogResponse.class);
            if (response == null) {
                throw new IllegalStateException("Replication log response is empty");
            }
            List<ReplicationLogEntry> logs = response.logs() == null ? List.of() : response.logs();
            return new ReplicationLogResponse(response.lamportTimestamp(), logs);
        } catch (RestClientException e) {
            log.error("Failed to fetch replication log from URI={}", requestUri, e);
            throw new IllegalStateException("Unable to fetch replication log from peer", e);
        }
    }

}
