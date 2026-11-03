package com.smartprocure.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base class for all SmartProcure JPA entities.
 *
 * <p>Provides:
 * <ul>
 *   <li>Auto-generated surrogate primary key (BIGSERIAL)</li>
 *   <li>Immutable {@code created_at} / mutable {@code updated_at} timestamps</li>
 *   <li>{@code created_by} / {@code updated_by} populated from Spring Security context
 *       via {@link SpringSecurityAuditorAware}</li>
 * </ul>
 *
 * <p>{@code @EnableJpaAuditing} is configured on {@link com.smartprocure.config.JpaConfig}
 * (not on the main application class) so {@code @WebMvcTest} slices can load
 * without triggering JPA auditing infrastructure.
 *
 * <p>Entities that require optimistic-locking concurrency control declare their own
 * {@code @Version Long version} field rather than inheriting it here, because not
 * every table needs a version column and Hibernate schema validation would fail
 * if the column is absent.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
