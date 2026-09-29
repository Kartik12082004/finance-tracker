package com.kartik.finance_tracker;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.TimeZone;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresIntegrationTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    // One PostgreSQL container shared by every integration test class.
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDataSource(
            DynamicPropertyRegistry registry
    ) throws URISyntaxException {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        Path privateKeyPath = Path.of(
                AbstractPostgresIntegrationTest.class
                        .getClassLoader()
                        .getResource("jwt/jwt-private.pem")
                        .toURI()
        );

        registry.add(
                "jwt.private-key-path",
                privateKeyPath::toString
        );
    }
}
