package com.finance.PaymentProcessing.dto;

import java.math.BigDecimal;

public record BankAccountResponse(
	String accountId,
	String accountNumber,
	String accountHolderName,
	String payerId,
	String accountType,
	BigDecimal balanceInInr,
	BigDecimal maxTransactionLimitInInr,
	boolean active) {
}