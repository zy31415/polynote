package tech.yang_zhang.polynote.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvironmentConfig {

    private static final String POD_NAME_ENV = "POD_NAME";
    private static final String DB_PATH_ENV = "DB_PATH";

    @Bean
    public AppEnvironmentProperties appEnvironmentProperties() {
        String podName = Optional.ofNullable(System.getenv(POD_NAME_ENV))
                .filter(s -> !s.isBlank())
                .orElseGet(() -> {
                    String user = System.getenv().getOrDefault("USER", "unknown");
                    return "local-" + user;
                });

        String dbPath = System.getenv(DB_PATH_ENV);
        if (dbPath == null || dbPath.isBlank()) {
            throw new IllegalStateException(DB_PATH_ENV + " environment variable must be set");
        }

        return new AppEnvironmentProperties(podName, dbPath);
    }
}
