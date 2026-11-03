package com.smartprocure.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for all monetary calculations in SmartProcure.
 *
 * <p><b>Financial precision rules:</b>
 * <ul>
 *   <li>Internal storage scale: 4 decimal places (matches {@code NUMERIC(19,4)} columns)</li>
 *   <li>Display scale: 2 decimal places</li>
 *   <li>Rounding: {@link RoundingMode#HALF_UP} everywhere (standard accounting rounding)</li>
 *   <li>Never use {@code double} or {@code float} for money — always {@link BigDecimal}</li>
 * </ul>
 *
 * <p>All methods are null-safe and treat {@code null} as zero unless documented otherwise.
 */
public final class MoneyUtils {

    public static final int          SCALE        = 4;
    public static final int          DISPLAY_SCALE = 2;
    public static final RoundingMode ROUNDING     = RoundingMode.HALF_UP;
    public static final BigDecimal   ZERO         = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private MoneyUtils() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    // ── Conversion ─────────────────────────────────────────────────────────

    /** Parses a string into a scaled {@link BigDecimal}. Returns zero for null/blank input. */
    public static BigDecimal of(String value) {
        if (value == null || value.isBlank()) return ZERO;
        return new BigDecimal(value.trim()).setScale(SCALE, ROUNDING);
    }

    /** Converts a {@code double} to a {@link BigDecimal} safely (avoids binary floating-point issues). */
    public static BigDecimal of(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING);
    }

    /** Scales an existing {@link BigDecimal} to the standard storage scale. */
    public static BigDecimal scale(BigDecimal value) {
        if (value == null) return ZERO;
        return value.setScale(SCALE, ROUNDING);
    }

    // ── Arithmetic ─────────────────────────────────────────────────────────

    /** Multiplies two {@link BigDecimal} values with standard scale. Null-safe (null treated as zero). */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return ZERO;
        return a.multiply(b).setScale(SCALE, ROUNDING);
    }

    /** Multiplies unit price by integer quantity — the core purchase-request calculation. */
    public static BigDecimal multiply(BigDecimal unitPrice, int quantity) {
        return multiply(unitPrice, BigDecimal.valueOf(quantity));
    }

    /** Sums a variadic list of {@link BigDecimal} values. Null elements are skipped. */
    public static BigDecimal add(BigDecimal... values) {
        BigDecimal total = ZERO;
        for (BigDecimal v : values) {
            if (v != null) total = total.add(v);
        }
        return total.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        if (a == null) a = ZERO;
        if (b == null) b = ZERO;
        return a.subtract(b).setScale(SCALE, ROUNDING);
    }

    // ── Comparisons ────────────────────────────────────────────────────────

    public static boolean isPositive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isZeroOrNegative(BigDecimal v) {
        return v == null || v.compareTo(BigDecimal.ZERO) <= 0;
    }

    public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) > 0;
    }

    public static boolean isGreaterThanOrEqual(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) >= 0;
    }

    public static boolean isLessThan(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) < 0;
    }

    public static boolean isLessThanOrEqual(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return false;
        return a.compareTo(b) <= 0;
    }

    /** Returns true if {@code a} and {@code b} are numerically equal (ignores scale). */
    public static boolean isEqual(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    // ── Formatting ─────────────────────────────────────────────────────────

    /** Formats as {@code $12,345.67}. Returns {@code "$0.00"} for null. */
    public static String format(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    /** Formats as plain numeric string with 2 decimal places, e.g. {@code "12345.67"}. */
    public static String formatPlain(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(DISPLAY_SCALE, ROUNDING).toPlainString();
    }
}
