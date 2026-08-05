package com.finance.PaymentProcessing.dto;

import java.util.UUID;

public record BeneficiaryResponse(UUID beneficiaryId, String name, String accountNumber, String bankName,
        String ifscCode, String email, String phone) {
}