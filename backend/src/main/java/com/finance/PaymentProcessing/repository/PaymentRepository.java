package com.finance.PaymentProcessing.repository;

import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    boolean existsById(UUID id);
    Optional<Payment> findByIdempotencyKey(String key);
    Optional<Payment> findByPayerIdAndInvoiceId(UUID payerId, String invoiceId);
    Page<Payment> findAll(Pageable pageable);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
