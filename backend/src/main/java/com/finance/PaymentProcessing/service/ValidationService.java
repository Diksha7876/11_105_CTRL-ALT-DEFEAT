package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.model.PaymentType;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("INR", "USD", "EUR", "GBP");
    private final BeneficiaryRepository beneficiaryRepository;
    private final BankAccountRepository accountRepository;

    public ValidationService(BeneficiaryRepository beneficiaryRepository, BankAccountRepository accountRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
    }

    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0)
            throw new BadRequestException("Amount must be greater than zero");
    }

    public void validateCurrency(String currency) {
        if (currency == null || !SUPPORTED_CURRENCIES.contains(currency.toUpperCase()))
            throw new BadRequestException("Unsupported currency. Use INR, USD, EUR, or GBP");
    }

    public void validateBeneficiary(UUID beneficiaryId) {
        if (!beneficiaryRepository.existsById(beneficiaryId))
            throw new NotFoundException("Beneficiary not found: " + beneficiaryId);
    }

    public void validateSourceAccount(UUID sourceAccountId, UUID beneficiaryId) {
        var account = accountRepository.findById(sourceAccountId)
            .orElseThrow(() -> new NotFoundException("INVALID_ACCOUNT", "Source account not found: " + sourceAccountId));
        var beneficiary = beneficiaryRepository.findById(beneficiaryId)
            .orElseThrow(() -> new NotFoundException("INVALID_ACCOUNT", "Beneficiary not found: " + beneficiaryId));
        if (!account.isActive() || account.getAccountNumber().equals(beneficiary.getAccountNumber()))
            throw new BadRequestException("INVALID_ACCOUNT", "Source and destination accounts must be different active accounts");
    }

    public void validatePaymentDetails(PaymentType paymentType, String invoiceId) {
        if (paymentType == PaymentType.BILL_PAYMENT && (invoiceId == null || invoiceId.isBlank())) {
            throw new BadRequestException("invoiceId is required for a bill payment");
        }
        if (paymentType == PaymentType.BENEFICIARY_TRANSFER && invoiceId != null && !invoiceId.isBlank()) {
            throw new BadRequestException("invoiceId must not be supplied for a beneficiary transfer");
        }
    }

    public void validateStatusTransition(PaymentStatus oldStatus, PaymentStatus newStatus) {
        boolean valid = switch (oldStatus) {
            case CREATED   -> newStatus == PaymentStatus.VALIDATED || newStatus == PaymentStatus.FAILED;
            case VALIDATED -> newStatus == PaymentStatus.SENT      || newStatus == PaymentStatus.FAILED;
            case SENT      -> newStatus == PaymentStatus.COMPLETED || newStatus == PaymentStatus.FAILED;
            default        -> false;
        };
        if (!valid)
            throw new BadRequestException("Invalid status transition from " + oldStatus + " to " + newStatus);
    }
}
