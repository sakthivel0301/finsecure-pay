package com.finsecure.pay.model;

import java.math.BigDecimal;

public record TransactionRequest(
        String transactionId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String currency) {
}
