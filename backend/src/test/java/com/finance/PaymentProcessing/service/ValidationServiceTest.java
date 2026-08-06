package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.*;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock private BeneficiaryRepository beneficiaryRepository;
    @Mock private BankAccountRepository accountRepository;

    private ValidationService service;

    @BeforeEach
    void setUp() {
        service = new ValidationService(beneficiaryRepository, accountRepository);
    }

    // =========================================================================
    // validateAmount
    // =========================================================================

    @Test
    void validateAmount_positive_doesNotThrow() {
        assertThatCode(() -> service.validateAmount(new BigDecimal("0.01"))).doesNotThrowAnyException();
        assertThatCode(() -> service.validateAmount(new BigDecimal("1000000"))).doesNotThrowAnyException();
    }

    @Test
    void validateAmount_null_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateAmount(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void validateAmount_zero_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateAmount(BigDecimal.ZERO))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void validateAmount_negative_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateAmount(new BigDecimal("-0.01")))
                .isInstanceOf(BadRequestException.class);
    }

    // =========================================================================
    // validateCurrency
    // =========================================================================

    @ParameterizedTest(name = "currency ''{0}'' is supported")
    @ValueSource(strings = {"INR", "USD", "EUR", "GBP", "inr", "usd", "eur", "gbp", "Inr"})
    void validateCurrency_supportedCurrencies_doesNotThrow(String currency) {
        assertThatCode(() -> service.validateCurrency(currency)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "currency ''{0}'' is not supported")
    @NullAndEmptySource
    @ValueSource(strings = {"JPY", "AUD", "CAD", "XYZ", "US", ""})
    void validateCurrency_unsupportedOrNull_throwsBadRequest(String currency) {
        assertThatThrownBy(() -> service.validateCurrency(currency))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported currency");
    }

    // =========================================================================
    // validateBeneficiary
    // =========================================================================

    @Test
    void validateBeneficiary_exists_doesNotThrow() {
        UUID id = UUID.randomUUID();
        when(beneficiaryRepository.existsById(id)).thenReturn(true);
        assertThatCode(() -> service.validateBeneficiary(id)).doesNotThrowAnyException();
    }

    @Test
    void validateBeneficiary_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(beneficiaryRepository.existsById(id)).thenReturn(false);
        assertThatThrownBy(() -> service.validateBeneficiary(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // =========================================================================
    // validateSourceAccount
    // =========================================================================

    private BankAccount activeAccount(UUID accountId, String accountNumber) {
        BankAccount a = new BankAccount();
        a.setAccountId(accountId);
        a.setAccountNumber(accountNumber);
        a.setActive(true);
        return a;
    }

    private Beneficiary beneficiary(UUID beneficiaryId, String accountNumber) {
        Beneficiary b = new Beneficiary();
        b.setBeneficiaryId(beneficiaryId);
        b.setAccountNumber(accountNumber);
        return b;
    }

    @Test
    void validateSourceAccount_validDifferentAccounts_doesNotThrow() {
        UUID srcId  = UUID.randomUUID();
        UUID beneId = UUID.randomUUID();
        when(accountRepository.findById(srcId))
                .thenReturn(Optional.of(activeAccount(srcId, "ACC-001")));
        when(beneficiaryRepository.findById(beneId))
                .thenReturn(Optional.of(beneficiary(beneId, "ACC-999")));

        assertThatCode(() -> service.validateSourceAccount(srcId, beneId)).doesNotThrowAnyException();
    }

    @Test
    void validateSourceAccount_sourceAccountNotFound_throwsNotFoundException() {
        UUID srcId  = UUID.randomUUID();
        UUID beneId = UUID.randomUUID();
        when(accountRepository.findById(srcId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateSourceAccount(srcId, beneId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(srcId.toString());
    }

    @Test
    void validateSourceAccount_beneficiaryNotFound_throwsNotFoundException() {
        UUID srcId  = UUID.randomUUID();
        UUID beneId = UUID.randomUUID();
        when(accountRepository.findById(srcId))
                .thenReturn(Optional.of(activeAccount(srcId, "ACC-001")));
        when(beneficiaryRepository.findById(beneId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateSourceAccount(srcId, beneId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(beneId.toString());
    }

    @Test
    void validateSourceAccount_inactiveSourceAccount_throwsBadRequest() {
        UUID srcId  = UUID.randomUUID();
        UUID beneId = UUID.randomUUID();
        BankAccount inactive = activeAccount(srcId, "ACC-001");
        inactive.setActive(false);
        when(accountRepository.findById(srcId)).thenReturn(Optional.of(inactive));
        when(beneficiaryRepository.findById(beneId))
                .thenReturn(Optional.of(beneficiary(beneId, "ACC-999")));

        assertThatThrownBy(() -> service.validateSourceAccount(srcId, beneId))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_ACCOUNT"));
    }

    @Test
    void validateSourceAccount_sameAccountNumber_throwsBadRequest() {
        UUID srcId  = UUID.randomUUID();
        UUID beneId = UUID.randomUUID();
        when(accountRepository.findById(srcId))
                .thenReturn(Optional.of(activeAccount(srcId, "SAME-ACCOUNT")));
        when(beneficiaryRepository.findById(beneId))
                .thenReturn(Optional.of(beneficiary(beneId, "SAME-ACCOUNT")));

        assertThatThrownBy(() -> service.validateSourceAccount(srcId, beneId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("different active accounts");
    }

    // =========================================================================
    // validatePaymentDetails
    // =========================================================================

    @Test
    void validatePaymentDetails_nullType_doesNotThrow() {
        assertThatCode(() -> service.validatePaymentDetails(null, PaymentMethod.NET_BANKING, null)).doesNotThrowAnyException();
        assertThatCode(() -> service.validatePaymentDetails(null, PaymentMethod.CARD, "INV-001")).doesNotThrowAnyException();
    }

    @Test
    void validatePaymentDetails_billPaymentWithInvoiceId_doesNotThrow() {
        assertThatCode(() -> service.validatePaymentDetails(PaymentType.BILL_PAYMENT, PaymentMethod.NET_BANKING, "INV-001"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "BILL_PAYMENT with invoiceId=''{0}'' throws")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validatePaymentDetails_billPaymentBlankOrNullInvoiceId_throwsBadRequest(String invoiceId) {
        assertThatThrownBy(() -> service.validatePaymentDetails(PaymentType.BILL_PAYMENT, PaymentMethod.NET_BANKING, invoiceId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invoiceId is required");
    }

    @Test
    void validatePaymentDetails_beneficiaryTransferNetBankingNullInvoiceId_doesNotThrow() {
        assertThatCode(() -> service.validatePaymentDetails(PaymentType.BENEFICIARY_TRANSFER, PaymentMethod.NET_BANKING, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePaymentDetails_beneficiaryTransferNetBankingWithInvoiceId_throwsBadRequest() {
        assertThatThrownBy(() -> service.validatePaymentDetails(PaymentType.BENEFICIARY_TRANSFER, PaymentMethod.NET_BANKING, "INV-001"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be supplied");
    }

    @Test
    void validatePaymentDetails_beneficiaryTransferCardWithInvoiceId_doesNotThrow() {
        assertThatCode(() -> service.validatePaymentDetails(PaymentType.BENEFICIARY_TRANSFER, PaymentMethod.CARD, "INV-001"))
                .doesNotThrowAnyException();
    }

    @Test
    void validatePaymentDetails_beneficiaryTransferUpiWithInvoiceId_doesNotThrow() {
        assertThatCode(() -> service.validatePaymentDetails(PaymentType.BENEFICIARY_TRANSFER, PaymentMethod.UPI, "INV-001"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} requires invoiceId")
    @CsvSource({"CARD", "UPI"})
    void validatePaymentDetails_cardAndUpiWithoutInvoiceId_throwsBadRequest(PaymentMethod paymentMethod) {
        assertThatThrownBy(() -> service.validatePaymentDetails(PaymentType.BENEFICIARY_TRANSFER, paymentMethod, "   "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invoiceId is required for card and UPI payments");
    }

    // =========================================================================
    // validateMethodSpecificDetails – null method
    // =========================================================================

    @Test
    void validateMethodSpecificDetails_nullMethod_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                null, null, null, null, null, null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("paymentMethod is required");
    }

    // =========================================================================
    // validateMethodSpecificDetails – NET_BANKING
    // =========================================================================

    @Test
    void validateMethodSpecificDetails_netBanking_valid_doesNotThrow() {
        assertThatCode(() -> service.validateMethodSpecificDetails(
                PaymentMethod.NET_BANKING, UUID.randomUUID(),
                null, null, null, null, null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validateMethodSpecificDetails_netBanking_nullBeneficiaryId_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.NET_BANKING, null,
                null, null, null, null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("beneficiaryId is required for net banking");
    }

    @Test
    void validateMethodSpecificDetails_netBanking_cardTypePresent_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.NET_BANKING, UUID.randomUUID(),
                CardType.CREDIT_CARD, null, null, null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("card fields are not allowed for net banking");
    }

    @Test
    void validateMethodSpecificDetails_netBanking_upiIdPresent_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.NET_BANKING, UUID.randomUUID(),
                null, null, null, null, null, null, "user@upi"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("card fields are not allowed for net banking");
    }

    // =========================================================================
    // validateMethodSpecificDetails – UPI
    // =========================================================================

    @ParameterizedTest(name = "valid UPI ID: ''{0}''")
    @ValueSource(strings = {"user@upi", "john.doe@okaxis", "test_123@ybl", "ab@cd"})
    void validateMethodSpecificDetails_upi_validUpiIds_doesNotThrow(String upiId) {
        assertThatCode(() -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, null,
                null, null, null, null, null, null, upiId))
                .doesNotThrowAnyException();
    }

    @Test
    void validateMethodSpecificDetails_upi_beneficiaryIdPresent_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, UUID.randomUUID(),
                null, null, null, null, null, null, "user@upi"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("beneficiaryId must not be supplied for UPI");
    }

    @Test
    void validateMethodSpecificDetails_upi_cardTypePresent_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, null,
                CardType.DEBIT_CARD, null, null, null, null, null, "user@upi"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("card fields are not allowed for UPI");
    }

    @ParameterizedTest(name = "invalid UPI ID: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "noemail", "@nodomain", "a@b", "user@", "invalid upi@bank"})
    void validateMethodSpecificDetails_upi_invalidUpiId_throwsBadRequest(String upiId) {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.UPI, null,
                null, null, null, null, null, null, upiId))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_UPI"));
    }

    // =========================================================================
    // validateMethodSpecificDetails – CARD
    // =========================================================================

    private String futureYear() {
        return String.valueOf(YearMonth.now().getYear() + 2);
    }

    @Test
    void validateMethodSpecificDetails_card_valid_doesNotThrow() {
        // 4532015112830366 is a Luhn-valid Visa test number
        assertThatCode(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                CardType.CREDIT_CARD, "John Doe", "4532015112830366",
                "12", futureYear(), "123", null))
                .doesNotThrowAnyException();
    }

    @Test
    void validateMethodSpecificDetails_card_beneficiaryIdPresent_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, UUID.randomUUID(),
                CardType.CREDIT_CARD, "John", "4532015112830366",
                "12", futureYear(), "123", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("beneficiaryId must not be supplied for card");
    }

    @Test
    void validateMethodSpecificDetails_card_nullCardType_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                null, "John", "4532015112830366",
                "12", futureYear(), "123", null))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_CARD"))
                .hasMessageContaining("Card type is required");
    }

    @ParameterizedTest(name = "cardHolderName=''{0}'' is blank/null → invalid")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void validateMethodSpecificDetails_card_blankHolderName_throwsBadRequest(String name) {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                CardType.CREDIT_CARD, name, "4532015112830366",
                "12", futureYear(), "123", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cardholder name is required");
    }

    @ParameterizedTest(name = "card number ''{0}'' is invalid")
    @NullAndEmptySource
    @ValueSource(strings = {"123456789012", "12345678901234567890", "4111111111111112"})
    void validateMethodSpecificDetails_card_invalidCardNumber_throwsBadRequest(String cardNumber) {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                CardType.CREDIT_CARD, "John", cardNumber,
                "12", futureYear(), "123", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Card number is invalid");
    }

    @ParameterizedTest(name = "non-numeric expiry month=''{0}'' year=''{1}''")
    @CsvSource({"abc, 2030", "12, xyz", "' ', 2030"})
    void validateMethodSpecificDetails_card_nonNumericExpiry_throwsBadRequest(String month, String year) {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                CardType.CREDIT_CARD, "John", "4532015112830366",
                month, year, "123", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expiry is invalid");
    }

    @ParameterizedTest(name = "invalid expiry month: {0}")
    @ValueSource(ints = {0, 13})
    void validateMethodSpecificDetails_card_invalidExpiryMonth_throwsBadRequest(int month) {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                CardType.CREDIT_CARD, "John", "4532015112830366",
                String.valueOf(month), futureYear(), "123", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Expiry month");
    }

    @Test
    void validateMethodSpecificDetails_card_expiredCard_throwsBadRequest() {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                CardType.CREDIT_CARD, "John", "4532015112830366",
                "01", "2020", "123", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @ParameterizedTest(name = "invalid CVV: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"12", "12345", "ab3", "   "})
    void validateMethodSpecificDetails_card_invalidCvv_throwsBadRequest(String cvv) {
        assertThatThrownBy(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                CardType.CREDIT_CARD, "John", "4532015112830366",
                "12", futureYear(), cvv, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CVV must be 3 or 4 digits");
    }

    @ParameterizedTest(name = "valid CVV: ''{0}''")
    @ValueSource(strings = {"123", "1234"})
    void validateMethodSpecificDetails_card_validCvv_doesNotThrow(String cvv) {
        assertThatCode(() -> service.validateMethodSpecificDetails(
                PaymentMethod.CARD, null,
                CardType.CREDIT_CARD, "John", "4532015112830366",
                "12", futureYear(), cvv, null))
                .doesNotThrowAnyException();
    }

    // =========================================================================
    // normalizeCardNumber
    // =========================================================================

    @ParameterizedTest(name = "''{0}'' → ''{1}''")
    @CsvSource({
        "4111111111111111, 4111111111111111",
        "'4111 1111 1111 1111', 4111111111111111",
        "'4111-1111-1111-1111', 4111111111111111",
        "'', ''",
    })
    void normalizeCardNumber_stripsNonDigits(String input, String expected) {
        assertThat(service.normalizeCardNumber(input)).isEqualTo(expected);
    }

    @Test
    void normalizeCardNumber_null_returnsEmptyString() {
        assertThat(service.normalizeCardNumber(null)).isEmpty();
    }

    // =========================================================================
    // validateStatusTransition
    // =========================================================================

    @ParameterizedTest(name = "{0} → {1} is valid")
    @CsvSource({
        "CREATED,   VALIDATED",
        "CREATED,   FAILED",
        "VALIDATED, SENT",
        "VALIDATED, FAILED",
        "SENT,      COMPLETED",
        "SENT,      FAILED",
    })
    void validateStatusTransition_validTransitions_doesNotThrow(PaymentStatus from, PaymentStatus to) {
        assertThatCode(() -> service.validateStatusTransition(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} → {1} is invalid")
    @CsvSource({
        "CREATED,   SENT",
        "CREATED,   COMPLETED",
        "VALIDATED, CREATED",
        "VALIDATED, COMPLETED",
        "SENT,      CREATED",
        "SENT,      VALIDATED",
        "COMPLETED, CREATED",
        "COMPLETED, VALIDATED",
        "COMPLETED, SENT",
        "COMPLETED, FAILED",
        "FAILED,    CREATED",
        "FAILED,    VALIDATED",
        "FAILED,    SENT",
        "FAILED,    COMPLETED",
    })
    void validateStatusTransition_invalidTransitions_throwsBadRequest(PaymentStatus from, PaymentStatus to) {
        assertThatThrownBy(() -> service.validateStatusTransition(from, to))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }
}
