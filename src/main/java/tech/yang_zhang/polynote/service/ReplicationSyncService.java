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
        log.info("Starting replication sync with nodeId={}", nodeId);

        Optional<String> lastSyncedOpId = replicationSyncStateDao.findLastSyncedOpId(nodeId);
        URI remoteLogUri = buildReplicationLogUri(nodeId, lastSyncedOpId.orElse(null));

        List<ReplicationLogEntry> remoteEntries = fetchRemoteLog(remoteLogUri);
        if (remoteEntries.isEmpty()) {
            log.info("No new replication entries from nodeId={}", nodeId);
            return;
        }

        for (ReplicationLogEntry entry : remoteEntries) {
            log.info("Fetched remote opId={} type={} noteId={} from nodeId={}",
                    entry.opId(), entry.type(), entry.noteId(), nodeId);
            // TODO: Apply the remote mutation to the local store and surface conflicts.
        }

        String latestOpId = remoteEntries.get(remoteEntries.size() - 1).opId();
        replicationSyncStateDao.updateLastSyncedOpId(nodeId, latestOpId);

        log.info("Replication sync completed for nodeId={} with {} entries. lastSyncedOpId={}.",
                nodeId, remoteEntries.size(), latestOpId);
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

    private URI buildReplicationLogUri(String nodeId, @Nullable String sinceOpId) {
        String baseUrl = "http://polynote-" + nodeId.toLowerCase();
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/replication/log");
        if (sinceOpId != null && !sinceOpId.isBlank()) {
            builder.queryParam("since", sinceOpId);
        }
        return builder.build().toUri();
    }
}
