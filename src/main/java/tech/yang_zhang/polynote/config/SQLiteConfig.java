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

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + path);
        return dataSource;
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
