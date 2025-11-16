package tech.yang_zhang.polynote.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import org.springframework.lang.Nullable;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;
import tech.yang_zhang.polynote.model.OperationType;

@Repository
public class ReplicationLogDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ReplicationLogDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void initializeSchema() {
        jdbcTemplate.getJdbcTemplate().execute(
                "CREATE TABLE IF NOT EXISTS replication_log (" +
                        "op_id TEXT PRIMARY KEY," +
                        "ts TEXT NOT NULL," +
                        "node_id TEXT NOT NULL," +
                        "type TEXT NOT NULL," +
                        "note_id TEXT NOT NULL," +
                        "payload TEXT" +
                        ")"
        );
    }

    public void insert(ReplicationLogEntry entry) {
        String sql = "INSERT INTO replication_log (op_id, ts, node_id, type, note_id, payload) " +
                "VALUES (:opId, :ts, :nodeId, :type, :noteId, :payload)";

        Map<String, Object> params = Map.of(
                "opId", entry.opId(),
                "ts", entry.timestamp().toString(),
                "nodeId", entry.nodeId(),
                "type", entry.type().name(),
                "noteId", entry.noteId(),
                "payload", entry.payload()
        );
        jdbcTemplate.update(sql, new MapSqlParameterSource(params));
    }

    public List<ReplicationLogEntry> findSince(@Nullable String sinceOpIdExclusive) {
        StringBuilder sql = new StringBuilder("SELECT op_id, ts, node_id, type, note_id, payload FROM replication_log");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (sinceOpIdExclusive != null && !sinceOpIdExclusive.isBlank()) {
            sql.append(" WHERE ts > (SELECT ts FROM replication_log WHERE op_id = :sinceOpId)");
            params.addValue("sinceOpId", sinceOpIdExclusive);
        }
        sql.append(" ORDER BY ts ASC, op_id ASC");
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> mapRow(rs));
    }

    public Optional<ReplicationLogEntry> findByOpId(String opId) {
        String sql = "SELECT op_id, ts, node_id, type, note_id, payload FROM replication_log WHERE op_id = :opId";
        return jdbcTemplate.query(sql, Map.of("opId", opId), rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(mapRow(rs));
        });
    }

    private ReplicationLogEntry mapRow(ResultSet rs) throws SQLException {
        return new ReplicationLogEntry(
                rs.getString("op_id"),
                Instant.parse(rs.getString("ts")),
                rs.getString("node_id"),
                OperationType.valueOf(rs.getString("type")),
                rs.getString("note_id"),
                rs.getString("payload")
        );
    }
}
