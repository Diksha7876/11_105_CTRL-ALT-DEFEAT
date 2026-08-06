package com.finance.PaymentProcessing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record BankAccountRequest(
	@NotBlank @Pattern(regexp = "[A-Za-z0-9]{6,34}") String accountNumber,
	@NotBlank String accountHolderName,
	String payerId,
	BigDecimal openingBalanceInr,
	BigDecimal maxTransactionLimitInInr,
	String accountType) {
}