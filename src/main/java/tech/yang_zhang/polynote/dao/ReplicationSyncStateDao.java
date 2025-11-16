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
                        "last_synced_op_id TEXT," +
                        "updated_at TEXT NOT NULL" +
                        ")"
        );
    }

    public Optional<String> findLastSyncedOpId(String nodeId) {
        String sql = "SELECT last_synced_op_id FROM replication_sync_state WHERE node_id = :nodeId";
        return jdbcTemplate.query(sql, new MapSqlParameterSource("nodeId", nodeId), rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.ofNullable(rs.getString("last_synced_op_id"));
        });
    }

    public void updateLastSyncedOpId(String nodeId, String opId) {
        String sql = "INSERT INTO replication_sync_state (node_id, last_synced_op_id, updated_at) " +
                "VALUES (:nodeId, :opId, :updatedAt) " +
                "ON CONFLICT(node_id) DO UPDATE SET " +
                "last_synced_op_id = excluded.last_synced_op_id, " +
                "updated_at = excluded.updated_at";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("nodeId", nodeId)
                .addValue("opId", opId)
                .addValue("updatedAt", Instant.now().toString());

        jdbcTemplate.update(sql, params);
    }
}
