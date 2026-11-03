package com.smartprocure.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that guarantees every request carries a correlation ID.
 *
 * <p>Order: {@link Ordered#HIGHEST_PRECEDENCE} — must run before all other
 * filters so that log statements in Spring Security, Spring MVC, and business
 * layers all carry the same correlation ID in MDC.
 *
 * <p>Behaviour:
 * <ol>
 *   <li>If the request contains {@code X-Correlation-ID} header, that value is reused.</li>
 *   <li>Otherwise a new UUID is generated.</li>
 *   <li>The ID is placed in SLF4J MDC under key {@code correlationId}.</li>
 *   <li>The ID is echoed back in the response {@code X-Correlation-ID} header.</li>
 *   <li>MDC is cleared in the {@code finally} block to avoid thread-local leaks.</li>
 * </ol>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER  = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = extractOrGenerate(request);

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }

    private String extractOrGenerate(HttpServletRequest request) {
        String header = request.getHeader(CORRELATION_ID_HEADER);
        return (header != null && !header.isBlank()) ? header : UUID.randomUUID().toString();
    }
}
