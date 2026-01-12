package tech.yang_zhang.polynote.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import tech.yang_zhang.polynote.model.Note;

@Repository
public class NotesDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public NotesDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void initializeSchema() {
        jdbcTemplate.getJdbcTemplate().execute(
                "CREATE TABLE IF NOT EXISTS notes (" +
                        "id TEXT PRIMARY KEY," +
                        "title TEXT NOT NULL," +
                        "body TEXT," +
                        "ts TEXT NOT NULL," +  // logical time stamp
                        "updated_at integer NOT NULL," + // physical time stamp
                        "updated_by TEXT NOT NULL," +
                        "tomestoned INTEGER NOT NULL DEFAULT 0" +
                        ")"
        );
    }

    public void insert(Note note) {
        String sql = "INSERT INTO notes (id, title, body, ts, updated_at, updated_by, tomestoned) " +
                "VALUES (:id, :title, :body, :ts, :updatedAt, :updatedBy, :tomestoned)";

        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "ts", note.ts(),
                "updatedAt", note.updatedAt(),
                "updatedBy", note.updatedBy(),
                "tomestoned", note.tomestoned() ? 1 : 0
        );

        jdbcTemplate.update(sql, new MapSqlParameterSource(params));
    }

    public boolean insertOrIgnore(Note note) {
        String sql = "INSERT OR IGNORE INTO notes (id, title, body, ts, updated_at, updated_by, tomestoned) " +
                "VALUES (:id, :title, :body, :ts, :updatedAt, :updatedBy, :tomestoned)";

        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "ts", note.ts(),
                "updatedAt", note.updatedAt(),
                "updatedBy", note.updatedBy(),
                "tomestoned", note.tomestoned() ? 1 : 0
        );

        int rows = jdbcTemplate.update(sql, new MapSqlParameterSource(params));
        return rows > 0;
    }

    public List<Note> findAllNonTomestoned() {
        String sql = "SELECT id, title, body, ts, updated_at, updated_by, tomestoned FROM notes WHERE tomestoned = 0";
        return jdbcTemplate.getJdbcTemplate().query(sql, (rs, rowNum) -> mapRow(rs));
    }

    public boolean updateNonTomestoned(Note note) {
        String sql = "UPDATE notes SET title = :title, body = :body, ts = :ts, updated_at = :updatedAt, updated_by = :updatedBy " +
                "WHERE id = :id AND tomestoned = 0";
        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "ts", note.ts(),
                "updatedAt", note.updatedAt(),
                "updatedBy", note.updatedBy()
        );
        return jdbcTemplate.update(sql, new MapSqlParameterSource(params)) > 0;
    }

    public boolean update(Note note) {
        String sql = "UPDATE notes SET title = :title, body = :body, ts = :ts, updated_at = :updated_at, updated_by = :updatedBy, tomestoned = :tomestoned " +
                "WHERE id = :id";
        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "ts", note.ts(),
                "updated_at", note.updatedAt(),
                "updatedBy", note.updatedBy(),
                "tomestoned", note.tomestoned() ? 1 : 0
        );
        return jdbcTemplate.update(sql, new MapSqlParameterSource(params)) > 0;
    }

    public boolean updateAtTs(long ts, Note note) {
        String sql = "UPDATE notes SET title = :title, body = :body, ts = :newTs, updated_at = :updated_at, updated_by = :updatedBy " +
                "WHERE id = :id AND ts = :ts AND tomestoned = 0";
        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "newTs", note.ts(),
                "updated_at", note.updatedAt(),
                "updatedBy", note.updatedBy(),
                "ts", ts
        );
        return jdbcTemplate.update(sql, new MapSqlParameterSource(params)) > 0;
    }

    public Note deleteAndReturn(String id, long newTs, String updatedBy) {
        String sql = "UPDATE notes SET tomestoned = 1, ts = :newTs, updated_at = :now, updated_by = :updatedBy " +
                "WHERE id = :id AND tomestoned = 0 " +
                "RETURNING id, title, body, ts, updated_at, updated_by, tomestoned";
        Map<String, Object> params = Map.of(
                "id", id,
                "newTs", newTs,
                "now", now(),
                "updatedBy", updatedBy
        );
        return updateWithParams(sql, new MapSqlParameterSource(params), id);
    }

    public Note deleteAndReturn(String noteId, long oldTs, long newTs, String updatedBy) {
        String sql = "UPDATE notes SET tomestoned = 1, ts = :newTs, updated_at = :now, updated_by = :updatedBy " +
                "WHERE id = :id AND ts = :oldTs AND tomestoned = 0 " +
                "RETURNING id, title, body, ts, updated_at, updated_by, tomestoned";
        Map<String, Object> params = Map.of(
                "id", noteId,
                "oldTs", oldTs,
                "newTs", newTs,
                "now", now(),
                "updatedBy", updatedBy
        );
        return updateWithParams(sql, new MapSqlParameterSource(params), noteId);
    }

    private Note updateWithParams(String sql, MapSqlParameterSource params, String id) {
        return jdbcTemplate.query(sql, params, (ResultSet rs) -> {
            if (!rs.next()) {
                return null;
            }
            Note result = mapRow(rs);
            if (rs.next()) {
                // This should never happen since id is primary key. If it does, a 500 return code will be shown.
                throw new IllegalStateException("Multiple rows returned when deleting note with id=" + id);
            }
            return result;
        });
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        return new Note(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("ts"),
                rs.getLong("updated_at"),
                rs.getString("updated_by"),
                rs.getBoolean("tomestoned")
        );
    }

    public Note findById(String id) {
        String sql = "SELECT id, title, body, ts, updated_at, updated_by, tomestoned FROM notes WHERE id = :id";
        Map<String, Object> params = Map.of("id", id);
        return jdbcTemplate.query(sql, new MapSqlParameterSource(params), (ResultSet rs) -> {
            if (!rs.next()) {
                return null;
            }
            Note result = mapRow(rs);
            if (rs.next()) {
                // This should never happen since id is primary key. If it does, a 500 return code will be shown.
                throw new IllegalStateException("Multiple rows returned when finding note with id=" + id);
            }
            return result;
        });
    }

    public void reset() {
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM notes");
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
