package com.finance.PaymentProcessing.dto;

import com.finance.PaymentProcessing.model.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusRequest(@NotNull PaymentStatus status, String remarks, String errorCode, String actor) {
}
