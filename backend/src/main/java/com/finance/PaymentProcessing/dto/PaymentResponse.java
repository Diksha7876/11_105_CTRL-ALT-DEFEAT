package com.finance.PaymentProcessing.dto;

import com.finance.PaymentProcessing.model.CardType;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID paymentId, BigDecimal amount, String currency, String reference,
        PaymentStatus status, PaymentType paymentType, PaymentMethod paymentMethod, CardType cardType,
        UUID payerId, String invoiceId, UUID sourceAccountId, UUID beneficiaryId,
        String cardLast4, String cardHolderName, String upiId,
        Instant createdAt, Instant updatedAt) {
}
