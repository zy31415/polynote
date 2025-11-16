package tech.yang_zhang.polynote.dao;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;

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
}
