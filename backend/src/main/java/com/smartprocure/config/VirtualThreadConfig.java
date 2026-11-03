package com.smartprocure.config;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

/**
 * Virtual Thread Configuration – Java 21 Project Loom.
 *
 * <p>Enables virtual threads for:
 * <ul>
 *   <li>Tomcat request processing (high-concurrency HTTP handling)</li>
 *   <li>Spring {@code @Async} task execution (e.g., notification dispatch)</li>
 * </ul>
 *
 * <p>Spring Boot property {@code spring.threads.virtual.enabled=true} is also
 * set in {@code application.yml} as the primary enablement mechanism. These beans
 * provide finer-grained control for specific executors (e.g., concurrent vendor
 * quotation retrieval in Phase 11).
 *
 * <p>CPU-bound tasks (report aggregation, AI post-processing) should NOT use
 * virtual threads; they are routed through a dedicated platform thread pool.
 */
@Configuration
public class VirtualThreadConfig {

    /**
     * Configures Tomcat to dispatch each HTTP request on a new virtual thread.
     * Replaces the default platform-thread pool, dramatically increasing
     * I/O-bound concurrency without platform thread exhaustion.
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> tomcatVirtualThreadCustomizer() {
        return protocolHandler ->
                protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Virtual-thread-backed {@link AsyncTaskExecutor} for Spring's {@code @Async}
     * method execution. Used by notification dispatch, email sending, and
     * asynchronous event publication.
     *
     * <p>Phase 11 will benchmark this executor against the platform-thread pool
     * under concurrent load to document throughput and latency improvements.
     */
    @Bean(name = "virtualThreadTaskExecutor")
    public AsyncTaskExecutor virtualThreadTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
