package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryImplTest {

    @Mock private JdbcTemplate jdbc;
    @InjectMocks private PaymentRepositoryImpl repository;

    private UUID paymentId;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setCurrency("INR");
        payment.setReference("REF-001");
        payment.setStatus(PaymentStatus.SENT);
        payment.setVersion(0L);
        payment.setPaymentType(PaymentType.BENEFICIARY_TRANSFER);
        payment.setPaymentMethod(PaymentMethod.NET_BANKING);
        payment.setPayerId(UUID.randomUUID());
        payment.setIdempotencyKey("idem-001");
    }

    // =========================================================================
    // save – INSERT (paymentId is null)
    // =========================================================================

    @Test
    void save_newPayment_assignsUuidAndTimestampsAndVersion() {
        Payment newPayment = new Payment();
        newPayment.setAmount(new BigDecimal("100"));
        newPayment.setCurrency("INR");
        newPayment.setReference("REF");
        newPayment.setStatus(PaymentStatus.SENT);
        newPayment.setPaymentType(PaymentType.BENEFICIARY_TRANSFER);
        newPayment.setPaymentMethod(PaymentMethod.NET_BANKING);
        newPayment.setPayerId(UUID.randomUUID());
        newPayment.setIdempotencyKey("key-new");

        repository.save(newPayment);

        assertThat(newPayment.getPaymentId()).isNotNull();
        assertThat(newPayment.getCreatedAt()).isNotNull();
        assertThat(newPayment.getUpdatedAt()).isNotNull();
        assertThat(newPayment.getVersion()).isEqualTo(0L);
    }

    @Test
    void save_newPayment_executesInsertSql() {
        Payment newPayment = new Payment();
        newPayment.setAmount(new BigDecimal("100"));
        newPayment.setCurrency("INR");
        newPayment.setReference("REF");
        newPayment.setStatus(PaymentStatus.SENT);
        newPayment.setPaymentType(PaymentType.BENEFICIARY_TRANSFER);
        newPayment.setPaymentMethod(PaymentMethod.NET_BANKING);
        newPayment.setPayerId(UUID.randomUUID());
        newPayment.setIdempotencyKey("key-new");

        repository.save(newPayment);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        // INSERT has 19 args
        verify(jdbc).update(sqlCaptor.capture(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).containsIgnoringCase("INSERT INTO payments");
    }

    @Test
    void save_newPayment_returnsSameReference() {
        Payment newPayment = new Payment();
        newPayment.setAmount(new BigDecimal("100"));
        newPayment.setCurrency("INR");
        newPayment.setReference("REF");
        newPayment.setStatus(PaymentStatus.SENT);
        newPayment.setPaymentType(PaymentType.BENEFICIARY_TRANSFER);
        newPayment.setPaymentMethod(PaymentMethod.NET_BANKING);
        newPayment.setPayerId(UUID.randomUUID());
        newPayment.setIdempotencyKey("key-new");

        assertThat(repository.save(newPayment)).isSameAs(newPayment);
    }

    // =========================================================================
    // save – UPDATE (paymentId is set)
    // =========================================================================

    @Test
    void save_existingPayment_updateSucceeds_incrementsVersion() {
        when(jdbc.update(anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        repository.save(payment);

        assertThat(payment.getVersion()).isEqualTo(1L);
    }

    @Test
    void save_existingPayment_updateSucceeds_refreshesUpdatedAt() {
        Instant before = Instant.now().minusSeconds(1);
        when(jdbc.update(anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        repository.save(payment);

        assertThat(payment.getUpdatedAt()).isAfter(before);
    }

    @Test
    void save_existingPayment_executesUpdateSql() {
        when(jdbc.update(anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        repository.save(payment);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).containsIgnoringCase("UPDATE payments");
    }

    @Test
    void save_existingPayment_updateReturnsZero_throwsConflictException() {
        when(jdbc.update(anyString(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(0);

        assertThatThrownBy(() -> repository.save(payment))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getErrorCode())
                        .isEqualTo("OPTIMISTIC_LOCK"));
    }

    // =========================================================================
    // findById
    // =========================================================================

    @SuppressWarnings("unchecked")
    @Test
    void findById_found_returnsOptional() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(payment));

        assertThat(repository.findById(paymentId)).isPresent().contains(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findById_notFound_returnsEmpty() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findById_passesIdAsString() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(payment));

        repository.findById(paymentId);

        verify(jdbc).query(anyString(), any(RowMapper.class), eq(paymentId.toString()));
    }

    // =========================================================================
    // existsById
    // =========================================================================

    @Test
    void existsById_countOne_returnsTrue() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        assertThat(repository.existsById(paymentId)).isTrue();
    }

    @Test
    void existsById_countZero_returnsFalse() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        assertThat(repository.existsById(paymentId)).isFalse();
    }

    @Test
    void existsById_nullCount_returnsFalse() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(null);
        assertThat(repository.existsById(paymentId)).isFalse();
    }

    // =========================================================================
    // findByIdempotencyKey
    // =========================================================================

    @SuppressWarnings("unchecked")
    @Test
    void findByIdempotencyKey_found_returnsOptional() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("idem-001")))
                .thenReturn(List.of(payment));

        assertThat(repository.findByIdempotencyKey("idem-001")).isPresent().contains(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByIdempotencyKey_notFound_returnsEmpty() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        assertThat(repository.findByIdempotencyKey("unknown")).isEmpty();
    }

    // =========================================================================
    // findByPayerIdAndInvoiceId
    // =========================================================================

    @SuppressWarnings("unchecked")
    @Test
    void findByPayerIdAndInvoiceId_found_returnsOptional() {
        UUID payerId = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class), anyString(), anyString()))
                .thenReturn(List.of(payment));

        assertThat(repository.findByPayerIdAndInvoiceId(payerId, "INV-001"))
                .isPresent().contains(payment);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByPayerIdAndInvoiceId_notFound_returnsEmpty() {
        UUID payerId = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class), anyString(), anyString()))
                .thenReturn(List.of());

        assertThat(repository.findByPayerIdAndInvoiceId(payerId, "INV-XXX")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByPayerIdAndInvoiceId_passesPayerIdAsStringAndInvoiceId() {
        UUID payerId = UUID.randomUUID();
        when(jdbc.query(anyString(), any(RowMapper.class), anyString(), anyString()))
                .thenReturn(List.of());

        repository.findByPayerIdAndInvoiceId(payerId, "INV-007");

        verify(jdbc).query(anyString(), any(RowMapper.class),
                eq(payerId.toString()), eq("INV-007"));
    }

    // =========================================================================
    // findAll (with pagination)
    // =========================================================================

    @SuppressWarnings("unchecked")
    @Test
    void findAll_returnsPageWithCorrectContent() {
        Pageable pageable = PageRequest.of(0, 20, Sort.unsorted());
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);
        when(jdbc.query(anyString(), any(RowMapper.class), anyInt(), anyLong()))
                .thenReturn(List.of(payment));

        Page<Payment> result = repository.findAll(pageable);

        assertThat(result.getContent()).containsExactly(payment);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findAll_nullCountFromDb_treatedAsZero() {
        Pageable pageable = PageRequest.of(0, 20, Sort.unsorted());
        when(jdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(null);
        when(jdbc.query(anyString(), any(RowMapper.class), anyInt(), anyLong()))
                .thenReturn(List.of());

        Page<Payment> result = repository.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // =========================================================================
    // findByStatus (with pagination)
    // =========================================================================

    @SuppressWarnings("unchecked")
    @Test
    void findByStatus_returnsPageWithCorrectContent() {
        Pageable pageable = PageRequest.of(0, 20, Sort.unsorted());
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        when(jdbc.query(anyString(), any(RowMapper.class), anyString(), anyInt(), anyLong()))
                .thenReturn(List.of(payment));

        Page<Payment> result = repository.findByStatus(PaymentStatus.SENT, pageable);

        assertThat(result.getContent()).containsExactly(payment);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByStatus_passesStatusNameToJdbc() {
        Pageable pageable = PageRequest.of(0, 20, Sort.unsorted());
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbc.query(anyString(), any(RowMapper.class), anyString(), anyInt(), anyLong()))
                .thenReturn(List.of());

        repository.findByStatus(PaymentStatus.COMPLETED, pageable);

        // Count query receives status name
        verify(jdbc).queryForObject(anyString(), eq(Integer.class), eq("COMPLETED"));
        // Data query receives status name as first arg
        verify(jdbc).query(anyString(), any(RowMapper.class),
                eq("COMPLETED"), anyInt(), anyLong());
    }
}
