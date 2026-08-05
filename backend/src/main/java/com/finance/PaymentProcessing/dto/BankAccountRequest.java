package com.finance.PaymentProcessing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountRequest(
	@NotBlank @Pattern(regexp = "[A-Za-z0-9]{6,34}") String accountNumber,
	@NotBlank String accountHolderName,
	UUID payerId,
	BigDecimal openingBalanceInr,
	String accountType) {
}