package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.model.Payment;
import com.finance.PaymentProcessing.model.PaymentStatus;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentLifecycleServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private HistoryService historyService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ScheduledExecutorService mockScheduler;
    @Mock private TransactionTemplate mockTransactionTemplate;

    private PaymentLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new PaymentLifecycleService(paymentRepository, historyService, transactionManager);
        // Replace internally-created scheduler and TransactionTemplate with controllable mocks
        ReflectionTestUtils.setField(service, "scheduler", mockScheduler);
        ReflectionTestUtils.setField(service, "transactionTemplate", mockTransactionTemplate);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Makes the mock TransactionTemplate run its Consumer inline (no real transaction). */
    @SuppressWarnings("unchecked")
    private void executeTransactionInline() {
        doAnswer(inv -> {
            Consumer<org.springframework.transaction.TransactionStatus> action = inv.getArgument(0);
            action.accept(null);
            return null;
        }).when(mockTransactionTemplate).executeWithoutResult(any());
    }

    /** Captures and runs the Runnable passed to scheduler.schedule(). */
    private void captureAndRunScheduledTask() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockScheduler).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
    }

    private Payment sentPayment(UUID id) {
        Payment p = new Payment();
        p.setPaymentId(id);
        p.setStatus(PaymentStatus.SENT);
        return p;
    }

    // =========================================================================
    // scheduleCompletion – scheduling contract
    // =========================================================================

    @Test
    void scheduleCompletion_submitsTaskWith30SecondDelay() {
        service.scheduleCompletion(UUID.randomUUID());

        verify(mockScheduler).schedule(any(Runnable.class), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    void scheduleCompletion_taskWrappedInsideTransactionTemplate() {
        executeTransactionInline();
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        service.scheduleCompletion(paymentId);
        captureAndRunScheduledTask();

        verify(mockTransactionTemplate).executeWithoutResult(any());
    }

    // =========================================================================
    // completeIfSent – SENT → COMPLETED happy path
    // =========================================================================

    @Test
    void completeIfSent_sentPayment_transitionsToCompletedAndSaves() {
        executeTransactionInline();
        UUID paymentId = UUID.randomUUID();
        Payment payment = sentPayment(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        service.scheduleCompletion(paymentId);
        captureAndRunScheduledTask();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository).save(payment);
    }

    @Test
    void completeIfSent_sentPayment_recordsHistoryTransition() {
        executeTransactionInline();
        UUID paymentId = UUID.randomUUID();
        Payment payment = sentPayment(paymentId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        service.scheduleCompletion(paymentId);
        captureAndRunScheduledTask();

        verify(historyService).recordTransition(
                eq(payment),
                eq(PaymentStatus.SENT),
                eq(PaymentStatus.COMPLETED),
                eq("Auto-completed after 30 seconds"),
                isNull(),
                eq("SYSTEM"));
    }

    // =========================================================================
    // completeIfSent – status guards
    // =========================================================================

    @Test
    void completeIfSent_createdPayment_isIgnored() {
        executeTransactionInline();
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setStatus(PaymentStatus.CREATED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.scheduleCompletion(paymentId);
        captureAndRunScheduledTask();

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(historyService);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void completeIfSent_validatedPayment_isIgnored() {
        executeTransactionInline();
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setStatus(PaymentStatus.VALIDATED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.scheduleCompletion(paymentId);
        captureAndRunScheduledTask();

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(historyService);
    }

    @Test
    void completeIfSent_alreadyCompletedPayment_isIgnored() {
        executeTransactionInline();
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.scheduleCompletion(paymentId);
        captureAndRunScheduledTask();

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(historyService);
    }

    @Test
    void completeIfSent_failedPayment_isIgnored() {
        executeTransactionInline();
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setStatus(PaymentStatus.FAILED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.scheduleCompletion(paymentId);
        captureAndRunScheduledTask();

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(historyService);
    }

    @Test
    void completeIfSent_paymentNotFound_doesNothing() {
        executeTransactionInline();
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        service.scheduleCompletion(paymentId);
        captureAndRunScheduledTask();

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(historyService);
    }

    // =========================================================================
    // shutdown
    // =========================================================================

    @Test
    void shutdown_gracefulTermination_doesNotCallShutdownNow() throws InterruptedException {
        when(mockScheduler.awaitTermination(3, TimeUnit.SECONDS)).thenReturn(true);

        service.shutdown();

        verify(mockScheduler).shutdown();
        verify(mockScheduler, never()).shutdownNow();
    }

    @Test
    void shutdown_timeoutExceeded_callsShutdownNow() throws InterruptedException {
        when(mockScheduler.awaitTermination(3, TimeUnit.SECONDS)).thenReturn(false);

        service.shutdown();

        verify(mockScheduler).shutdown();
        verify(mockScheduler).shutdownNow();
    }

    @Test
    void shutdown_interruptedException_callsShutdownNowAndResetsInterruptFlag() throws InterruptedException {
        when(mockScheduler.awaitTermination(3, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException("interrupted"));

        service.shutdown();

        verify(mockScheduler).shutdownNow();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        // Clear the interrupt flag so it does not affect subsequent tests
        Thread.interrupted();
    }
}
