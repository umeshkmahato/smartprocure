package com.smartprocure.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Date/time utility helpers for SmartProcure.
 *
 * <p>All timestamps stored in the database are UTC.
 * Formatting is locale-independent for API responses.
 */
public final class DateUtils {

    public static final ZoneId            UTC              = ZoneId.of("UTC");
    public static final DateTimeFormatter DATE_FMT         = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FMT     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static LocalDateTime now()  { return LocalDateTime.now(UTC); }
    public static LocalDate    today() { return LocalDate.now(UTC); }

    public static boolean isExpired(LocalDate date) {
        return date != null && date.isBefore(today());
    }

    public static boolean isFuture(LocalDateTime dt) {
        return dt != null && dt.isAfter(now());
    }

    public static String format(LocalDateTime dt) {
        return dt == null ? "" : DATETIME_FMT.format(dt);
    }

    public static String format(LocalDate date) {
        return date == null ? "" : DATE_FMT.format(date);
    }

    public static String formatDisplay(LocalDate date) {
        return date == null ? "" : DISPLAY_DATE_FMT.format(date);
    }
}
