package com.smartprocure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Phase 1 Integration Test — Spring Application Context.
 *
 * <p>Validates that the complete Spring Boot context starts successfully with:
 * <ul>
 *   <li>Flyway migration V1 executed (pgcrypto, uuid-ossp, vector extensions)</li>
 *   <li>JPA schema validation passes (no entities yet → nothing to validate)</li>
 *   <li>Security filter chain configured correctly</li>
 *   <li>Actuator endpoints registered</li>
 *   <li>All Phase 1 beans (configs, filters, exception handler) wired</li>
 * </ul>
 *
 * <p>Uses {@code pgvector/pgvector:pg16} Testcontainer because the
 * {@code CREATE EXTENSION IF NOT EXISTS vector} migration requires the
 * pgvector extension, which is pre-installed in that image.
 *
 * <p>{@code @ServiceConnection} automatically sets:
 * {@code spring.datasource.url / username / password} from the running container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ApplicationContextTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("smartprocure_test")
                    .withUsername("sp_test")
                    .withPassword("sp_test_pass");

    @Test
    @DisplayName("Spring application context loads without errors")
    void contextLoads() {
        // A passing test here means:
        // ✓ All Spring beans initialised successfully
        // ✓ Flyway V1 migration executed without error
        // ✓ DataSource connection established
        // ✓ Security configuration valid
        // ✓ Virtual thread executor configured
    }
}
