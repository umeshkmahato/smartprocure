package com.smartprocure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SmartProcure – AI-Powered Procurement and Vendor Management System.
 *
 * <p>Architecture: Modular Monolith → Microservice-ready domain boundaries.
 * <p>Java 21 Virtual Threads enabled via {@code spring.threads.virtual.enabled=true}.
 * <p>JPA Auditing enabled in {@link com.smartprocure.config.JpaConfig} (not here,
 * so {@code @WebMvcTest} slices can load without the full JPA context).
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SmartProcureApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartProcureApplication.class, args);
    }
}
