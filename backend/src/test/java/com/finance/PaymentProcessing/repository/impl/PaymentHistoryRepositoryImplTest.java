package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.model.PaymentHistory;
import com.finance.PaymentProcessing.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryRepositoryImplTest {

    @Mock private JdbcTemplate jdbc;
    @InjectMocks private PaymentHistoryRepositoryImpl repository;

    private UUID paymentId;
    private PaymentHistory history;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        history = new PaymentHistory();
        history.setPaymentId(paymentId);
        history.setOldStatus(PaymentStatus.CREATED);
        history.setNewStatus(PaymentStatus.VALIDATED);
        history.setRemarks("ok");
        history.setErrorCode(null);
        history.setActor("SYS");
    }

    // -------------------------------------------------------------------------
    // save
    // -------------------------------------------------------------------------

    @Test
    void save_assignsHistoryIdBeforeInsert() {
        repository.save(history);

        assertThat(history.getHistoryId()).isNotNull();
    }

    @Test
    void save_assignsTimestampBeforeInsert() {
        Instant before = Instant.now().minusSeconds(1);

        repository.save(history);

        assertThat(history.getTimestamp()).isAfter(before);
    }

    @Test
    void save_executesInsertSql() {
        repository.save(history);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(),
                any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).containsIgnoringCase("INSERT INTO payment_history");
    }

    @Test
    void save_nullOldStatus_passedAsNullToJdbc() {
        history.setOldStatus(null);

        repository.save(history);

        // 3rd argument is old_status: historyId, paymentId, oldStatus, newStatus, ts, remarks, errorCode, actor
        ArgumentCaptor<Object> oldStatusArg = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), any(), any(), oldStatusArg.capture(),
                any(), any(), any(), any(), any());
        assertThat(oldStatusArg.getValue()).isNull();
    }

    @Test
    void save_newStatusNamePassedToJdbc() {
        history.setNewStatus(PaymentStatus.FAILED);

        repository.save(history);

        // 4th argument is new_status
        ArgumentCaptor<Object> newStatusArg = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), any(), any(), any(), newStatusArg.capture(),
                any(), any(), any(), any());
        assertThat(newStatusArg.getValue()).isEqualTo("FAILED");
    }

    @Test
    void save_errorCodeAndRemarksPassedThrough() {
        history.setRemarks("fraud check");
        history.setErrorCode("FRAUD_001");

        repository.save(history);

        // args: historyId, paymentId, oldStatus, newStatus, timestamp, remarks, errorCode, actor
        ArgumentCaptor<Object> remarksArg = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> errorCodeArg = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(),
                remarksArg.capture(), errorCodeArg.capture(), any());
        assertThat(remarksArg.getValue()).isEqualTo("fraud check");
        assertThat(errorCodeArg.getValue()).isEqualTo("FRAUD_001");
    }

    // -------------------------------------------------------------------------
    // findByPaymentIdOrderByTimestampAsc
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void findByPaymentIdOrderByTimestampAsc_returnsHistoryList() {
        PaymentHistory h2 = new PaymentHistory();
        h2.setPaymentId(paymentId);
        h2.setNewStatus(PaymentStatus.SENT);
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(history, h2));

        List<PaymentHistory> result = repository.findByPaymentIdOrderByTimestampAsc(paymentId);

        assertThat(result).hasSize(2).containsExactly(history, h2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByPaymentIdOrderByTimestampAsc_emptyResult_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        assertThat(repository.findByPaymentIdOrderByTimestampAsc(paymentId)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByPaymentIdOrderByTimestampAsc_passesPaymentIdAsString() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        repository.findByPaymentIdOrderByTimestampAsc(paymentId);

        verify(jdbc).query(anyString(), any(RowMapper.class), eq(paymentId.toString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByPaymentIdOrderByTimestampAsc_sqlContainsOrderByTimestamp() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        repository.findByPaymentIdOrderByTimestampAsc(paymentId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlCaptor.capture(), any(RowMapper.class), anyString());
        assertThat(sqlCaptor.getValue()).containsIgnoringCase("ORDER BY timestamp ASC");
    }
}
