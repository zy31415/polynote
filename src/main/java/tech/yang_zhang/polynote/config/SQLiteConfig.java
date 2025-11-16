package tech.yang_zhang.polynote.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.sqlite.SQLiteDataSource;

@Configuration
public class SQLiteConfig {

    private static final String DB_PATH_ENV = "DB_PATH";

    @Bean
    public DataSource dataSource() {
        String dbPath = System.getenv(DB_PATH_ENV);
        if (dbPath == null || dbPath.isBlank()) {
            throw new IllegalStateException(DB_PATH_ENV + " environment variable must be set to a writable SQLite file path");
        }

        Path path = Paths.get(dbPath).toAbsolutePath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create directories for DB_PATH at " + path, e);
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + path);
        return dataSource;
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
