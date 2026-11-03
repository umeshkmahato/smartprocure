package com.smartprocure.user.service;

import com.smartprocure.common.security.UserPrincipal;
import com.smartprocure.user.entity.User;
import com.smartprocure.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security hook — loads a {@link UserPrincipal} from the database.
 *
 * <p>Called by {@link com.smartprocure.common.security.JwtAuthenticationFilter}
 * on every authenticated request to verify the JWT subject still exists and
 * is active. The query eager-loads roles to populate {@code GrantedAuthority}
 * without a second round-trip.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + usernameOrEmail));
        return UserPrincipal.create(user);
    }
}
