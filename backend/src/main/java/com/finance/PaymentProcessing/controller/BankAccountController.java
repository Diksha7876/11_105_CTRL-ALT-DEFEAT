package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.*;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.model.BankAccount;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {
    private static final String DEFAULT_PAYER_ID = "111111111";
    private static final Set<String> SUPPORTED_ACCOUNT_TYPES = Set.of("SAVINGS", "CURRENT", "SALARY");
    private static final BigDecimal DEFAULT_MAX_TXN_LIMIT_INR = new BigDecimal("1000000.00");

    private final BankAccountRepository repository;

    public BankAccountController(BankAccountRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<BankAccountResponse> create(@Valid @RequestBody BankAccountRequest request) {
        if (repository.findAll().stream().anyMatch(a -> a.getAccountNumber().equals(request.accountNumber())))
            throw new BadRequestException("INVALID_ACCOUNT", "Account number already exists");
        BankAccount account = new BankAccount();
        account.setAccountNumber(request.accountNumber());
        account.setAccountHolderName(request.accountHolderName());
        account.setPayerId(request.payerId() != null ? request.payerId() : DEFAULT_PAYER_ID);
        BigDecimal openingBalance = request.openingBalanceInr() != null ? request.openingBalanceInr() : new BigDecimal("50000.00");
        if (openingBalance.signum() < 0) {
            throw new BadRequestException("INVALID_BALANCE", "Opening balance cannot be negative");
        }
        account.setBalanceInInr(openingBalance);
        BigDecimal maxTransactionLimit = request.maxTransactionLimitInInr() != null
            ? request.maxTransactionLimitInInr()
            : DEFAULT_MAX_TXN_LIMIT_INR;
        if (maxTransactionLimit.signum() <= 0) {
            throw new BadRequestException("INVALID_LIMIT", "Max transaction limit must be greater than 0");
        }
        account.setMaxTransactionLimitInInr(maxTransactionLimit);
        String accountType = request.accountType() == null ? "SAVINGS" : request.accountType().trim().toUpperCase();
        if (!SUPPORTED_ACCOUNT_TYPES.contains(accountType)) {
            throw new BadRequestException("INVALID_ACCOUNT_TYPE", "Supported account types: SAVINGS, CURRENT, SALARY");
        }
        account.setAccountType(accountType);
        account = repository.save(account);
        return ResponseEntity.created(URI.create("/api/accounts/" + account.getAccountId())).body(toResponse(account));
    }

    @PatchMapping("/{accountId}/limit")
    public ResponseEntity<BankAccountResponse> updateTransactionLimit(
            @PathVariable String accountId,
            @Valid @RequestBody BankAccountLimitUpdateRequest request) {
        BankAccount account = repository.findById(accountId)
            .orElseThrow(() -> new BadRequestException("INVALID_ACCOUNT", "Source account not found: " + accountId));
        if (request.maxTransactionLimitInInr().signum() <= 0) {
            throw new BadRequestException("INVALID_LIMIT", "Max transaction limit must be greater than 0");
        }
        account.setMaxTransactionLimitInInr(request.maxTransactionLimitInInr());
        account = repository.save(account);
        return ResponseEntity.ok(toResponse(account));
    }

    @GetMapping
    public List<BankAccountResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private BankAccountResponse toResponse(BankAccount a) {
        return new BankAccountResponse(
            a.getAccountId(),
            a.getAccountNumber(),
            a.getAccountHolderName(),
            a.getPayerId(),
            a.getAccountType(),
            a.getBalanceInInr(),
            a.getMaxTransactionLimitInInr(),
            a.isActive());
    }
}