package com.smartprocure.common.util;

import com.smartprocure.common.exception.ErrorCode;
import com.smartprocure.common.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Spring Security context helpers.
 *
 * <p>All methods are stateless and read from the current thread's
 * {@link SecurityContextHolder}. Safe to call from any layer.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Returns the authenticated username, or empty if request is unauthenticated. */
    public static Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(auth.getName());
    }

    /** Returns the authenticated username or throws {@link UnauthorizedException}. */
    public static String requireCurrentUsername() {
        return getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    /** Checks whether the current user has the given role (without the ROLE_ prefix). */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /** Returns true if the authenticated user's username equals the supplied value. */
    public static boolean isCurrentUser(String username) {
        return getCurrentUsername().map(u -> u.equals(username)).orElse(false);
    }
}
