package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.dto.PaymentRequest;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.*;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentFailureAuditServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private HistoryService historyService;

    @InjectMocks
    private PaymentFailureAuditService service;

    private String payerId;
    private String beneficiaryId;
    private String sourceAccountId;
    private String idempotencyKey;
    private RuntimeException cause;

    @BeforeEach
    void setUp() {
        payerId        = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        beneficiaryId  = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        sourceAccountId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        idempotencyKey = "idem-key-001";
        cause          = new BadRequestException("VALIDATION_FAILED", "bad input");
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /** Builds a minimal valid PaymentRequest. Override individual fields per test. */
    private PaymentRequest request(String beneId, String srcId, BigDecimal amount,
            String currency, String reference, PaymentMethod method,
            PaymentType type, String cardNumber, String cardHolder,
            String upiId) {
        return new PaymentRequest(amount, currency, reference, payerId, method,
                srcId, beneId, null, cardHolder, cardNumber,
                null, null, null, upiId, type, null);
    }

    private PaymentRequest defaultRequest() {
        return request(beneficiaryId, sourceAccountId,
                new BigDecimal("500.00"), "INR", "REF-001",
                PaymentMethod.NET_BANKING, PaymentType.BENEFICIARY_TRANSFER,
                null, null, null);
    }

    private Beneficiary stubBeneficiary(String id) {
        Beneficiary b = new Beneficiary();
        b.setBeneficiaryId(id);
        return b;
    }

    private Payment stubSavedPayment() {
        Payment p = new Payment();
        p.setPaymentId(com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId());
        p.setStatus(PaymentStatus.FAILED);
        return p;
    }

    // =========================================================================
    // persistFailedAttempt – beneficiary resolution
    // =========================================================================

    @Test
    void persistFailedAttempt_withValidBeneficiaryId_usesThatId() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        Payment saved = stubSavedPayment();
        when(paymentRepository.save(any())).thenReturn(saved);

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getBeneficiaryId()).isEqualTo(beneficiaryId);
    }

    @Test
    void persistFailedAttempt_withNullBeneficiaryId_fallsBackToFirstBeneficiary() {
        String fallbackId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        Beneficiary fallback = stubBeneficiary(fallbackId);

        // existsById is never called when requestedId is null (short-circuit &&)
        when(beneficiaryRepository.findAll()).thenReturn(List.of(fallback));
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        Payment saved = stubSavedPayment();
        when(paymentRepository.save(any())).thenReturn(saved);

        PaymentRequest req = request(null, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getBeneficiaryId()).isEqualTo(fallbackId);
    }

    @Test
    void persistFailedAttempt_withUnknownBeneficiaryId_fallsBackToFirstBeneficiary() {
        String unknownBeneId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        String fallbackId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        Beneficiary fallback = stubBeneficiary(fallbackId);

        when(beneficiaryRepository.existsById(unknownBeneId)).thenReturn(false);
        when(beneficiaryRepository.findAll()).thenReturn(List.of(fallback));
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(unknownBeneId, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getBeneficiaryId()).isEqualTo(fallbackId);
    }

    @Test
    void persistFailedAttempt_noBeneficiaryAvailableAtAll_doesNothing() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(false);
        when(beneficiaryRepository.findAll()).thenReturn(List.of());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, cause);

        verifyNoInteractions(paymentRepository, historyService);
    }

    // =========================================================================
    // persistFailedAttempt – source account resolution
    // =========================================================================

    @Test
    void persistFailedAttempt_withValidSourceAccountId_usesIt() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getSourceAccountId()).isEqualTo(sourceAccountId);
    }

    @Test
    void persistFailedAttempt_withUnknownSourceAccountId_storesNull() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(false);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getSourceAccountId()).isNull();
    }

    @Test
    void persistFailedAttempt_withNullSourceAccountId_storesNull() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, null, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getSourceAccountId()).isNull();
    }

    // =========================================================================
    // persistFailedAttempt – saved payment fields
    // =========================================================================

    @Test
    void persistFailedAttempt_savedPaymentHasFailedStatusAndUniqueIdempotencyKey() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(saved.getIdempotencyKey()).startsWith(idempotencyKey + "-FAILED-");
        assertThat(saved.getInvoiceId()).isNull();
    }

    @Test
    void persistFailedAttempt_nullPaymentType_defaultsToBeneficiaryTransfer() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentType()).isEqualTo(PaymentType.BENEFICIARY_TRANSFER);
    }

    @Test
    void persistFailedAttempt_nullPaymentMethod_defaultsToNetBanking() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        // PaymentRequest requires non-null paymentMethod by annotation, but we pass null
        // to exercise the defaulting branch directly.
        PaymentRequest req = new PaymentRequest(new BigDecimal("100"), "INR", "REF",
                payerId, null, sourceAccountId, beneficiaryId, null,
                null, null, null, null, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.NET_BANKING);
    }

    // =========================================================================
    // persistFailedAttempt – history recording
    // =========================================================================

    @Test
    void persistFailedAttempt_recordsHistoryTransitionWithSystemActor() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        Payment saved = stubSavedPayment();
        when(paymentRepository.save(any())).thenReturn(saved);

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, cause);

        verify(historyService).recordTransition(
                eq(saved),
                isNull(),
                eq(PaymentStatus.FAILED),
                eq("bad input"),
                eq("VALIDATION_FAILED"),
                eq("SYSTEM"));
    }

    // =========================================================================
    // persistFailedAttempt – error code resolution
    // =========================================================================

    @Test
    void persistFailedAttempt_badRequestException_usesItsErrorCode() {
        BadRequestException ex = new BadRequestException("AMOUNT_TOO_LOW", "amount is too low");
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, ex);

        verify(historyService).recordTransition(any(), isNull(), eq(PaymentStatus.FAILED),
                any(), eq("AMOUNT_TOO_LOW"), any());
    }

    @Test
    void persistFailedAttempt_conflictException_usesItsErrorCode() {
        ConflictException ex = new ConflictException("DUPLICATE_INVOICE", "duplicate");
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, ex);

        verify(historyService).recordTransition(any(), isNull(), eq(PaymentStatus.FAILED),
                any(), eq("DUPLICATE_INVOICE"), any());
    }

    @Test
    void persistFailedAttempt_notFoundException_usesItsErrorCode() {
        NotFoundException ex = new NotFoundException("RESOURCE_NOT_FOUND", "not found");
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, ex);

        verify(historyService).recordTransition(any(), isNull(), eq(PaymentStatus.FAILED),
                any(), eq("RESOURCE_NOT_FOUND"), any());
    }

    @Test
    void persistFailedAttempt_genericRuntimeException_defaultsToValidationFailed() {
        RuntimeException ex = new RuntimeException("unexpected");
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, ex);

        verify(historyService).recordTransition(any(), isNull(), eq(PaymentStatus.FAILED),
                any(), eq("VALIDATION_FAILED"), any());
    }

    // =========================================================================
    // persistFailedAttempt – amount sanitization
    // =========================================================================

    @Test
    void persistFailedAttempt_validAmount_storedAsIs() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        service.persistFailedAttempt(defaultRequest(), idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void persistFailedAttempt_nullAmount_defaultsToOne() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, null,
                "INR", "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void persistFailedAttempt_zeroAmount_defaultsToOne() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, BigDecimal.ZERO,
                "INR", "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void persistFailedAttempt_negativeAmount_defaultsToOne() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("-10"),
                "INR", "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.ONE);
    }

    // =========================================================================
    // persistFailedAttempt – currency sanitization
    // =========================================================================

    @ParameterizedTest(name = "currency ''{0}'' → ''{1}''")
    @CsvSource({
        "INR,    INR",
        "usd,    USD",
        " eur ,  EUR",
        "GBPXYZ, GBP",
        "ab,     ABI"   // padded with INR, then substring(0,3) = "ABI"
    })
    void persistFailedAttempt_currencySanitization(String input, String expected) {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                input, "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "blank currency ''{0}'' defaults to INR")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void persistFailedAttempt_blankCurrencyDefaultsToInr(String currency) {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                currency, "REF", PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo("INR");
    }

    // =========================================================================
    // persistFailedAttempt – reference sanitization
    // =========================================================================

    @ParameterizedTest(name = "blank reference ''{0}'' defaults to FAILED_VALIDATION")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void persistFailedAttempt_blankReferenceDefaultsToFailedValidation(String ref) {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", ref, PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getReference()).isEqualTo("FAILED_VALIDATION");
    }

    @Test
    void persistFailedAttempt_referenceLongerThan255_isTruncated() {
        String longRef = "R".repeat(300);
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", longRef, PaymentMethod.NET_BANKING, null, null, null, null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getReference()).hasSize(255);
    }

    // =========================================================================
    // persistFailedAttempt – card / UPI sanitization
    // =========================================================================

    @Test
    void persistFailedAttempt_cardNumber16Digits_storesLast4() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.CARD, null, "4111111111111234", "John Doe", null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCardLast4()).isEqualTo("1234");
    }

    @Test
    void persistFailedAttempt_cardNumberWithSpaces_storesLast4OfDigitsOnly() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.CARD, null, "4111 1111 1111 5678", "Jane", null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCardLast4()).isEqualTo("5678");
    }

    @Test
    void persistFailedAttempt_cardNumberFewerThan4Digits_storesNullCardLast4() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.CARD, null, "123", "Test", null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCardLast4()).isNull();
    }

    @Test
    void persistFailedAttempt_cardHolderNameTrimmed() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.CARD, null, "4111111111111111", "  John Doe  ", null);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCardHolderName()).isEqualTo("John Doe");
    }

    @Test
    void persistFailedAttempt_upiIdNormalisedToLowerCase() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.UPI, null, null, null, "  USER@UPI ");
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getUpiId()).isEqualTo("user@upi");
    }

    @ParameterizedTest(name = "blank upiId ''{0}'' stored as null")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void persistFailedAttempt_blankUpiIdStoredAsNull(String upiId) {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenReturn(stubSavedPayment());

        PaymentRequest req = request(beneficiaryId, sourceAccountId, new BigDecimal("100"),
                "INR", "REF", PaymentMethod.NET_BANKING, null, null, null, upiId);
        service.persistFailedAttempt(req, idempotencyKey, cause);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getUpiId()).isNull();
    }

    // =========================================================================
    // persistFailedAttempt – swallows secondary exceptions
    // =========================================================================

    @Test
    void persistFailedAttempt_saveThrows_doesNotPropagate() {
        when(beneficiaryRepository.existsById(beneficiaryId)).thenReturn(true);
        when(bankAccountRepository.existsById(sourceAccountId)).thenReturn(true);
        when(paymentRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> service.persistFailedAttempt(defaultRequest(), idempotencyKey, cause))
                .doesNotThrowAnyException();

        verifyNoInteractions(historyService);
    }
}
