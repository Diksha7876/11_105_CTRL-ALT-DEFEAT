package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.*;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.*;
import com.finance.PaymentProcessing.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService service;

    @InjectMocks
    private PaymentController controller;

    private UUID paymentId;
    private String idempotencyKey;
    private PaymentResponse sampleResponse;
    private PaymentRequest sampleRequest;

    @BeforeEach
    void setUp() {
        paymentId     = UUID.randomUUID();
        idempotencyKey = "idem-key-001";

        sampleResponse = new PaymentResponse(
                paymentId, new BigDecimal("200.00"), "INR", "REF-001",
                PaymentStatus.SENT, PaymentType.BENEFICIARY_TRANSFER,
                PaymentMethod.NET_BANKING, null,
                UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                null, null, null, null, null);

        sampleRequest = new PaymentRequest(
                new BigDecimal("200.00"), "INR", "REF-001", UUID.randomUUID(),
                PaymentMethod.NET_BANKING, UUID.randomUUID(), UUID.randomUUID(),
                null, null, null, null, null, null, null,
                PaymentType.BENEFICIARY_TRANSFER, null);
    }

    // =========================================================================
    // createPayment
    // =========================================================================

    @Test
    void createPayment_newPayment_returns201WithLocationHeader() {
        when(service.createPayment(sampleRequest, idempotencyKey))
                .thenReturn(new PaymentCreationResult(sampleResponse, true));

        ResponseEntity<PaymentResponse> response = controller.createPayment(idempotencyKey, sampleRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/payments/" + paymentId);
    }

    @Test
    void createPayment_newPayment_returnsPaymentInBody() {
        when(service.createPayment(sampleRequest, idempotencyKey))
                .thenReturn(new PaymentCreationResult(sampleResponse, true));

        ResponseEntity<PaymentResponse> response = controller.createPayment(idempotencyKey, sampleRequest);

        assertThat(response.getBody()).isEqualTo(sampleResponse);
        verify(service).createPayment(sampleRequest, idempotencyKey);
    }

    @Test
    void createPayment_duplicateIdempotencyKey_returns200WithExistingPayment() {
        when(service.createPayment(sampleRequest, idempotencyKey))
                .thenReturn(new PaymentCreationResult(sampleResponse, false));

        ResponseEntity<PaymentResponse> response = controller.createPayment(idempotencyKey, sampleRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(sampleResponse);
        assertThat(response.getHeaders().getLocation()).isNull();
    }

    @Test
    void createPayment_serviceThrows_propagatesException() {
        when(service.createPayment(sampleRequest, idempotencyKey))
                .thenThrow(new BadRequestException("INSUFFICIENT_FUNDS", "Insufficient balance"));

        assertThatThrownBy(() -> controller.createPayment(idempotencyKey, sampleRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient balance");
    }

    // =========================================================================
    // getPayment
    // =========================================================================

    @Test
    void getPayment_existingId_returnsPaymentResponse() {
        when(service.getPayment(paymentId)).thenReturn(sampleResponse);

        PaymentResponse result = controller.getPayment(paymentId);

        assertThat(result).isEqualTo(sampleResponse);
        verify(service).getPayment(paymentId);
    }

    @Test
    void getPayment_unknownId_propagatesNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(service.getPayment(unknownId))
                .thenThrow(new NotFoundException("Payment not found: " + unknownId));

        assertThatThrownBy(() -> controller.getPayment(unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // =========================================================================
    // updatePaymentStatus
    // =========================================================================

    @Test
    void updatePaymentStatus_validTransition_returnsUpdatedResponse() {
        PaymentStatusRequest statusRequest = new PaymentStatusRequest(
                PaymentStatus.COMPLETED, "done", null, "OPS");
        PaymentResponse updated = new PaymentResponse(
                paymentId, new BigDecimal("200.00"), "INR", "REF-001",
                PaymentStatus.COMPLETED, PaymentType.BENEFICIARY_TRANSFER,
                PaymentMethod.NET_BANKING, null,
                UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                null, null, null, null, null);
        when(service.updateStatus(paymentId, statusRequest)).thenReturn(updated);

        PaymentResponse result = controller.updatePaymentStatus(paymentId, statusRequest);

        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        verify(service).updateStatus(paymentId, statusRequest);
    }

    @Test
    void updatePaymentStatus_unknownId_propagatesNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        PaymentStatusRequest statusRequest = new PaymentStatusRequest(
                PaymentStatus.COMPLETED, null, null, null);
        when(service.updateStatus(unknownId, statusRequest))
                .thenThrow(new NotFoundException("Payment not found: " + unknownId));

        assertThatThrownBy(() -> controller.updatePaymentStatus(unknownId, statusRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    void updatePaymentStatus_invalidTransition_propagatesBadRequestException() {
        PaymentStatusRequest statusRequest = new PaymentStatusRequest(
                PaymentStatus.CREATED, null, null, null);
        when(service.updateStatus(paymentId, statusRequest))
                .thenThrow(new BadRequestException("INVALID_TRANSITION", "Invalid status transition"));

        assertThatThrownBy(() -> controller.updatePaymentStatus(paymentId, statusRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }

    // =========================================================================
    // listPayments
    // =========================================================================

    @Test
    void listPayments_withStatus_delegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<PaymentResponse> page = new PageImpl<>(List.of(sampleResponse));
        when(service.listPayments(PaymentStatus.SENT, pageable)).thenReturn(page);

        Page<PaymentResponse> result = controller.listPayments(PaymentStatus.SENT, pageable);

        assertThat(result.getContent()).containsExactly(sampleResponse);
        verify(service).listPayments(PaymentStatus.SENT, pageable);
    }

    @Test
    void listPayments_nullStatus_delegatesNullToService() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<PaymentResponse> emptyPage = Page.empty(pageable);
        when(service.listPayments(null, pageable)).thenReturn(emptyPage);

        Page<PaymentResponse> result = controller.listPayments(null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(service).listPayments(null, pageable);
    }

    @Test
    void listPayments_multipleStatuses_eachDelegatesCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        for (PaymentStatus status : PaymentStatus.values()) {
            Page<PaymentResponse> page = new PageImpl<>(List.of());
            when(service.listPayments(status, pageable)).thenReturn(page);

            controller.listPayments(status, pageable);

            verify(service).listPayments(status, pageable);
        }
    }
}
