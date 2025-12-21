package tech.yang_zhang.polynote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
import tech.yang_zhang.polynote.model.Note;
import tech.yang_zhang.polynote.model.OperationType;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;

import java.net.URI;
import java.util.Arrays;
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

    public List<ReplicationLogEntry> getReplicationLog(@Nullable Long since) {
        return replicationLogDao.findSince(since);
    }

    public void replicationSync(String nodeId) {
        log.info("Replication sync triggered for nodeId={}", nodeId);
        sync(nodeId);
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
        replicationLogDao.insert(entry);

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

    // todo: rethink about this function. Should it be service code or domain logic code?
    public void sync(String nodeId) {
        // todo: should the whole sync operation in a transaction? Probably not - why?
        log.info("Starting replication sync with nodeId={}", nodeId);

        Optional<Long> lastSyncedSeq = replicationSyncStateDao.findLastSyncedSeq(nodeId);
        log.debug("lastSyncedSeq={}", lastSyncedSeq);
        URI remoteLogUri = buildReplicationLogUri(nodeId, lastSyncedSeq.orElse(null));

        log.info("Fetching replication log from nodeId={} at URI={}", nodeId, remoteLogUri);
        List<ReplicationLogEntry> remoteEntries = fetchRemoteLog(remoteLogUri);
        if (remoteEntries.isEmpty()) {
            log.info("No new replication entries from nodeId={}", nodeId);
            return;
        }

        Long latestSeq = null;

        for (ReplicationLogEntry entry : remoteEntries) {
            log.info("Fetched remote seq={} opId={} ts={} noteId={} type={} payload={}",
                    entry.seq(), entry.opId(), entry.ts(), entry.nodeId(), entry.type(), entry.payload());

            // for each log entry:
            //  1. update Lamport clock atomically using getAndUpdate.
            //  2. In one transaction:
            //     a. apply the mutation to the local note store
            //     b. insert the replication log entry into local replication log table
            //     c. update the last synced seq for the remote node
            //  Question: how does the updated Lamport clock get reflected in the log entry?

            // Note: the ts here is not recorded in the system but only logged. This ts represents the event of receiving a remote log.
            //  Technically, it's OK to not tick the clock here.
            long ts = lamportClockService.syncAndTick(entry.ts());
            log.info("Remote log opId={} is received at time={}", entry.opId(), ts);

            latestSeq = replicationSyncService.processReplicationLog(entry);
        }

        log.info("Replication sync completed for nodeId={} with {} entries. lastSyncedSeq={}.",
                nodeId, remoteEntries.size(), latestSeq);
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

    private List<ReplicationLogEntry> fetchRemoteLog(URI requestUri) {
        try {
            ReplicationLogEntry[] response = restTemplate.getForObject(requestUri, ReplicationLogEntry[].class);
            if (response == null || response.length == 0) {
                return List.of();
            }
            return Arrays.asList(response);
        } catch (RestClientException e) {
            log.error("Failed to fetch replication log from URI={}", requestUri, e);
            throw new IllegalStateException("Unable to fetch replication log from peer", e);
        }
    }

}
