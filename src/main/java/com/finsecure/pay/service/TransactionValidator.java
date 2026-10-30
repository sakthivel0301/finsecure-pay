package com.finsecure.pay.service;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Business rules for transaction validation.
 * R1 all fields present | R2 amount > 0 | R3 amount <= 100000
 * R4 supported currency | R5 sender != receiver | R6 account format ACC + 6-10 digits
 */
@Service
public class TransactionValidator {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");
    public static final Set<String> SUPPORTED_CURRENCIES = Set.of("INR", "USD", "EUR", "GBP");
    private static final String ACCOUNT_PATTERN = "^ACC\\d{6,10}$";

    public ValidationResult validate(TransactionRequest tx) {
        List<String> errors = new ArrayList<>();

        if (tx == null) {
            return ValidationResult.failed(List.of("Request body is required"));
        }
        if (isBlank(tx.transactionId())) errors.add("transactionId is required");
        if (isBlank(tx.fromAccount())) errors.add("fromAccount is required");
        if (isBlank(tx.toAccount())) errors.add("toAccount is required");
        if (tx.amount() == null) errors.add("amount is required");
        if (isBlank(tx.currency())) errors.add("currency is required");

        if (tx.amount() != null) {
            if (tx.amount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("amount must be greater than zero");
            } else if (tx.amount().compareTo(MAX_AMOUNT) > 0) {
                errors.add("amount exceeds maximum limit of " + MAX_AMOUNT);
            }
        }
        if (!isBlank(tx.currency()) && !SUPPORTED_CURRENCIES.contains(tx.currency().toUpperCase())) {
            errors.add("unsupported currency: " + tx.currency());
        }
        if (!isBlank(tx.fromAccount()) && !tx.fromAccount().matches(ACCOUNT_PATTERN)) {
            errors.add("fromAccount format invalid");
        }
        if (!isBlank(tx.toAccount()) && !tx.toAccount().matches(ACCOUNT_PATTERN)) {
            errors.add("toAccount format invalid");
        }
        if (!isBlank(tx.fromAccount()) && tx.fromAccount().equals(tx.toAccount())) {
            errors.add("fromAccount and toAccount must differ");
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.failed(errors);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
