package com.finsecure.pay;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResult;
import com.finsecure.pay.service.TransactionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidatorTest {

    private final TransactionValidator validator = new TransactionValidator();

    private TransactionRequest tx(String id, String from, String to, String amt, String cur) {
        return new TransactionRequest(id, from, to, amt == null ? null : new BigDecimal(amt), cur);
    }

    @Test
    @DisplayName("Valid transaction passes")
    void validTransaction() {
        ValidationResult r = validator.validate(tx("T1", "ACC123456", "ACC654321", "500.00", "INR"));
        assertTrue(r.valid());
        assertTrue(r.errors().isEmpty());
    }

    @Test
    @DisplayName("Zero amount fails")
    void zeroAmount() {
        ValidationResult r = validator.validate(tx("T2", "ACC123456", "ACC654321", "0", "INR"));
        assertFalse(r.valid());
        assertTrue(r.errors().contains("amount must be greater than zero"));
    }

    @Test
    @DisplayName("Negative amount fails")
    void negativeAmount() {
        assertFalse(validator.validate(tx("T3", "ACC123456", "ACC654321", "-10", "USD")).valid());
    }

    @Test
    @DisplayName("Amount above limit fails")
    void amountOverLimit() {
        ValidationResult r = validator.validate(tx("T4", "ACC123456", "ACC654321", "100000.01", "EUR"));
        assertFalse(r.valid());
    }

    @Test
    @DisplayName("Amount exactly at limit passes")
    void amountAtLimit() {
        assertTrue(validator.validate(tx("T5", "ACC123456", "ACC654321", "100000", "GBP")).valid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"JPY", "XYZ", "btc"})
    @DisplayName("Unsupported currency fails")
    void unsupportedCurrency(String currency) {
        assertFalse(validator.validate(tx("T6", "ACC123456", "ACC654321", "10", currency)).valid());
    }

    @Test
    @DisplayName("Lowercase supported currency passes")
    void lowercaseCurrency() {
        assertTrue(validator.validate(tx("T7", "ACC123456", "ACC654321", "10", "inr")).valid());
    }

    @Test
    @DisplayName("Same sender and receiver fails")
    void sameAccount() {
        ValidationResult r = validator.validate(tx("T8", "ACC123456", "ACC123456", "10", "INR"));
        assertFalse(r.valid());
        assertTrue(r.errors().contains("fromAccount and toAccount must differ"));
    }

    @Test
    @DisplayName("Invalid account format fails")
    void badAccountFormat() {
        ValidationResult r = validator.validate(tx("T9", "12345", "ACC654321", "10", "INR"));
        assertFalse(r.valid());
        assertTrue(r.errors().contains("fromAccount format invalid"));
    }

    @Test
    @DisplayName("Missing fields fail")
    void missingFields() {
        ValidationResult r = validator.validate(new TransactionRequest(null, null, null, null, null));
        assertFalse(r.valid());
        assertEquals(5, r.errors().size());
    }

    @Test
    @DisplayName("Null request fails")
    void nullRequest() {
        assertFalse(validator.validate(null).valid());
    }
}
