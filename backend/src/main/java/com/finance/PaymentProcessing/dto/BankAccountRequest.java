package com.finance.PaymentProcessing.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record BankAccountRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9]{6,34}") String accountNumber, @NotBlank String accountHolderName) { }