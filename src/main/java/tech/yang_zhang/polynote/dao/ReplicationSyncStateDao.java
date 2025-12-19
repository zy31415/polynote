package tech.yang_zhang.polynote.dao;

import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;

@Repository
public class ReplicationSyncStateDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReplicationSyncStateDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void initializeSchema() {
        jdbcTemplate.getJdbcTemplate().execute(
                "CREATE TABLE IF NOT EXISTS replication_sync_state (" +
                        "node_id TEXT PRIMARY KEY," +
                        "last_synced_seq INTEGER," +
                        "last_synced_ts TEXT NOT NULL" +
                        ")"
        );
    }

    public Optional<Long> findLastSyncedSeq(String nodeId) {
        String sql = "SELECT last_synced_seq FROM replication_sync_state WHERE node_id = :nodeId";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("nodeId", nodeId), rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            long seq = rs.getLong("last_synced_seq");
            return rs.wasNull() ? Optional.empty() : Optional.of(seq);
        });
    }

    public void updateLastSyncedSeq(String nodeId, long seq) {
        String sql = "INSERT INTO replication_sync_state (node_id, last_synced_seq, last_synced_ts) " +
                "VALUES (:nodeId, :seq, :updatedAt) " +
                "ON CONFLICT(node_id) DO UPDATE SET " +
                "last_synced_seq = excluded.last_synced_seq, " +
                "last_synced_ts = excluded.last_synced_ts";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("nodeId", nodeId)
                .addValue("seq", seq)
                .addValue("updatedAt", Instant.now().toString());

        jdbcTemplate.update(sql, params);
    }
}
