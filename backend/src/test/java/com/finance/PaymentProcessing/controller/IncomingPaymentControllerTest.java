package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.CurrentUserResponse;
import com.finance.PaymentProcessing.dto.IncomingPaymentRequest;
import com.finance.PaymentProcessing.dto.IncomingPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class IncomingPaymentControllerTest {

    private static final String DEFAULT_PAYER_ID = "111111111";

    private final IncomingPaymentController controller = new IncomingPaymentController();

    @BeforeEach
    void clearStore() {
        // STORE is a static CopyOnWriteArrayList — clear it before each test
        ((List<?>) ReflectionTestUtils.getField(IncomingPaymentController.class, "STORE")).clear();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private IncomingPaymentRequest request(BigDecimal amount, String currency, String reference,
            String sourceName, String destAccountId, Instant receivedAt) {
        return new IncomingPaymentRequest(amount, currency, reference, sourceName, destAccountId, receivedAt);
    }

    private IncomingPaymentRequest minimalRequest() {
        return request(new BigDecimal("100.00"), "INR", "REF-001", "Acme Corp", null, null);
    }

    // =========================================================================
    // getCurrentUser
    // =========================================================================

    @Test
    void getCurrentUser_returnsDefaultPayerId() {
        CurrentUserResponse response = controller.getCurrentUser();

        assertThat(response).isNotNull();
        assertThat(response.payerId()).isEqualTo(DEFAULT_PAYER_ID);
    }

    // =========================================================================
    // listIncomingPayments
    // =========================================================================

    @Test
    void listIncomingPayments_emptyStore_returnsEmptyList() {
        List<IncomingPaymentResponse> result = controller.listIncomingPayments();

        assertThat(result).isEmpty();
    }

    @Test
    void listIncomingPayments_afterCreate_returnsSavedPayments() {
        controller.createIncomingPayment(minimalRequest());
        controller.createIncomingPayment(
                request(new BigDecimal("200"), "USD", "REF-002", "Beta Ltd", null, null));

        List<IncomingPaymentResponse> result = controller.listIncomingPayments();

        assertThat(result).hasSize(2);
    }

    @Test
    void listIncomingPayments_returnsDefensiveCopy() {
        controller.createIncomingPayment(minimalRequest());

        List<IncomingPaymentResponse> result = controller.listIncomingPayments();

        // Mutating the returned list must not affect the store
        assertThatCode(() -> result.clear()).doesNotThrowAnyException();
        assertThat(controller.listIncomingPayments()).hasSize(1);
    }

    // =========================================================================
    // createIncomingPayment – HTTP contract
    // =========================================================================

    @Test
    void createIncomingPayment_returns201Created() {
        ResponseEntity<IncomingPaymentResponse> response = controller.createIncomingPayment(minimalRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createIncomingPayment_locationHeaderPointsToNewResource() {
        ResponseEntity<IncomingPaymentResponse> response = controller.createIncomingPayment(minimalRequest());

        String id = response.getBody().incomingPaymentId();
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/incoming-payments/" + id);
    }

    // =========================================================================
    // createIncomingPayment – response body fields
    // =========================================================================

    @Test
    void createIncomingPayment_payerIdIsAlwaysDefault() {
        IncomingPaymentResponse body = controller.createIncomingPayment(minimalRequest()).getBody();

        assertThat(body.payerId()).isEqualTo(DEFAULT_PAYER_ID);
    }

    @Test
    void createIncomingPayment_generatesUniqueIds() {
        String id1 = controller.createIncomingPayment(minimalRequest()).getBody().incomingPaymentId();
        String id2 = controller.createIncomingPayment(minimalRequest()).getBody().incomingPaymentId();

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void createIncomingPayment_amountTrailingZerosStripped() {
        IncomingPaymentRequest req = request(
                new BigDecimal("100.50000"), "INR", "REF", "Src", null, null);

        IncomingPaymentResponse body = controller.createIncomingPayment(req).getBody();

        assertThat(body.amount()).isEqualByComparingTo("100.5");
        assertThat(body.amount().toPlainString()).doesNotEndWith("0");
    }

    @Test
    void createIncomingPayment_currencyTrimmed() {
        IncomingPaymentRequest req = request(
                new BigDecimal("50"), "  USD  ", "REF", "Src", null, null);

        IncomingPaymentResponse body = controller.createIncomingPayment(req).getBody();

        assertThat(body.currency()).isEqualTo("USD");
    }

    @Test
    void createIncomingPayment_referenceTrimmed() {
        IncomingPaymentRequest req = request(
                new BigDecimal("50"), "INR", "  REF-TRIM  ", "Src", null, null);

        IncomingPaymentResponse body = controller.createIncomingPayment(req).getBody();

        assertThat(body.reference()).isEqualTo("REF-TRIM");
    }

    @Test
    void createIncomingPayment_sourceNameTrimmed() {
        IncomingPaymentRequest req = request(
                new BigDecimal("50"), "INR", "REF", "  Acme Corp  ", null, null);

        IncomingPaymentResponse body = controller.createIncomingPayment(req).getBody();

        assertThat(body.sourceName()).isEqualTo("Acme Corp");
    }

    @Test
    void createIncomingPayment_destinationAccountIdPreserved() {
        String destId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        IncomingPaymentRequest req = request(new BigDecimal("50"), "INR", "REF", "Src", destId, null);

        IncomingPaymentResponse body = controller.createIncomingPayment(req).getBody();

        assertThat(body.destinationAccountId()).isEqualTo(destId);
    }

    @Test
    void createIncomingPayment_nullDestinationAccountId_storedAsNull() {
        IncomingPaymentResponse body = controller.createIncomingPayment(minimalRequest()).getBody();

        assertThat(body.destinationAccountId()).isNull();
    }

    @Test
    void createIncomingPayment_explicitReceivedAt_usedAsIs() {
        Instant receivedAt = Instant.parse("2024-06-15T10:00:00Z");
        IncomingPaymentRequest req = request(new BigDecimal("50"), "INR", "REF", "Src", null, receivedAt);

        IncomingPaymentResponse body = controller.createIncomingPayment(req).getBody();

        assertThat(body.receivedAt()).isEqualTo(receivedAt);
    }

    @Test
    void createIncomingPayment_nullReceivedAt_defaultsToNow() {
        Instant before = Instant.now().minusSeconds(1);

        IncomingPaymentResponse body = controller.createIncomingPayment(minimalRequest()).getBody();

        assertThat(body.receivedAt()).isAfter(before);
    }

    @Test
    void createIncomingPayment_createdAtAndUpdatedAtPopulated() {
        Instant before = Instant.now().minusSeconds(1);

        IncomingPaymentResponse body = controller.createIncomingPayment(minimalRequest()).getBody();

        assertThat(body.createdAt()).isAfter(before);
        assertThat(body.updatedAt()).isAfter(before);
    }

    // =========================================================================
    // createIncomingPayment – store persistence
    // =========================================================================

    @Test
    void createIncomingPayment_paymentPersistedInStore() {
        IncomingPaymentResponse created = controller.createIncomingPayment(minimalRequest()).getBody();

        List<IncomingPaymentResponse> stored = controller.listIncomingPayments();

        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).incomingPaymentId()).isEqualTo(created.incomingPaymentId());
    }

    @Test
    void createIncomingPayment_multiplePayments_allPersistedInOrder() {
        controller.createIncomingPayment(
                request(new BigDecimal("10"), "INR", "R1", "S1", null, null));
        controller.createIncomingPayment(
                request(new BigDecimal("20"), "USD", "R2", "S2", null, null));
        controller.createIncomingPayment(
                request(new BigDecimal("30"), "EUR", "R3", "S3", null, null));

        List<IncomingPaymentResponse> all = controller.listIncomingPayments();

        assertThat(all).hasSize(3);
        assertThat(all).extracting(IncomingPaymentResponse::reference)
                .containsExactly("R1", "R2", "R3");
    }
}
