package tech.yang_zhang.polynote.notes.dao;

import java.util.Map;

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
}
