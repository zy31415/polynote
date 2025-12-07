package tech.yang_zhang.polynote.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class SQLiteConfig {

    private static final Logger log = LoggerFactory.getLogger(SQLiteConfig.class);

    private final AppEnvironmentProperties environmentProperties;

    public SQLiteConfig(AppEnvironmentProperties environmentProperties) {
        this.environmentProperties = environmentProperties;
    }

    @Bean
    public DataSource dataSource() {
        Path path = Paths.get(environmentProperties.dbPath()).toAbsolutePath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create directories for DB_PATH at " + path, e);
        }

        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + path);

        // Configure WAL mode on this database
        enableWalMode(ds);
        return ds;
    }

    private void enableWalMode(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Switch journal mode to WAL and log the actual mode returned
            try (ResultSet rs = stmt.executeQuery("PRAGMA journal_mode = WAL;")) {
                if (rs.next()) {
                    String mode = rs.getString(1);
                    log.info("SQLite journal_mode = {}", mode);
                }
            }

            // Reasonable defaults for a local-first app
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;");

        } catch (SQLException e) {
            throw new IllegalStateException("Failed to enable WAL mode for SQLite", e);
        }
    }


    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}