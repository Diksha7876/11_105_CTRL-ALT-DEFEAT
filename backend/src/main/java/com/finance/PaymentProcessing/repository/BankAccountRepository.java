package com.finance.PaymentProcessing.repository;

import com.finance.PaymentProcessing.model.BankAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository {
    BankAccount save(BankAccount account);
    Optional<BankAccount> findById(UUID id);
    List<BankAccount> findAll();
    boolean existsById(UUID id);
}
