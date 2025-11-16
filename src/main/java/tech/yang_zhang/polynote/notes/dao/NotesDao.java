package tech.yang_zhang.polynote.notes.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import tech.yang_zhang.polynote.notes.model.Note;

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
                        "updated_at TEXT NOT NULL," +
                        "updated_by TEXT NOT NULL" +
                        ")"
        );
    }

    public void insert(Note note) {
        String sql = "INSERT INTO notes (id, title, body, updated_at, updated_by) " +
                "VALUES (:id, :title, :body, :updatedAt, :updatedBy)";

        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "updatedAt", note.updatedAt().toString(),
                "updatedBy", note.updatedBy()
        );

        jdbcTemplate.update(sql, new MapSqlParameterSource(params));
    }

    public Optional<Note> findById(String id) {
        String sql = "SELECT id, title, body, updated_at, updated_by FROM notes WHERE id = :id";
        return jdbcTemplate.query(sql, Map.of("id", id), (ResultSet rs) -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(mapRow(rs));
        });
    }

    public boolean update(Note note) {
        String sql = "UPDATE notes SET title = :title, body = :body, updated_at = :updatedAt, updated_by = :updatedBy " +
                "WHERE id = :id";
        Map<String, Object> params = Map.of(
                "id", note.id(),
                "title", note.title(),
                "body", note.body(),
                "updatedAt", note.updatedAt().toString(),
                "updatedBy", note.updatedBy()
        );
        return jdbcTemplate.update(sql, new MapSqlParameterSource(params)) > 0;
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        return new Note(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("body"),
                Instant.parse(rs.getString("updated_at")),
                rs.getString("updated_by")
        );
    }
}
