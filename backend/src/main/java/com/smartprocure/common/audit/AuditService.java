package com.smartprocure.common.audit;

import com.smartprocure.common.filter.CorrelationIdFilter;
import com.smartprocure.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central audit-log writer.
 *
 * <p>Writes to the immutable {@code audit_logs} table.
 * Uses {@code Propagation.REQUIRES_NEW} so audit entries are committed
 * even if the calling transaction rolls back (e.g., a failed approval action
 * must still leave an audit record of the attempt).
 *
 * <p>The {@code @Async} variant can be used in Phase 12 when the Kafka event
 * bus takes over audit delivery. For now, logging is synchronous.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log a business action with no value change (create / delete / status change).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, Long entityId, String action, String description) {
        persist(entityType, entityId, action, description, null, null);
    }

    /**
     * Log a business action that changes a value (update / approval decision).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, Long entityId, String action,
                    String description, String oldValue, String newValue) {
        persist(entityType, entityId, action, description, oldValue, newValue);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void persist(String entityType, Long entityId, String action,
                         String description, String oldValue, String newValue) {
        try {
            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .description(description)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .performedBy(SecurityUtils.getCurrentUsername().orElse("system"))
                    .correlationId(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // Audit failure must never break the business flow
            log.error("Failed to write audit log [entity={} id={} action={}]: {}",
                    entityType, entityId, action, ex.getMessage());
        }
    }
}
