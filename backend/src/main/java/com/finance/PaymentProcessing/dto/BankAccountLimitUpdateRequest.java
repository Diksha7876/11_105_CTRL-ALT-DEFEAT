package com.finance.PaymentProcessing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BankAccountLimitUpdateRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 7, fraction = 2) BigDecimal maxTransactionLimitInInr) {
}
