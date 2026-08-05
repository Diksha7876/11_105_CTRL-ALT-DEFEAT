package com.finance.PaymentProcessing.repository;

import com.finance.PaymentProcessing.model.Beneficiary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeneficiaryRepository {
    Beneficiary save(Beneficiary beneficiary);
    Optional<Beneficiary> findById(UUID id);
    List<Beneficiary> findAll();
    boolean existsById(UUID id);
    Optional<Beneficiary> findByAccountNumber(String accountNumber);
}