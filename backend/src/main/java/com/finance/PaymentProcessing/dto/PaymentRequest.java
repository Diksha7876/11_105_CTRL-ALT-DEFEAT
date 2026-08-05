package com.finance.PaymentProcessing.dto;

import com.finance.PaymentProcessing.model.CardType;
import com.finance.PaymentProcessing.model.PaymentMethod;
import com.finance.PaymentProcessing.model.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
                @NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "1000000.00") @Digits(integer = 7, fraction = 2) BigDecimal amount,
                @NotBlank String currency,
                String reference,
                UUID payerId,
                @NotNull PaymentMethod paymentMethod,
                UUID beneficiaryId,
                CardType cardType,
                String cardHolderName,
                String cardNumber,
                String expiryMonth,
                String expiryYear,
                String cvv,
                PaymentType paymentType,
                String invoiceId) {
}
