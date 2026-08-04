package com.finance.PaymentProcessing.dto;

import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID paymentId, BigDecimal amount, String currency, String reference,
        PaymentStatus status, PaymentType paymentType, UUID payerId, String invoiceId, UUID sourceAccountId, UUID beneficiaryId,
        Instant createdAt, Instant updatedAt) {
}
