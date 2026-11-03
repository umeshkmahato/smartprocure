package com.smartprocure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA and Transaction configuration.
 *
 * <p>{@code @EnableJpaAuditing} is placed here — not on
 * {@link com.smartprocure.SmartProcureApplication} — so that
 * {@code @WebMvcTest} slices can load without starting the full JPA context
 * (avoids "No auditor available" errors in controller-only tests).
 *
 * <p>{@code auditorAwareRef} points to
 * {@link com.smartprocure.common.audit.SpringSecurityAuditorAware}, which
 * resolves the currently authenticated username from the Spring Security
 * context for automatic population of {@code createdBy} / {@code updatedBy}
 * columns on every entity that extends {@link com.smartprocure.common.audit.AuditableEntity}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
@EnableTransactionManagement
public class JpaConfig {
    // Datasource, HikariCP pool, and Hibernate properties are configured
    // in application.yml. This class activates auditing and transaction management.
}
