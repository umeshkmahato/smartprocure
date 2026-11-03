package com.smartprocure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC web configuration.
 *
 * <p>CORS is handled entirely by
 * {@link SecurityConfig#corsConfigurationSource()} at the Spring Security
 * filter level. No MVC-layer CORS config is needed for a stateless REST API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Default response content type to JSON.
     * Prevents 406 errors when clients omit the Accept header.
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON);
    }
}
