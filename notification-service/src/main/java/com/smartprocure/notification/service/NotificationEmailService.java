package com.smartprocure.notification.service;

import com.smartprocure.notification.model.InvoiceVerifiedEvent;
import com.smartprocure.notification.model.PurchaseOrderIssuedEvent;
import com.smartprocure.notification.model.VendorApprovedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Composes and sends transactional email notifications for each domain event.
 * Uses Spring's {@link JavaMailSender} — configured for MailHog locally,
 * swap {@code MAIL_HOST/PORT} to a production SMTP relay in production.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEmailService {

    private final JavaMailSender mailSender;
    private final MeterRegistry  meterRegistry;

    @Value("${app.notification.from-address:no-reply@smartprocure.internal}")
    private String fromAddress;

    // ── Vendor Approved ───────────────────────────────────────────────────────

    public void sendVendorApproved(VendorApprovedEvent event) {
        if (event.vendorEmail() == null || event.vendorEmail().isBlank()) {
            log.warn("[Email] Skipping vendor-approved email — no email address: vendorId={}",
                    event.vendorId());
            return;
        }
        String subject = "Your vendor application has been approved — SmartProcure";
        String body = """
                <h2>Congratulations, %s!</h2>
                <p>Your vendor application has been reviewed and <strong>approved</strong>
                by the SmartProcure procurement team.</p>
                <p>You are now eligible to receive Purchase Orders. Our team will be in
                touch with next steps.</p>
                <hr/>
                <p style="color:#888;font-size:12px;">
                  Approved by: %s &nbsp;|&nbsp; SmartProcure Procurement Platform
                </p>
                """.formatted(event.vendorName(), event.approvedBy());
        send(event.vendorEmail(), subject, body, "vendor.approved");
    }

    // ── Purchase Order Issued ─────────────────────────────────────────────────

    public void sendPoIssued(PurchaseOrderIssuedEvent event) {
        if (event.vendorEmail() == null || event.vendorEmail().isBlank()) {
            log.warn("[Email] Skipping PO-issued email — no email address: vendorId={}",
                    event.vendorId());
            return;
        }
        String subject = "New Purchase Order %s issued — SmartProcure".formatted(event.poNumber());
        String body = """
                <h2>New Purchase Order Received</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>A new Purchase Order has been issued to your company:</p>
                <table cellpadding="6" style="border-collapse:collapse">
                  <tr><td><strong>PO Number</strong></td><td>%s</td></tr>
                  <tr><td><strong>Amount</strong></td><td>%s %s</td></tr>
                  <tr><td><strong>Issued By</strong></td><td>%s</td></tr>
                </table>
                <p>Please log in to the SmartProcure portal to acknowledge and process
                this order.</p>
                <hr/>
                <p style="color:#888;font-size:12px;">SmartProcure Procurement Platform</p>
                """.formatted(event.vendorName(), event.poNumber(),
                              event.totalAmount().toPlainString(), event.currency(),
                              event.issuedBy());
        send(event.vendorEmail(), subject, body, "po.issued");
    }

    // ── Invoice Verified ──────────────────────────────────────────────────────

    public void sendInvoiceVerified(InvoiceVerifiedEvent event) {
        if (event.vendorEmail() == null || event.vendorEmail().isBlank()) {
            log.warn("[Email] Skipping invoice-verified email — no email address: vendorId={}",
                    event.vendorId());
            return;
        }
        String subject = "Invoice %s verified — SmartProcure".formatted(event.invoiceNumber());
        String body = """
                <h2>Invoice Verified</h2>
                <p>Dear <strong>%s</strong>,</p>
                <p>Your invoice has been successfully verified by our finance team:</p>
                <table cellpadding="6" style="border-collapse:collapse">
                  <tr><td><strong>Invoice Number</strong></td><td>%s</td></tr>
                  <tr><td><strong>PO Number</strong></td><td>%s</td></tr>
                  <tr><td><strong>Amount</strong></td><td>%s %s</td></tr>
                </table>
                <p>Payment will be processed according to the agreed payment terms.</p>
                <hr/>
                <p style="color:#888;font-size:12px;">SmartProcure Procurement Platform</p>
                """.formatted(event.vendorName(), event.invoiceNumber(), event.poNumber(),
                              event.totalAmount().toPlainString(), event.currency());
        send(event.vendorEmail(), subject, body, "invoice.verified");
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody, String metricTag) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("[Email] Sent: type={}, to={}", metricTag, to);
            meterRegistry.counter("notification.email.sent",
                    "type", metricTag, "status", "success").increment();
        } catch (Exception ex) {
            log.error("[Email] Failed to send: type={}, to={}, error={}",
                    metricTag, to, ex.getMessage(), ex);
            meterRegistry.counter("notification.email.sent",
                    "type", metricTag, "status", "error").increment();
            throw new RuntimeException("Email dispatch failed: " + ex.getMessage(), ex);
        }
    }
}
