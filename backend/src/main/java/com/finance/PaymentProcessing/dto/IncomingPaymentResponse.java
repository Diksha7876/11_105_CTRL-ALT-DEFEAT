package com.finance.PaymentProcessing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IncomingPaymentResponse(
    UUID incomingPaymentId,
    UUID payerId,
    BigDecimal amount,
    String currency,
    String reference,
    String sourceName,
    UUID destinationAccountId,
    Instant receivedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
