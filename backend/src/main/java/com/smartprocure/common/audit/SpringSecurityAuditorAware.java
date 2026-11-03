package com.smartprocure.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the currently authenticated username for JPA auditing.
 *
 * <p>Populates {@code created_by} and {@code updated_by} columns on every
 * entity extending {@link AuditableEntity} automatically on each
 * insert / update via Spring Data JPA's auditing infrastructure.
 *
 * <p>Falls back to {@code "system"} for:
 * <ul>
 *   <li>Unauthenticated requests (public endpoints)</li>
 *   <li>Background jobs and scheduled tasks</li>
 *   <li>Flyway data seeds</li>
 * </ul>
 */
@Component("springSecurityAuditorAware")
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return Optional.of("system");
        }

        return Optional.of(auth.getName());
    }
}
