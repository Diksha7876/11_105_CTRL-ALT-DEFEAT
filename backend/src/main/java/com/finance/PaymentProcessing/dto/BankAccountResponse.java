package com.finance.PaymentProcessing.dto;
import java.util.UUID;
public record BankAccountResponse(UUID accountId, String accountNumber, String accountHolderName, boolean active) { }