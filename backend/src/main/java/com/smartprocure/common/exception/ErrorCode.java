package com.smartprocure.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Centralised catalogue of all SmartProcure business error codes.
 *
 * <p>Every error returned by the API carries one of these codes so the frontend
 * can make programmatic decisions (show retry button, redirect to login, etc.)
 * without parsing human-readable messages.
 *
 * <p>Convention: {@code HTTP status} drives the HTTP response code;
 * {@code defaultMessage} is used when the thrower provides no override.
 */
public enum ErrorCode {

    // ── Generic ────────────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST,                "Validation failed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND,                "Resource not found"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,                   "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN,                         "Access denied"),

    // ── User / Authentication ──────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND,                "User not found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT,            "User already exists"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT,           "Email address is already registered"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED,        "Invalid username or password"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN,              "Account is disabled"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN,                "Account is locked"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED,              "Token has expired"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED,              "Token is invalid"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED,   "Refresh token not found or already used"),

    // ── Vendor ────────────────────────────────────────────────────────────
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND,          "Vendor not found"),
    VENDOR_ALREADY_EXISTS(HttpStatus.CONFLICT,      "A vendor with this email or tax ID already exists"),
    VENDOR_NOT_APPROVED(HttpStatus.BAD_REQUEST,     "Vendor has not been approved"),
    VENDOR_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "Vendor is already approved"),
    VENDOR_INACTIVE(HttpStatus.BAD_REQUEST,         "Vendor is inactive"),
    VENDOR_CONTACT_NOT_FOUND(HttpStatus.NOT_FOUND,  "Vendor contact not found"),

    // ── Purchase Request ──────────────────────────────────────────────────
    PURCHASE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND,        "Purchase request not found"),
    PURCHASE_REQUEST_NOT_DRAFT(HttpStatus.BAD_REQUEST,      "Purchase request is not in DRAFT status"),
    PURCHASE_REQUEST_NOT_SUBMITTED(HttpStatus.BAD_REQUEST,  "Purchase request has not been submitted for approval"),
    PURCHASE_REQUEST_ALREADY_APPROVED(HttpStatus.BAD_REQUEST,  "Purchase request has already been approved"),
    PURCHASE_REQUEST_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "Purchase request has already been cancelled"),
    PURCHASE_REQUEST_UNAUTHORIZED(HttpStatus.FORBIDDEN,     "Not authorised to modify this purchase request"),
    PURCHASE_REQUEST_EMPTY_ITEMS(HttpStatus.BAD_REQUEST,    "Purchase request must contain at least one line item"),
    // aliases used by PurchaseRequestService
    PR_NOT_FOUND(HttpStatus.NOT_FOUND,            "Purchase request not found"),
    PR_NOT_EDITABLE(HttpStatus.BAD_REQUEST,       "Purchase request cannot be edited in its current state"),
    PR_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid purchase request status transition"),
    PR_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND,       "Purchase request line item not found"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN,           "You do not have permission to perform this action"),

    // ── Approval ──────────────────────────────────────────────────────────
    APPROVAL_THRESHOLD_NOT_FOUND(HttpStatus.NOT_FOUND,      "Approval threshold configuration not found"),
    APPROVAL_UNAUTHORIZED(HttpStatus.FORBIDDEN,             "Not authorised to approve this request"),
    APPROVAL_ALREADY_ACTIONED(HttpStatus.BAD_REQUEST,       "This approval action has already been completed"),
    APPROVAL_SELF_APPROVAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Self-approval is not permitted"),
    APPROVAL_WRONG_LEVEL(HttpStatus.BAD_REQUEST,            "Approval action is not valid for the current level"),
    DELEGATION_NOT_FOUND(HttpStatus.NOT_FOUND,              "Approval delegation not found"),
    DELEGATION_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST,   "Delegation end date must be after start date"),
    DELEGATION_ALREADY_EXISTS(HttpStatus.CONFLICT,          "An active delegation already exists for this period"),

    // ── Quotation ─────────────────────────────────────────────────────────
    QUOTATION_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND,   "Quotation request not found"),
    QUOTATION_NOT_FOUND(HttpStatus.NOT_FOUND,           "Quotation not found"),
    QUOTATION_EXPIRED(HttpStatus.BAD_REQUEST,           "Quotation validity date has passed"),
    QUOTATION_ALREADY_SELECTED(HttpStatus.BAD_REQUEST,  "A winning quotation has already been selected"),
    QUOTATION_VENDOR_NOT_INVITED(HttpStatus.BAD_REQUEST,"This vendor was not invited to submit a quotation"),

    // ── Purchase Order ────────────────────────────────────────────────────
    PURCHASE_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND,          "Purchase order not found"),
    PURCHASE_ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST,    "Purchase order cannot be cancelled in its current state"),
    PURCHASE_ORDER_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "Invalid purchase order status transition"),
    PURCHASE_ORDER_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST,"Purchase order has already been confirmed"),

    // ── Invoice ───────────────────────────────────────────────────────────
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND,          "Invoice not found"),
    INVOICE_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST,  "Invoice total does not match the purchase order total"),
    INVOICE_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "Invoice has already been verified"),
    INVOICE_PO_NOT_DELIVERED(HttpStatus.BAD_REQUEST, "Cannot process invoice: purchase order has not been delivered"),
    INVOICE_DUPLICATE(HttpStatus.CONFLICT,           "An invoice with this number already exists"),

    // ── AI ────────────────────────────────────────────────────────────────
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI service is temporarily unavailable"),
    AI_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY,              "AI provider returned an error"),
    AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT,                 "AI request timed out"),

    // ── File Upload ───────────────────────────────────────────────────────
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST,        "File type is not allowed"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST,           "File size exceeds the maximum allowed limit"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND,                 "File not found"),

    // ── Rate Limiting ─────────────────────────────────────────────────────
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests — please try again later"),

    // ── Concurrency ───────────────────────────────────────────────────────
    CONCURRENT_UPDATE_CONFLICT(HttpStatus.CONFLICT,
            "Record was modified by another user — please refresh and try again");

    // ── Enum internals ─────────────────────────────────────────────────────
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus()    { return httpStatus; }
    public String getDefaultMessage()    { return defaultMessage; }
}
