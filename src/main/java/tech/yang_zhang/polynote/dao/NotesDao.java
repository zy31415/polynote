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
                        "updated_at INTEGER NOT NULL," +
                        "updated_by TEXT NOT NULL," +
                        "tomestoned INTEGER NOT NULL DEFAULT 0" +
                        ")"
        );
    }

    public void insert(Note note) {
        String sql = "INSERT INTO notes (id, title, body, updated_at, updated_by, tomestoned) " +
                "VALUES (:id, :title, :body, :updatedAt, :updatedBy, :tomestoned)";

        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "updatedAt", note.updatedAt(),
                "updatedBy", note.updatedBy(),
                "tomestoned", note.tomestoned() ? 1 : 0
        );

        jdbcTemplate.update(sql, new MapSqlParameterSource(params));
    }

    public boolean insertOrIgnore(Note note) {
        String sql = "INSERT OR IGNORE INTO notes (id, title, body, updated_at, updated_by, tomestoned) " +
                "VALUES (:id, :title, :body, :updatedAt, :updatedBy, :tomestoned)";

        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "updatedAt", note.updatedAt(),
                "updatedBy", note.updatedBy(),
                "tomestoned", note.tomestoned() ? 1 : 0
        );

        int rows = jdbcTemplate.update(sql, new MapSqlParameterSource(params));
        return rows > 0;
    }

    public List<Note> findAllNonTomestoned() {
        String sql = "SELECT id, title, body, updated_at, updated_by, tomestoned FROM notes WHERE tomestoned = 0";
        return jdbcTemplate.getJdbcTemplate().query(sql, (rs, rowNum) -> mapRow(rs));
    }

    public boolean update(Note note) {
        String sql = "UPDATE notes SET title = :title, body = :body, updated_at = :updatedAt, updated_by = :updatedBy " +
                "WHERE id = :id AND tomestoned = 0";
        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "updatedAt", note.updatedAt(),
                "updatedBy", note.updatedBy()
        );
        return jdbcTemplate.update(sql, new MapSqlParameterSource(params)) > 0;
    }

    public boolean updateAtTs(long ts, Note note) {
        String sql = "UPDATE notes SET title = :title, body = :body, updated_at = :updatedAt, updated_by = :updatedBy " +
                "WHERE id = :id AND updated_at = :ts AND tomestoned = 0";
        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "updatedAt", note.updatedAt(),
                "updatedBy", note.updatedBy(),
                "ts", ts
        );
        return jdbcTemplate.update(sql, new MapSqlParameterSource(params)) > 0;
    }

    public Note deleteAndReturn(String id, long newUpdatedAt, String updatedBy) {
        String sql = "UPDATE notes SET tomestoned = 1, updated_at = :newUpdatedAt, updated_by = :updatedBy " +
                "WHERE id = :id AND tomestoned = 0 " +
                "RETURNING id, title, body, updated_at, updated_by, tomestoned";
        Map<String, Object> params = Map.of(
                "id", id,
                "newUpdatedAt", newUpdatedAt,
                "updatedBy", updatedBy
        );
        return updateWithParams(sql, new MapSqlParameterSource(params), id);
    }

    public Note deleteAndReturn(String noteId, long oldUpdatedAt, long newUpdatedAt, String updatedBy) {
        String sql = "UPDATE notes SET tomestoned = 1, updated_at = :newUpdatedAt, updated_by = :updatedBy " +
                "WHERE id = :id AND updated_at = :oldUpdatedAt AND tomestoned = 0 " +
                "RETURNING id, title, body, updated_at, updated_by, tomestoned";
        Map<String, Object> params = Map.of(
                "id", noteId,
                "oldUpdatedAt", oldUpdatedAt,
                "newUpdatedAt", newUpdatedAt,
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
                rs.getLong("updated_at"),
                rs.getString("updated_by"),
                rs.getBoolean("tomestoned")
        );
    }

    public Note findById(String id) {
        String sql = "SELECT id, title, body, updated_at, updated_by, tomestoned FROM notes WHERE id = :id";
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
}
