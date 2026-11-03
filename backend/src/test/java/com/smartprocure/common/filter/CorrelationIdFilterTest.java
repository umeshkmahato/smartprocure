package com.smartprocure.common.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest  request;
    private MockHttpServletResponse response;
    private MockFilterChain         chain;

    @BeforeEach
    void setUp() {
        filter   = new CorrelationIdFilter();
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain    = new MockFilterChain();
    }

    @AfterEach
    void cleanMdc() { MDC.clear(); }

    @Test
    @DisplayName("Generates a correlation ID when the request carries none")
    void generatesId_whenAbsent() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotNull().isNotBlank().hasSizeGreaterThan(10);
    }

    @Test
    @DisplayName("Echoes back the caller-supplied correlation ID")
    void propagatesExistingId() throws Exception {
        String supplied = "my-trace-id-abc123";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, supplied);
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo(supplied);
    }

    @Test
    @DisplayName("Clears MDC after the request completes (no thread-local leak)")
    void clearsMdc_afterRequest() throws Exception {
        filter.doFilter(request, response, chain);
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Each request gets a unique correlation ID")
    void generatesUniqueIds_perRequest() throws Exception {
        MockHttpServletResponse r1 = new MockHttpServletResponse();
        MockHttpServletResponse r2 = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), r1, new MockFilterChain());
        filter.doFilter(new MockHttpServletRequest(), r2, new MockFilterChain());
        assertThat(r1.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotEqualTo(r2.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("Blank X-Correlation-ID header treated as absent — new ID generated")
    void blankHeader_treatedAsAbsent() throws Exception {
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");
        filter.doFilter(request, response, chain);
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isNotBlank().isNotEqualTo("   ");
    }
}
