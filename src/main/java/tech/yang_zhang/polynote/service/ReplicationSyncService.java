package tech.yang_zhang.polynote.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import tech.yang_zhang.polynote.dao.ReplicationSyncStateDao;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;

@Service
public class ReplicationSyncService {

    private static final Logger log = LoggerFactory.getLogger(ReplicationSyncService.class);

    private final ReplicationSyncStateDao replicationSyncStateDao;
    private final RestTemplate restTemplate;

    public ReplicationSyncService(ReplicationSyncStateDao replicationSyncStateDao,
                                  RestTemplateBuilder restTemplateBuilder) {
        this.replicationSyncStateDao = replicationSyncStateDao;
        this.restTemplate = restTemplateBuilder.build();
    }

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

        for (ReplicationLogEntry entry : remoteEntries) {
            log.info("Fetched remote seq={} opId={} ts={} noteId={} type={} payload={}",
                    entry.seq(), entry.opId(), entry.ts(), entry.nodeId(), entry.type(), entry.payload());
            // TODO: Apply the remote mutation to the local store and surface conflicts.
            // for each log entry:
            //  1. update Lamport clock atomically using getAndUpdate.
            //  2. In one transaction:
            //     a. apply the mutation to the local note store
            //     b. insert the replication log entry into local replication log table
            //     c. update the last synced seq for the remote node
            //  Question: how does the updated Lamport clock get reflected in the log entry?
        }

        // todo: in the above loop, we should update lastSyncedSeq incrementally instead of at the end.
        Long latestSeq = remoteEntries.get(remoteEntries.size() - 1).seq();
        if (latestSeq == null) {
            throw new IllegalStateException("Remote replication entry missing sequence value");
        }
        replicationSyncStateDao.updateLastSyncedSeq(nodeId, latestSeq);

        log.info("Replication sync completed for nodeId={} with {} entries. lastSyncedSeq={}.",
                nodeId, remoteEntries.size(), latestSeq);
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
}
