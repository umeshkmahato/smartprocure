package com.smartprocure.user.entity;

/**
 * Lifecycle states for a SmartProcure user account.
 * Persisted as a VARCHAR and validated by a database CHECK constraint.
 */
public enum UserStatus {
    /** Normal working state — all operations permitted. */
    ACTIVE,

    /** Disabled by administrator — login blocked. */
    INACTIVE,

    /** Temporarily locked (e.g. too many failed logins) — login blocked. */
    LOCKED,

    /** Account created but email not yet verified (future use). */
    PENDING
}
