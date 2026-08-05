package com.finance.PaymentProcessing.repository;

import com.finance.PaymentProcessing.model.PaymentHistory;
import java.util.List;
import java.util.UUID;

public interface PaymentHistoryRepository {
    void save(PaymentHistory history);
    List<PaymentHistory> findByPaymentIdOrderByTimestampAsc(UUID paymentId);
}
