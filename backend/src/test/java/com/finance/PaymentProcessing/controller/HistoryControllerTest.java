package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.PaymentHistoryResponse;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.service.HistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryControllerTest {

    @Mock
    private HistoryService service;

    @InjectMocks
    private HistoryController controller;

    private UUID paymentId;
    private List<PaymentHistoryResponse> sampleHistory;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        sampleHistory = List.of(
                new PaymentHistoryResponse(UUID.randomUUID(), PaymentStatus.CREATED,
                        PaymentStatus.VALIDATED, Instant.now(), "validated ok", null, "SYS"),
                new PaymentHistoryResponse(UUID.randomUUID(), PaymentStatus.VALIDATED,
                        PaymentStatus.SENT, Instant.now(), null, null, "OPS"));
    }

    // =========================================================================
    // getPaymentHistory
    // =========================================================================

    @Test
    void getPaymentHistory_delegatesToService_returnsHistory() {
        when(service.getHistory(paymentId)).thenReturn(sampleHistory);

        List<PaymentHistoryResponse> result = controller.getPaymentHistory(paymentId);

        assertThat(result).isEqualTo(sampleHistory);
        verify(service).getHistory(paymentId);
    }

    @Test
    void getPaymentHistory_emptyHistory_returnsEmptyList() {
        when(service.getHistory(paymentId)).thenReturn(List.of());

        List<PaymentHistoryResponse> result = controller.getPaymentHistory(paymentId);

        assertThat(result).isEmpty();
    }

    @Test
    void getPaymentHistory_unknownPaymentId_propagatesNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(service.getHistory(unknownId))
                .thenThrow(new NotFoundException("Payment not found: " + unknownId));

        assertThatThrownBy(() -> controller.getPaymentHistory(unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // =========================================================================
    // getTransactionTimeline
    // =========================================================================

    @Test
    void getTransactionTimeline_delegatesToService_returnsHistory() {
        when(service.getHistory(paymentId)).thenReturn(sampleHistory);

        List<PaymentHistoryResponse> result = controller.getTransactionTimeline(paymentId);

        assertThat(result).isEqualTo(sampleHistory);
        verify(service).getHistory(paymentId);
    }

    @Test
    void getTransactionTimeline_emptyHistory_returnsEmptyList() {
        when(service.getHistory(paymentId)).thenReturn(List.of());

        List<PaymentHistoryResponse> result = controller.getTransactionTimeline(paymentId);

        assertThat(result).isEmpty();
    }

    @Test
    void getTransactionTimeline_unknownPaymentId_propagatesNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(service.getHistory(unknownId))
                .thenThrow(new NotFoundException("Payment not found: " + unknownId));

        assertThatThrownBy(() -> controller.getTransactionTimeline(unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // =========================================================================
    // Both endpoints call the same service method
    // =========================================================================

    @Test
    void bothEndpoints_callSameServiceMethod_forSamePaymentId() {
        when(service.getHistory(paymentId)).thenReturn(sampleHistory);

        List<PaymentHistoryResponse> fromHistory  = controller.getPaymentHistory(paymentId);
        List<PaymentHistoryResponse> fromTimeline = controller.getTransactionTimeline(paymentId);

        assertThat(fromHistory).isEqualTo(fromTimeline);
        verify(service, times(2)).getHistory(paymentId);
    }
}
