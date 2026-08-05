package com.finance.PaymentProcessing.dto;

import com.finance.PaymentProcessing.model.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentHistoryResponse(UUID historyId, PaymentStatus oldStatus, PaymentStatus newStatus,
        Instant timestamp, String remarks, String errorCode, String actor) {
}
