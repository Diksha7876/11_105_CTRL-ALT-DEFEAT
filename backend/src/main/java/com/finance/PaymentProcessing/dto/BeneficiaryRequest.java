package com.finance.PaymentProcessing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BeneficiaryRequest(
                @NotBlank String name,
                @NotBlank @Pattern(regexp = "[A-Za-z0-9]{6,34}", message = "account number must be 6-34 alphanumeric characters") String accountNumber,
                @NotBlank String bankName,
                @NotBlank @Pattern(regexp = "[A-Za-z]{4}0[A-Za-z0-9]{6}", message = "IFSC code is invalid") String ifscCode,
                @NotBlank @Email String email,
                String phone) {
}