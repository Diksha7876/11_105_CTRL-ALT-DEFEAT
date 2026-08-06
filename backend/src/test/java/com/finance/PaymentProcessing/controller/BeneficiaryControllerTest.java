package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.BeneficiaryRequest;
import com.finance.PaymentProcessing.dto.BeneficiaryResponse;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.service.BeneficiaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryControllerTest {

    @Mock
    private BeneficiaryService service;

    @InjectMocks
    private BeneficiaryController controller;

    private String beneficiaryId;
    private BeneficiaryResponse sampleResponse;
    private BeneficiaryRequest sampleRequest;

    @BeforeEach
    void setUp() {
        beneficiaryId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        sampleResponse = new BeneficiaryResponse(
                beneficiaryId, "John Doe", "ACC123456",
                "HDFC Bank", "HDFC0001234", "john@example.com", "9876543210");
        sampleRequest = new BeneficiaryRequest(
                "John Doe", "ACC123456", "HDFC Bank",
                "HDFC0001234", "john@example.com", "9876543210");
    }

    // =========================================================================
    // addBeneficiary
    // =========================================================================

    @Test
    void addBeneficiary_success_returns201WithLocationHeader() {
        when(service.addBeneficiary(sampleRequest)).thenReturn(sampleResponse);

        ResponseEntity<BeneficiaryResponse> response = controller.addBeneficiary(sampleRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/beneficiaries/" + beneficiaryId);
    }

    @Test
    void addBeneficiary_success_returnsCreatedResponseInBody() {
        when(service.addBeneficiary(sampleRequest)).thenReturn(sampleResponse);

        ResponseEntity<BeneficiaryResponse> response = controller.addBeneficiary(sampleRequest);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(sampleResponse);
        verify(service).addBeneficiary(sampleRequest);
    }

    @Test
    void addBeneficiary_duplicateAccountNumber_propagatesException() {
        when(service.addBeneficiary(sampleRequest))
                .thenThrow(new BadRequestException("A beneficiary with this account number already exists"));

        assertThatThrownBy(() -> controller.addBeneficiary(sampleRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    // =========================================================================
    // listBeneficiaries
    // =========================================================================

    @Test
    void listBeneficiaries_delegatesToService_returnsResults() {
        BeneficiaryResponse second = new BeneficiaryResponse(
                com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId(), "Jane Smith", "ACC999888",
                "SBI", "SBIN0005678", "jane@example.com", null);
        when(service.listBeneficiaries()).thenReturn(List.of(sampleResponse, second));

        List<BeneficiaryResponse> result = controller.listBeneficiaries();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(sampleResponse, second);
        verify(service).listBeneficiaries();
    }

    @Test
    void listBeneficiaries_empty_returnsEmptyList() {
        when(service.listBeneficiaries()).thenReturn(List.of());

        List<BeneficiaryResponse> result = controller.listBeneficiaries();

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // getBeneficiary
    // =========================================================================

    @Test
    void getBeneficiary_existingId_returnsResponse() {
        when(service.getBeneficiary(beneficiaryId)).thenReturn(sampleResponse);

        BeneficiaryResponse result = controller.getBeneficiary(beneficiaryId);

        assertThat(result).isEqualTo(sampleResponse);
        verify(service).getBeneficiary(beneficiaryId);
    }

    @Test
    void getBeneficiary_unknownId_propagatesNotFoundException() {
        String unknownId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        when(service.getBeneficiary(unknownId))
                .thenThrow(new NotFoundException("Beneficiary not found: " + unknownId));

        assertThatThrownBy(() -> controller.getBeneficiary(unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }
}
