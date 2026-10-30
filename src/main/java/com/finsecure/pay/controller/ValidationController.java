package com.finsecure.pay.controller;

import com.finsecure.pay.model.TransactionRequest;
import com.finsecure.pay.model.ValidationResult;
import com.finsecure.pay.service.TransactionValidator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ValidationController {

    private static final Logger log = LoggerFactory.getLogger(ValidationController.class);

    private final TransactionValidator validator;
    private final Counter validCounter;
    private final Counter invalidCounter;
    private final String appVersion;

    public ValidationController(TransactionValidator validator, MeterRegistry registry,
                                @Value("${app.version:1.0.0}") String appVersion) {
        this.validator = validator;
        this.appVersion = appVersion;
        this.validCounter = Counter.builder("finsecure_transactions_total")
                .tag("result", "valid").register(registry);
        this.invalidCounter = Counter.builder("finsecure_transactions_total")
                .tag("result", "invalid").register(registry);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "finsecure-pay", "version", appVersion);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validate(@RequestBody(required = false) TransactionRequest request) {
        ValidationResult result = validator.validate(request);
        if (result.valid()) {
            validCounter.increment();
            log.info("Transaction {} validated OK", request.transactionId());
            return ResponseEntity.ok(result);
        }
        invalidCounter.increment();
        log.warn("Transaction validation failed: {}", result.errors());
        return ResponseEntity.badRequest().body(result);
    }

    /** Used only to deliberately trigger the alert in Task 6. */
    @GetMapping("/simulate-error")
    public ResponseEntity<Map<String, String>> simulateError() {
        log.error("Simulated error for alert testing");
        return ResponseEntity.status(500).body(Map.of("error", "simulated failure"));
    }
}
