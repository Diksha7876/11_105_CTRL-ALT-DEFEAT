package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.dto.PaymentHistoryResponse;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentHistory;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.repository.PaymentHistoryRepository;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private PaymentHistoryRepository historyRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private HistoryService service;

    private String paymentId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();

        payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setStatus(PaymentStatus.CREATED);
    }

    // -------------------------------------------------------------------------
    // recordTransition – happy path
    // -------------------------------------------------------------------------

    @Test
    void recordTransition_savesHistoryWithCorrectFields() {
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);

        service.recordTransition(payment, PaymentStatus.CREATED, PaymentStatus.VALIDATED,
                "looks good", null, "OPS_TEAM");

        verify(historyRepository).save(captor.capture());
        PaymentHistory saved = captor.getValue();

        assertThat(saved.getPaymentId()).isEqualTo(paymentId);
        assertThat(saved.getOldStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(saved.getNewStatus()).isEqualTo(PaymentStatus.VALIDATED);
        assertThat(saved.getRemarks()).isEqualTo("looks good");
        assertThat(saved.getErrorCode()).isNull();
        assertThat(saved.getActor()).isEqualTo("OPS_TEAM");
    }

    @Test
    void recordTransition_withErrorCode_savesErrorCode() {
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);

        service.recordTransition(payment, PaymentStatus.VALIDATED, PaymentStatus.FAILED,
                "fraud detected", "FRAUD_CHECK_FAILED", "RISK_ENGINE");

        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getErrorCode()).isEqualTo("FRAUD_CHECK_FAILED");
        assertThat(captor.getValue().getNewStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void recordTransition_nullRemarksAndErrorCode_savedAsNull() {
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);

        service.recordTransition(payment, PaymentStatus.CREATED, PaymentStatus.VALIDATED,
                null, null, "SYSTEM");

        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getRemarks()).isNull();
        assertThat(captor.getValue().getErrorCode()).isNull();
    }

    // -------------------------------------------------------------------------
    // recordTransition – actor defaulting
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "actor=''{0}'' defaults to API_CLIENT")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void recordTransition_nullOrBlankActor_defaultsToApiClient(String actor) {
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);

        service.recordTransition(payment, PaymentStatus.CREATED, PaymentStatus.VALIDATED,
                null, null, actor);

        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("API_CLIENT");
    }

    @Test
    void recordTransition_nonBlankActor_savedAsIs() {
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);

        service.recordTransition(payment, PaymentStatus.CREATED, PaymentStatus.VALIDATED,
                null, null, "PAYMENT_OPERATIONS");

        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("PAYMENT_OPERATIONS");
    }

    // -------------------------------------------------------------------------
    // getHistory – happy path
    // -------------------------------------------------------------------------

    @Test
    void getHistory_existingPayment_returnsMappedResponsesInOrder() {
        String histId1 = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        String histId2 = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        Instant t1 = Instant.parse("2024-01-01T10:00:00Z");
        Instant t2 = Instant.parse("2024-01-01T11:00:00Z");

        PaymentHistory h1 = buildHistory(histId1, paymentId, PaymentStatus.CREATED,
                PaymentStatus.VALIDATED, t1, "ok", null, "SYS");
        PaymentHistory h2 = buildHistory(histId2, paymentId, PaymentStatus.VALIDATED,
                PaymentStatus.SENT, t2, null, null, "OPS");

        when(paymentRepository.existsById(paymentId)).thenReturn(true);
        when(historyRepository.findByPaymentIdOrderByTimestampAsc(paymentId))
                .thenReturn(List.of(h1, h2));

        List<PaymentHistoryResponse> result = service.getHistory(paymentId);

        assertThat(result).hasSize(2);

        PaymentHistoryResponse r1 = result.get(0);
        assertThat(r1.historyId()).isEqualTo(histId1);
        assertThat(r1.oldStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(r1.newStatus()).isEqualTo(PaymentStatus.VALIDATED);
        assertThat(r1.timestamp()).isEqualTo(t1);
        assertThat(r1.remarks()).isEqualTo("ok");
        assertThat(r1.errorCode()).isNull();
        assertThat(r1.actor()).isEqualTo("SYS");

        PaymentHistoryResponse r2 = result.get(1);
        assertThat(r2.historyId()).isEqualTo(histId2);
        assertThat(r2.oldStatus()).isEqualTo(PaymentStatus.VALIDATED);
        assertThat(r2.newStatus()).isEqualTo(PaymentStatus.SENT);
        assertThat(r2.timestamp()).isEqualTo(t2);
        assertThat(r2.actor()).isEqualTo("OPS");
    }

    @Test
    void getHistory_existingPaymentWithNoEntries_returnsEmptyList() {
        when(paymentRepository.existsById(paymentId)).thenReturn(true);
        when(historyRepository.findByPaymentIdOrderByTimestampAsc(paymentId))
                .thenReturn(List.of());

        List<PaymentHistoryResponse> result = service.getHistory(paymentId);

        assertThat(result).isEmpty();
    }

    @Test
    void getHistory_failedTransition_mapsErrorCodeAndRemarks() {
        String histId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        PaymentHistory h = buildHistory(histId, paymentId, PaymentStatus.VALIDATED,
                PaymentStatus.FAILED, Instant.now(), "rejected by bank", "BANK_REJECT_001", "BANK_GW");

        when(paymentRepository.existsById(paymentId)).thenReturn(true);
        when(historyRepository.findByPaymentIdOrderByTimestampAsc(paymentId))
                .thenReturn(List.of(h));

        List<PaymentHistoryResponse> result = service.getHistory(paymentId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).errorCode()).isEqualTo("BANK_REJECT_001");
        assertThat(result.get(0).remarks()).isEqualTo("rejected by bank");
        assertThat(result.get(0).newStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    // -------------------------------------------------------------------------
    // getHistory – error case
    // -------------------------------------------------------------------------

    @Test
    void getHistory_unknownPaymentId_throwsNotFoundException() {
        String unknownId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        when(paymentRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory(unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        verify(historyRepository, never()).findByPaymentIdOrderByTimestampAsc(any());
    }

    // -------------------------------------------------------------------------
    // helper
    // -------------------------------------------------------------------------

    private PaymentHistory buildHistory(String histId, String pmtId, PaymentStatus oldStatus,
            PaymentStatus newStatus, Instant timestamp, String remarks,
            String errorCode, String actor) {
        PaymentHistory h = new PaymentHistory();
        h.setHistoryId(histId);
        h.setPaymentId(pmtId);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus);
        h.setTimestamp(timestamp);
        h.setRemarks(remarks);
        h.setErrorCode(errorCode);
        h.setActor(actor);
        return h;
    }
}
