package tech.yang_zhang.polynote.dao;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import tech.yang_zhang.polynote.model.OperationType;
import tech.yang_zhang.polynote.model.ReplicationLogEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
                        "seq INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "op_id TEXT NOT NULL UNIQUE," +
                        "ts INTEGER NOT NULL," +
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
                "ts", entry.ts(),
                "nodeId", entry.nodeId(),
                "type", entry.type().name(),
                "noteId", entry.noteId(),
                "payload", entry.payload()
        );
        jdbcTemplate.update(sql, new MapSqlParameterSource(params));
    }

    public void insertOrIgnore(ReplicationLogEntry entry) {
        String sql = "INSERT OR IGNORE INTO replication_log (op_id, ts, node_id, type, note_id, payload) " +
                "VALUES (:opId, :ts, :nodeId, :type, :noteId, :payload)";

        Map<String, Object> params = Map.of(
                "opId", entry.opId(),
                "ts", entry.ts(),
                "nodeId", entry.nodeId(),
                "type", entry.type().name(),
                "noteId", entry.noteId(),
                "payload", entry.payload()
        );
        jdbcTemplate.update(sql, new MapSqlParameterSource(params));
    }

    public List<ReplicationLogEntry> findSince(@Nullable Long sinceSeqExclusive) {
        StringBuilder sql = new StringBuilder("SELECT seq, op_id, ts, node_id, type, note_id, payload FROM replication_log");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (sinceSeqExclusive != null) {
            sql.append(" WHERE seq > :sinceSeqExclusive");
            params.addValue("sinceSeqExclusive", sinceSeqExclusive);
        }
        sql.append(" ORDER BY seq ASC");
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> mapRow(rs));
    }

    public long findMaxTimestamp() {
        Long maxTs = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT MAX(ts) FROM replication_log", Long.class);
        return maxTs != null ? maxTs : 0L;
    }

    private ReplicationLogEntry mapRow(ResultSet rs) throws SQLException {
        return new ReplicationLogEntry(
                rs.getLong("seq"),
                rs.getString("op_id"),
                rs.getLong("ts"),
                rs.getString("node_id"),
                OperationType.valueOf(rs.getString("type")),
                rs.getString("note_id"),
                rs.getString("payload")
        );
    }
}
