package com.finance.PaymentProcessing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountResponse(
	UUID accountId,
	String accountNumber,
	String accountHolderName,
	UUID payerId,
	String accountType,
	BigDecimal balanceInInr,
	boolean active) {
}