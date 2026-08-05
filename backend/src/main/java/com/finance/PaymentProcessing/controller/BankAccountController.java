package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.*;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.model.BankAccount;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {
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
        account = repository.save(account);
        return ResponseEntity.created(URI.create("/api/accounts/" + account.getAccountId())).body(toResponse(account));
    }

    @GetMapping
    public List<BankAccountResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    private BankAccountResponse toResponse(BankAccount a) {
        return new BankAccountResponse(a.getAccountId(), a.getAccountNumber(), a.getAccountHolderName(), a.isActive());
    }
}