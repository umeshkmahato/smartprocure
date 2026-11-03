package com.smartprocure.common.security;

import com.smartprocure.user.entity.User;
import com.smartprocure.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security {@link UserDetails} adapter for a SmartProcure {@link User}.
 *
 * <p>Created per-request by {@link com.smartprocure.user.service.UserDetailsServiceImpl}
 * and stored in the {@link org.springframework.security.core.context.SecurityContextHolder}.
 * Carries the user's id and email so controllers/services can access them
 * without a second DB round-trip.
 */
public class UserPrincipal implements UserDetails {

    private final Long    id;
    private final String  username;
    private final String  email;
    private final String  password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean active;

    private UserPrincipal(Long id, String username, String email, String password,
                          Collection<? extends GrantedAuthority> authorities, boolean active) {
        this.id          = id;
        this.username    = username;
        this.email       = email;
        this.password    = password;
        this.authorities = List.copyOf(authorities);
        this.active      = active;
    }

    /** Factory — converts a {@link User} entity to a {@link UserPrincipal}. */
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> auths = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                        "ROLE_" + role.getName().name()))
                .toList();

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                auths,
                user.getStatus() == UserStatus.ACTIVE
        );
    }

    // ── Extra getters not on UserDetails ──────────────────────────────────────

    public Long   getId()    { return id; }
    public String getEmail() { return email; }

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override public Collection<? extends GrantedAuthority> getAuthorities()  { return authorities; }
    @Override public String  getPassword()              { return password; }
    @Override public String  getUsername()              { return username; }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return active; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return active; }
}
