package com.smartprocure.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MoneyUtils – financial arithmetic and formatting")
class MoneyUtilsTest {

    @Test @DisplayName("multiply(unitPrice, qty) returns correct total")
    void multiply_unitPriceAndQuantity() {
        assertThat(MoneyUtils.multiply(new BigDecimal("19.99"), 5))
                .isEqualByComparingTo("99.9500");
    }

    @Test @DisplayName("multiply with null operand returns ZERO")
    void multiply_nullOperand_returnsZero() {
        assertThat(MoneyUtils.multiply(null, new BigDecimal("5"))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(MoneyUtils.multiply(new BigDecimal("5"), (BigDecimal) null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("add variadic returns correct sum")
    void add_multipleValues() {
        assertThat(MoneyUtils.add(new BigDecimal("100"), new BigDecimal("250.50"), new BigDecimal("49.99")))
                .isEqualByComparingTo("400.4900");
    }

    @Test @DisplayName("add skips null elements")
    void add_skipsNulls() {
        assertThat(MoneyUtils.add(new BigDecimal("100"), null, new BigDecimal("50")))
                .isEqualByComparingTo("150.0000");
    }

    @Test @DisplayName("subtract returns correct difference")
    void subtract_normalCase() {
        assertThat(MoneyUtils.subtract(new BigDecimal("100"), new BigDecimal("30.50")))
                .isEqualByComparingTo("69.5000");
    }

    @Test @DisplayName("subtract null a returns ZERO")
    void subtract_nullA_returnsZero() {
        assertThat(MoneyUtils.subtract(null, new BigDecimal("10"))).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("isPositive true for positive value")
    void isPositive_positive() { assertThat(MoneyUtils.isPositive(new BigDecimal("0.01"))).isTrue(); }

    @Test @DisplayName("isPositive false for zero")
    void isPositive_zero() { assertThat(MoneyUtils.isPositive(BigDecimal.ZERO)).isFalse(); }

    @Test @DisplayName("isPositive false for null")
    void isPositive_null() { assertThat(MoneyUtils.isPositive(null)).isFalse(); }

    @Test @DisplayName("isGreaterThan true when a > b")
    void isGreaterThan_larger() {
        assertThat(MoneyUtils.isGreaterThan(new BigDecimal("100"), new BigDecimal("99.99"))).isTrue();
    }

    @Test @DisplayName("isLessThan true when a < b")
    void isLessThan_smaller() {
        assertThat(MoneyUtils.isLessThan(new BigDecimal("9.99"), new BigDecimal("10"))).isTrue();
    }

    @Test @DisplayName("isEqual ignores trailing zeros (compareTo semantics)")
    void isEqual_ignoresScale() {
        assertThat(MoneyUtils.isEqual(new BigDecimal("10.00"), new BigDecimal("10.0000"))).isTrue();
    }

    @Test @DisplayName("Financial precision: 0.1 + 0.2 = 0.3 (no binary FP error)")
    void financialPrecision_noBinaryFloatingPointError() {
        // double: 0.1 + 0.2 = 0.30000000000000004 — BigDecimal must give exactly 0.3
        assertThat(MoneyUtils.add(new BigDecimal("0.1"), new BigDecimal("0.2")))
                .isEqualByComparingTo("0.3");
    }

    @Test @DisplayName("format returns currency string with commas")
    void format_returnsCurrencyString() {
        assertThat(MoneyUtils.format(new BigDecimal("12345.67"))).contains("12,345.67");
    }

    @Test @DisplayName("format null returns '$0.00'")
    void format_null() { assertThat(MoneyUtils.format(null)).isEqualTo("$0.00"); }

    @Test @DisplayName("formatPlain returns plain decimal string")
    void formatPlain_returnsDecimalString() {
        assertThat(MoneyUtils.formatPlain(new BigDecimal("1500.5"))).isEqualTo("1500.50");
    }
}
