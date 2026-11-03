package com.smartprocure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger UI configuration.
 *
 * <p>Accessible at:
 * <ul>
 *   <li>Swagger UI  → {@code http://localhost:8080/swagger-ui.html}</li>
 *   <li>OpenAPI JSON → {@code http://localhost:8080/api-docs}</li>
 * </ul>
 *
 * <p>JWT security scheme is pre-configured so every protected endpoint
 * shows the "Authorize" lock icon in Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI smartProcureOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development"),
                        new Server()
                                .url("https://api.smartprocure.internal")
                                .description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .components(new Components()
                        .addSecuritySchemes("Bearer JWT", jwtSecurityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("SmartProcure API")
                .description("""
                        **AI-Powered Procurement and Vendor Management System**

                        Supports the full procurement lifecycle:
                        - User management and role-based access control
                        - Vendor registration, approval, and performance tracking
                        - Purchase requests with multi-level approval workflow
                        - Vendor quotation comparison and AI recommendations
                        - Purchase order management and delivery tracking
                        - Invoice processing and PO matching
                        - AI procurement assistant (RAG + tool calling)
                        - Reports and analytics dashboard
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("SmartProcure Engineering")
                        .email("engineering@smartprocure.internal"))
                .license(new License()
                        .name("Proprietary — All Rights Reserved"));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description(
                        "JWT access token obtained from `POST /api/v1/auth/login`. " +
                        "Paste the token value (without 'Bearer ' prefix)."
                );
    }
}
