package com.smartprocure.config;

import com.smartprocure.common.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 7 configuration.
 *
 * <p><b>Phase 1 – Foundation:</b> Security filter chain is defined with all correct
 * production settings. JWT authentication filter and token validation are added in
 * Phase 2. Public endpoint list is pre-declared so Phase 2 only adds the filter bean.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Stateless sessions (NEVER store authentication state on server)</li>
 *   <li>CSRF disabled – REST API with JWT in HttpOnly cookies; CSRF handled separately</li>
 *   <li>BCrypt strength 12 – OWASP recommended minimum for 2025</li>
 *   <li>{@code @EnableMethodSecurity} – field-level authorization via {@code @PreAuthorize}</li>
 *   <li>Secure headers – X-Frame-Options, CSP, HSTS configured at filter level</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /** Endpoints that never require authentication. */
    private static final String[] PUBLIC_MATCHERS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── Sessions: stateless – no HttpSession created ────────────────
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── CSRF: disabled for stateless REST API ───────────────────────
            // JWT is stored in HttpOnly cookie; CSRF mitigation applied at
            // SameSite=Strict cookie level. Re-evaluate if adding form-based flows.
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS: resolved via CorsConfigurationSource bean below ─────────
            .cors(Customizer.withDefaults())

            // ── Authorisation rules ─────────────────────────────────────────
            // Phase 1: permit all to allow context load verification.
            // Phase 2: replace .anyRequest().permitAll() with .anyRequest().authenticated()
            // and add the JWT authentication filter.
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_MATCHERS).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            )

            // ── Exception handling (Phase 2 wires real handlers) ────────────
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(
                                "{\"errorCode\":\"UNAUTHORIZED\",\"status\":401," +
                                "\"message\":\"Authentication required\"}");
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write(
                                "{\"errorCode\":\"FORBIDDEN\",\"status\":403," +
                                "\"message\":\"Access denied\"}");
                    })
            )

            // ── Security headers ────────────────────────────────────────────
            .headers(headers -> headers
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(cto -> {})
                    .httpStrictTransportSecurity(hsts -> hsts
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31_536_000))
                    .contentSecurityPolicy(csp ->
                            csp.policyDirectives(
                                    "default-src 'self'; " +
                                    "frame-ancestors 'none'; " +
                                    "script-src 'self'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data:"))
            )

            // ── Disable form login and HTTP Basic ───────────────────────────
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        // JWT filter runs before Spring Security's username/password filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt with strength 12 – deliberately slow to resist brute-force.
     * Used by AuthService in Phase 2 for password hashing and verification.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * CORS configuration source consumed by Spring Security's cors() configurer.
     * Allows the React frontend origin to make credentialed cross-origin requests
     * (required for HttpOnly JWT refresh-token cookies).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Correlation-ID",
                "X-Requested-With", "Accept", "Origin"));
        config.setExposedHeaders(List.of("X-Correlation-ID", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
