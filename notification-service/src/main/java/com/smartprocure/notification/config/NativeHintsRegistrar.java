package com.smartprocure.notification.config;

import com.smartprocure.notification.model.InvoiceVerifiedEvent;
import com.smartprocure.notification.model.PurchaseOrderIssuedEvent;
import com.smartprocure.notification.model.VendorApprovedEvent;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phase 15 — GraalVM Native Image runtime hints.
 *
 * <p>Registered via {@code META-INF/spring/aot.factories} so Spring AOT
 * picks them up automatically during the {@code native} Maven profile build.
 * Supplements the JSON hint files in
 * {@code META-INF/native-image/com.smartprocure/notification-service/}.
 */
public class NativeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {

        // ── Domain event records (Jackson deserialisation) ────────────────────
        hints.reflection()
                .registerType(VendorApprovedEvent.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.PUBLIC_FIELDS)
                .registerType(PurchaseOrderIssuedEvent.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.PUBLIC_FIELDS)
                .registerType(InvoiceVerifiedEvent.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.PUBLIC_FIELDS);

        // ── Java time types used in event records ────────────────────────────
        hints.reflection()
                .registerType(Instant.class,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.PUBLIC_FIELDS)
                .registerType(LocalDateTime.class,
                        MemberCategory.INVOKE_PUBLIC_METHODS)
                .registerType(UUID.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);

        // ── Resources bundled inside the native image ─────────────────────────
        hints.resources()
                .registerPattern("application.yml")
                .registerPattern("application-*.yml")
                .registerPattern("logback-spring.xml")
                .registerPattern("META-INF/spring.factories")
                .registerPattern("META-INF/spring/*.imports");

        // ── Serialisation (Java serialisation, not Jackson) ───────────────────
        hints.serialization()
                .registerType(VendorApprovedEvent.class)
                .registerType(PurchaseOrderIssuedEvent.class)
                .registerType(InvoiceVerifiedEvent.class);
    }
}
