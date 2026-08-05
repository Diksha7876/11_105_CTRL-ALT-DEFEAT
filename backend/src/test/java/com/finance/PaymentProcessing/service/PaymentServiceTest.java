package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.dto.*;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.ConflictException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.*;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import com.finance.PaymentProcessing.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private ValidationService validationService;
    @Mock private HistoryService historyService;
    @Mock private PaymentLifecycleService paymentLifecycleService;
    @Mock private PaymentFailureAuditService paymentFailureAuditService;
    @Mock private CurrencyConversionService currencyConversionService;

    @InjectMocks
    private PaymentService service;

    private UUID paymentId;
    private UUID payerId;
    private UUID sourceAccountId;
    private UUID beneficiaryId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        paymentId     = UUID.randomUUID();
        payerId       = UUID.randomUUID();
        sourceAccountId = UUID.randomUUID();
        beneficiaryId = UUID.randomUUID();
        idempotencyKey = "test-idem-key-001";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PaymentRequest netBankingRequest() {
        return new PaymentRequest(
                new BigDecimal("200.00"), "INR", "REF-NB", payerId,
                PaymentMethod.NET_BANKING, sourceAccountId, beneficiaryId,
                null, null, null, null, null, null, null,
                PaymentType.BENEFICIARY_TRANSFER, null);
    }

    private PaymentRequest cardRequest() {
        return new PaymentRequest(
                new BigDecimal("150.00"), "USD", "REF-CARD", payerId,
                PaymentMethod.CARD, sourceAccountId, null,
                CardType.CREDIT_CARD, "  Jane Doe  ", "4111111111111234",
                "12", "2028", "123", null,
                PaymentType.BENEFICIARY_TRANSFER, null);
    }

    private PaymentRequest upiRequest() {
        return new PaymentRequest(
                new BigDecimal("50.00"), "INR", "REF-UPI", payerId,
                PaymentMethod.UPI, sourceAccountId, null,
                null, null, null, null, null, null, "  USER@UPI ",
                PaymentType.BENEFICIARY_TRANSFER, null);
    }

    private BankAccount activeAccount(BigDecimal balance) {
        BankAccount a = new BankAccount();
        a.setAccountId(sourceAccountId);
        a.setPayerId(payerId);
        a.setBalanceInInr(balance);
        a.setActive(true);
        return a;
    }

    private Payment savedPayment(PaymentStatus status) {
        Payment p = new Payment();
        p.setPaymentId(paymentId);
        p.setStatus(status);
        p.setAmount(new BigDecimal("200.00"));
        p.setCurrency("INR");
        return p;
    }

    /** Stubs the happy-path for a NET_BANKING payment ready to be saved. */
    private void stubNetBankingHappyPath(BigDecimal balance, BigDecimal debitInInr) {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(activeAccount(balance)));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(debitInInr);
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));
    }

    // =========================================================================
    // createPayment – idempotency
    // =========================================================================

    @Test
    void createPayment_duplicateIdempotencyKey_returnsExistingWithCreatedFalse() {
        Payment existing = savedPayment(PaymentStatus.SENT);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existing));

        PaymentCreationResult result = service.createPayment(netBankingRequest(), idempotencyKey);

        assertThat(result.created()).isFalse();
        assertThat(result.payment().paymentId()).isEqualTo(paymentId);
        verifyNoInteractions(validationService, bankAccountRepository,
                historyService, paymentLifecycleService, paymentFailureAuditService);
    }

    // =========================================================================
    // createPayment – early validation failures
    // =========================================================================

    @Test
    void createPayment_validateAmountThrows_auditCalledAndExceptionRethrown() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        BadRequestException ex = new BadRequestException("VALIDATION_FAILED", "bad amount");
        doThrow(ex).when(validationService).validateAmount(any());

        assertThatThrownBy(() -> service.createPayment(netBankingRequest(), idempotencyKey))
                .isSameAs(ex);
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), same(ex));
    }

    @Test
    void createPayment_validateCurrencyThrows_auditCalledAndExceptionRethrown() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        BadRequestException ex = new BadRequestException("VALIDATION_FAILED", "bad currency");
        doThrow(ex).when(validationService).validateCurrency(any());

        assertThatThrownBy(() -> service.createPayment(netBankingRequest(), idempotencyKey))
                .isSameAs(ex);
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), same(ex));
    }

    @Test
    void createPayment_nullPayerId_throwsBadRequestAndTriggersAudit() {
        PaymentRequest req = new PaymentRequest(
                new BigDecimal("100"), "INR", "R", null,
                PaymentMethod.NET_BANKING, sourceAccountId, beneficiaryId,
                null, null, null, null, null, null, null,
                PaymentType.BENEFICIARY_TRANSFER, null);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayment(req, idempotencyKey))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("payerId is required");
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), any());
    }

    @Test
    void createPayment_netBankingNullSourceAccountId_throwsBadRequestAndTriggersAudit() {
        PaymentRequest req = new PaymentRequest(
                new BigDecimal("100"), "INR", "R", payerId,
                PaymentMethod.NET_BANKING, null, beneficiaryId,
                null, null, null, null, null, null, null,
                PaymentType.BENEFICIARY_TRANSFER, null);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayment(req, idempotencyKey))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("sourceAccountId is required for net banking");
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), any());
    }

    @Test
    void createPayment_duplicateInvoice_throwsConflictAndTriggersAudit() {
        PaymentRequest req = new PaymentRequest(
                new BigDecimal("100"), "INR", "R", payerId,
                PaymentMethod.NET_BANKING, sourceAccountId, beneficiaryId,
                null, null, null, null, null, null, null,
                PaymentType.BILL_PAYMENT, "INV-001");
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        // ConflictException is thrown before bankAccountRepository.findById is reached
        when(paymentRepository.findByPayerIdAndInvoiceId(payerId, "INV-001"))
                .thenReturn(Optional.of(savedPayment(PaymentStatus.SENT)));

        assertThatThrownBy(() -> service.createPayment(req, idempotencyKey))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been paid");
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), any(ConflictException.class));
    }

    // =========================================================================
    // createPayment – source account resolution
    // =========================================================================

    @Test
    void createPayment_sourceAccountNotFound_throwsNotFoundAndTriggersAudit() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayment(netBankingRequest(), idempotencyKey))
                .isInstanceOf(NotFoundException.class);
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), any());
    }

    @Test
    void createPayment_sourceAccountBelongsToDifferentPayer_throwsBadRequestAndTriggersAudit() {
        BankAccount foreignAccount = new BankAccount();
        foreignAccount.setAccountId(sourceAccountId);
        foreignAccount.setPayerId(UUID.randomUUID()); // different payer
        foreignAccount.setBalanceInInr(new BigDecimal("9999"));
        foreignAccount.setActive(true);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId)).thenReturn(Optional.of(foreignAccount));

        assertThatThrownBy(() -> service.createPayment(netBankingRequest(), idempotencyKey))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to current payer");
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), any());
    }

    @Test
    void createPayment_sourceAccountInactive_throwsBadRequestAndTriggersAudit() {
        BankAccount inactive = activeAccount(new BigDecimal("9999"));
        inactive.setActive(false);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.createPayment(netBankingRequest(), idempotencyKey))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive");
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), any());
    }

    @Test
    void createPayment_noSourceAccountIdAndCard_picksActiveAccountFromRepo() {
        PaymentRequest req = new PaymentRequest(
                new BigDecimal("150.00"), "USD", "REF", payerId,
                PaymentMethod.CARD, null, null,
                CardType.CREDIT_CARD, "Jane", "4111111111111234",
                "12", "2028", "123", null,
                PaymentType.BENEFICIARY_TRANSFER, null);

        BankAccount shared = new BankAccount();
        UUID sharedId = UUID.randomUUID();
        shared.setAccountId(sharedId);
        shared.setPayerId(payerId);
        shared.setBalanceInInr(new BigDecimal("9999"));
        shared.setActive(true);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findAll()).thenReturn(List.of(shared));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(new BigDecimal("150.00"));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationService.normalizeCardNumber("4111111111111234")).thenReturn("4111111111111234");
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));

        PaymentCreationResult result = service.createPayment(req, idempotencyKey);

        assertThat(result.created()).isTrue();
    }

    @Test
    void createPayment_noSourceAccountIdNoActiveAccount_throwsBadRequestAndTriggersAudit() {
        PaymentRequest req = new PaymentRequest(
                new BigDecimal("100"), "INR", "R", payerId,
                PaymentMethod.CARD, null, null,
                CardType.CREDIT_CARD, "Jane", "4111111111111234",
                "12", "2028", "123", null,
                PaymentType.BENEFICIARY_TRANSFER, null);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.createPayment(req, idempotencyKey))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No active source account");
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), any());
    }

    // =========================================================================
    // createPayment – insufficient balance
    // =========================================================================

    @Test
    void createPayment_insufficientBalance_throwsBadRequestAndTriggersAudit() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(activeAccount(new BigDecimal("50.00"))));
        when(currencyConversionService.convertToInr(any(), anyString()))
                .thenReturn(new BigDecimal("200.00")); // debit > balance

        assertThatThrownBy(() -> service.createPayment(netBankingRequest(), idempotencyKey))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INSUFFICIENT_FUNDS"));
        verify(paymentFailureAuditService).persistFailedAttempt(any(), eq(idempotencyKey), any());
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    void createPayment_nullBalance_treatedAsZero_throwsInsufficientFunds() {
        BankAccount account = activeAccount(null);
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId)).thenReturn(Optional.of(account));
        when(currencyConversionService.convertToInr(any(), anyString()))
                .thenReturn(new BigDecimal("10.00"));

        assertThatThrownBy(() -> service.createPayment(netBankingRequest(), idempotencyKey))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INSUFFICIENT_FUNDS"));
    }

    // =========================================================================
    // createPayment – NET_BANKING success
    // =========================================================================

    @Test
    void createPayment_netBanking_returnsCreatedTrueWithSentStatus() {
        stubNetBankingHappyPath(new BigDecimal("1000.00"), new BigDecimal("200.00"));

        PaymentCreationResult result = service.createPayment(netBankingRequest(), idempotencyKey);

        assertThat(result.created()).isTrue();
        assertThat(result.payment().status()).isEqualTo(PaymentStatus.SENT);
    }

    @Test
    void createPayment_netBanking_debitsBalanceAndSavesAccount() {
        BankAccount account = activeAccount(new BigDecimal("1000.00"));
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId)).thenReturn(Optional.of(account));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(new BigDecimal("200.00"));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));

        service.createPayment(netBankingRequest(), idempotencyKey);

        assertThat(account.getBalanceInInr()).isEqualByComparingTo("800.00");
        verify(bankAccountRepository).save(account);
    }

    @Test
    void createPayment_netBanking_savesPaymentWithSentStatusAndIdempotencyKey() {
        stubNetBankingHappyPath(new BigDecimal("1000.00"), new BigDecimal("200.00"));

        service.createPayment(netBankingRequest(), idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SENT);
        assertThat(saved.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(saved.getPayerId()).isEqualTo(payerId);
        assertThat(saved.getBeneficiaryId()).isEqualTo(beneficiaryId);
        assertThat(saved.getSourceAccountId()).isEqualTo(sourceAccountId);
        assertThat(saved.getCurrency()).isEqualTo("INR");
    }

    @Test
    void createPayment_netBanking_recordsHistoryAndSchedulesCompletion() {
        stubNetBankingHappyPath(new BigDecimal("1000.00"), new BigDecimal("200.00"));
        Payment returnedPayment = savedPayment(PaymentStatus.SENT);
        when(paymentRepository.save(any())).thenReturn(returnedPayment);

        service.createPayment(netBankingRequest(), idempotencyKey);

        verify(historyService).recordTransition(eq(returnedPayment), isNull(),
                eq(PaymentStatus.SENT), anyString(), isNull(), eq("SYSTEM"));
        verify(paymentLifecycleService).scheduleCompletion(paymentId);
    }

    // =========================================================================
    // createPayment – CARD payment
    // =========================================================================

    @Test
    void createPayment_card_storesLast4AndTrimmedHolderName() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(activeAccount(new BigDecimal("9999"))));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(new BigDecimal("150.00"));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationService.normalizeCardNumber("4111111111111234")).thenReturn("4111111111111234");
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));

        service.createPayment(cardRequest(), idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCardLast4()).isEqualTo("1234");
        assertThat(captor.getValue().getCardHolderName()).isEqualTo("Jane Doe");
        assertThat(captor.getValue().getUpiId()).isNull();
    }

    @Test
    void createPayment_card_currencyStoredUpperCase() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(activeAccount(new BigDecimal("9999"))));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(new BigDecimal("100.00"));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationService.normalizeCardNumber(any())).thenReturn("4111111111111234");
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));

        // Request with lowercase currency
        PaymentRequest req = new PaymentRequest(
                new BigDecimal("1"), "usd", "R", payerId, PaymentMethod.CARD,
                sourceAccountId, null, CardType.CREDIT_CARD, "J", "4111111111111234",
                "12", "2028", "123", null, PaymentType.BENEFICIARY_TRANSFER, null);
        service.createPayment(req, idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo("USD");
    }

    // =========================================================================
    // createPayment – UPI payment
    // =========================================================================

    @Test
    void createPayment_upi_storesLowercaseTrimmedUpiId() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(activeAccount(new BigDecimal("9999"))));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(new BigDecimal("50.00"));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));

        service.createPayment(upiRequest(), idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getUpiId()).isEqualTo("user@upi");
        assertThat(captor.getValue().getCardLast4()).isNull();
        assertThat(captor.getValue().getCardHolderName()).isNull();
    }

    // =========================================================================
    // createPayment – reference resolution
    // =========================================================================

    @Test
    void createPayment_blankReference_netBanking_defaultsLabel() {
        stubNetBankingHappyPath(new BigDecimal("1000.00"), new BigDecimal("200.00"));
        PaymentRequest req = new PaymentRequest(
                new BigDecimal("200"), "INR", "   ", payerId,
                PaymentMethod.NET_BANKING, sourceAccountId, beneficiaryId,
                null, null, null, null, null, null, null,
                PaymentType.BENEFICIARY_TRANSFER, null);

        service.createPayment(req, idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getReference()).isEqualTo("Net banking payment");
    }

    @Test
    void createPayment_nullReference_card_defaultsLabel() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(activeAccount(new BigDecimal("9999"))));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(new BigDecimal("100.00"));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationService.normalizeCardNumber(any())).thenReturn("4111111111111234");
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));

        PaymentRequest req = new PaymentRequest(
                new BigDecimal("150"), "USD", null, payerId,
                PaymentMethod.CARD, sourceAccountId, null,
                CardType.CREDIT_CARD, "Jane", "4111111111111234",
                "12", "2028", "123", null,
                PaymentType.BENEFICIARY_TRANSFER, null);
        service.createPayment(req, idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getReference()).isEqualTo("Card payment");
    }

    @Test
    void createPayment_nullReference_upi_defaultsLabel() {
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(activeAccount(new BigDecimal("9999"))));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(new BigDecimal("50.00"));
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));

        PaymentRequest req = new PaymentRequest(
                new BigDecimal("50"), "INR", null, payerId,
                PaymentMethod.UPI, sourceAccountId, null,
                null, null, null, null, null, null, "user@upi",
                PaymentType.BENEFICIARY_TRANSFER, null);
        service.createPayment(req, idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getReference()).isEqualTo("UPI payment");
    }

    // =========================================================================
    // createPayment – invoice handling
    // =========================================================================

    @Test
    void createPayment_billPayment_invoiceIdStoredTrimmed() {
        PaymentRequest req = new PaymentRequest(
                new BigDecimal("100"), "INR", "R", payerId,
                PaymentMethod.NET_BANKING, sourceAccountId, beneficiaryId,
                null, null, null, null, null, null, null,
                PaymentType.BILL_PAYMENT, "  INV-007  ");
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(bankAccountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(activeAccount(new BigDecimal("9999"))));
        when(currencyConversionService.convertToInr(any(), anyString())).thenReturn(new BigDecimal("100.00"));
        when(paymentRepository.findByPayerIdAndInvoiceId(payerId, "INV-007")).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any())).thenReturn(savedPayment(PaymentStatus.SENT));

        service.createPayment(req, idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getInvoiceId()).isEqualTo("INV-007");
    }

    @Test
    void createPayment_beneficiaryTransfer_invoiceIdAlwaysNull() {
        stubNetBankingHappyPath(new BigDecimal("1000.00"), new BigDecimal("200.00"));

        service.createPayment(netBankingRequest(), idempotencyKey);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getInvoiceId()).isNull();
    }

    // =========================================================================
    // getPayment
    // =========================================================================

    @Test
    void getPayment_found_returnsMappedResponse() {
        Payment p = savedPayment(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(p));

        PaymentResponse response = service.getPayment(paymentId);

        assertThat(response.paymentId()).isEqualTo(paymentId);
        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void getPayment_notFound_throwsNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(paymentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPayment(unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // =========================================================================
    // updateStatus
    // =========================================================================

    @Test
    void updateStatus_validTransition_updatesStatusSavesAndRecordsHistory() {
        Payment payment = savedPayment(PaymentStatus.SENT);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        PaymentStatusRequest req = new PaymentStatusRequest(
                PaymentStatus.COMPLETED, "all good", null, "OPS");

        PaymentResponse response = service.updateStatus(paymentId, req);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(validationService).validateStatusTransition(PaymentStatus.SENT, PaymentStatus.COMPLETED);
        verify(paymentRepository).save(payment);
        verify(historyService).recordTransition(eq(payment),
                eq(PaymentStatus.SENT), eq(PaymentStatus.COMPLETED),
                eq("all good"), isNull(), eq("OPS"));
        assertThat(response.status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void updateStatus_paymentNotFound_throwsNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(paymentRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(unknownId,
                new PaymentStatusRequest(PaymentStatus.COMPLETED, null, null, null)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(historyService);
    }

    @Test
    void updateStatus_invalidTransition_validationServiceThrows() {
        Payment payment = savedPayment(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        doThrow(new BadRequestException("INVALID_TRANSITION", "cannot transition"))
                .when(validationService).validateStatusTransition(PaymentStatus.COMPLETED, PaymentStatus.SENT);

        assertThatThrownBy(() -> service.updateStatus(paymentId,
                new PaymentStatusRequest(PaymentStatus.SENT, null, null, null)))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(historyService);
    }

    // =========================================================================
    // listPayments
    // =========================================================================

    @Test
    void listPayments_withStatus_callsFindByStatus() {
        Pageable pageable = Pageable.ofSize(10);
        Payment p = savedPayment(PaymentStatus.SENT);
        Page<Payment> page = new PageImpl<>(List.of(p));
        when(paymentRepository.findByStatus(PaymentStatus.SENT, pageable)).thenReturn(page);

        Page<PaymentResponse> result = service.listPayments(PaymentStatus.SENT, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(PaymentStatus.SENT);
        verify(paymentRepository).findByStatus(PaymentStatus.SENT, pageable);
        verify(paymentRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listPayments_nullStatus_callsFindAll() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Payment> emptyPage = Page.empty(pageable);
        when(paymentRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<PaymentResponse> result = service.listPayments(null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(paymentRepository).findAll(pageable);
        verify(paymentRepository, never()).findByStatus(any(), any());
    }
}
