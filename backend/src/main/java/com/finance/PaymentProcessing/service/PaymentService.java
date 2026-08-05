package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.dto.*;
import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.*;
import com.finance.PaymentProcessing.repository.*;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ValidationService validationService;
    private final HistoryService historyService;

    public PaymentService(PaymentRepository paymentRepository,
            ValidationService validationService, HistoryService historyService) {
        this.paymentRepository = paymentRepository;
        this.validationService = validationService;
        this.historyService = historyService;
    }

    public PaymentCreationResult createPayment(PaymentRequest request, String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(p -> new PaymentCreationResult(toResponse(p), false))
                .orElseGet(() -> {
                    validationService.validateAmount(request.amount());
                    validationService.validateCurrency(request.currency());
                    validationService.validateBeneficiary(request.beneficiaryId());
                    validationService.validateSourceAccount(request.sourceAccountId(), request.beneficiaryId());
                    validationService.validatePaymentDetails(request.paymentType(), request.invoiceId());
                    String invoiceId = request.paymentType() == PaymentType.BILL_PAYMENT ? request.invoiceId().trim()
                            : null;
                    if (invoiceId != null
                            && paymentRepository.findByPayerIdAndInvoiceId(request.payerId(), invoiceId).isPresent()) {
                        throw new ConflictException("DUPLICATE_PAYMENT",
                                "This invoice has already been paid by this payer");
                    }
                    Payment payment = new Payment();
                    payment.setAmount(request.amount());
                    payment.setCurrency(request.currency().toUpperCase());
                    payment.setReference(request.reference());
                    payment.setSourceAccountId(request.sourceAccountId());
                    payment.setBeneficiaryId(request.beneficiaryId());
                    payment.setPayerId(request.payerId());
                    payment.setPaymentType(request.paymentType());
                    payment.setInvoiceId(invoiceId);
                    payment.setIdempotencyKey(idempotencyKey);
                    payment.setStatus(PaymentStatus.CREATED);
                    Payment saved = paymentRepository.save(payment);
                    historyService.recordTransition(saved, null, PaymentStatus.CREATED, "Payment created", null,
                            "API_CLIENT");
                    return new PaymentCreationResult(toResponse(saved), true);
                });
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        return toResponse(find(id));
    }

    public PaymentResponse updateStatus(UUID id, PaymentStatusRequest request) {
        Payment payment = find(id);
        validationService.validateStatusTransition(payment.getStatus(), request.status());
        PaymentStatus old = payment.getStatus();
        payment.setStatus(request.status());
        Payment saved = paymentRepository.save(payment);
        historyService.recordTransition(saved, old, request.status(), request.remarks(), request.errorCode(),
                request.actor());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(PaymentStatus status, Pageable pageable) {
        return (status == null ? paymentRepository.findAll(pageable) : paymentRepository.findByStatus(status, pageable))
                .map(this::toResponse);
    }

    private Payment find(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() -> new NotFoundException("Payment not found: " + id));
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getPaymentId(), p.getAmount(), p.getCurrency(), p.getReference(), p.getStatus(),
                p.getPaymentType(), p.getPayerId(), p.getInvoiceId(),
                p.getSourceAccountId(), p.getBeneficiaryId(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
