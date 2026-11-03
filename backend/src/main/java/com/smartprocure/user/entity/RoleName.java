package com.smartprocure.user.entity;

/** System roles — persisted as VARCHAR via @Enumerated(EnumType.STRING). */
public enum RoleName {
    EMPLOYEE,
    MANAGER,
    PROCUREMENT_ADMIN,
    FINANCE,
    ADMIN
}
