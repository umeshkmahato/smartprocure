package com.smartprocure.user.entity;

import com.smartprocure.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Core user account entity.
 *
 * <p>Extends {@link AuditableEntity} for automatic id, created_at/updated_at,
 * created_by/updated_by population. Adds an optimistic-locking {@code @Version}
 * column to prevent concurrent profile-update conflicts.
 *
 * <p>Roles are fetched EAGERLY because Spring Security requires them to be
 * available synchronously when building the {@link com.smartprocure.common.security.UserPrincipal}.
 * The role set is small (≤ 5 roles per user), so the N+1 cost is negligible.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email",      columnList = "email"),
        @Index(name = "idx_users_username",   columnList = "username"),
        @Index(name = "idx_users_status",     columnList = "status"),
        @Index(name = "idx_users_department", columnList = "department")
    }
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(exclude = {"passwordHash", "roles"})
public class User extends AuditableEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt-encoded password — never expose in DTOs or API responses. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "phone", length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // ── Convenience helpers ─────────────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(r -> r.getName() == roleName);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
