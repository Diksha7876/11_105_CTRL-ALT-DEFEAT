package com.finance.PaymentProcessing.service;

import com.finance.PaymentProcessing.dto.BeneficiaryRequest;
import com.finance.PaymentProcessing.dto.BeneficiaryResponse;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.exception.NotFoundException;
import com.finance.PaymentProcessing.model.Beneficiary;
import com.finance.PaymentProcessing.repository.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository repository;

    @InjectMocks
    private BeneficiaryService service;

    private UUID beneficiaryId;
    private Beneficiary savedBeneficiary;
    private BeneficiaryRequest validRequest;

    @BeforeEach
    void setUp() {
        beneficiaryId = UUID.randomUUID();

        validRequest = new BeneficiaryRequest(
                "John Doe",
                "ACC123456",
                "HDFC Bank",
                "HDFC0001234",
                "john.doe@example.com",
                "9876543210"
        );

        savedBeneficiary = new Beneficiary();
        savedBeneficiary.setBeneficiaryId(beneficiaryId);
        savedBeneficiary.setName("John Doe");
        savedBeneficiary.setAccountNumber("ACC123456");
        savedBeneficiary.setBankName("HDFC Bank");
        savedBeneficiary.setIfscCode("HDFC0001234");
        savedBeneficiary.setEmail("john.doe@example.com");
        savedBeneficiary.setPhone("9876543210");
    }

    // -------------------------------------------------------------------------
    // addBeneficiary
    // -------------------------------------------------------------------------

    @Test
    void addBeneficiary_success_returnsMappedResponse() {
        when(repository.findByAccountNumber("ACC123456")).thenReturn(Optional.empty());
        when(repository.save(any(Beneficiary.class))).thenReturn(savedBeneficiary);

        BeneficiaryResponse response = service.addBeneficiary(validRequest);

        assertThat(response.beneficiaryId()).isEqualTo(beneficiaryId);
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.accountNumber()).isEqualTo("ACC123456");
        assertThat(response.bankName()).isEqualTo("HDFC Bank");
        assertThat(response.ifscCode()).isEqualTo("HDFC0001234");
        assertThat(response.email()).isEqualTo("john.doe@example.com");
        assertThat(response.phone()).isEqualTo("9876543210");

        verify(repository).findByAccountNumber("ACC123456");
        verify(repository).save(any(Beneficiary.class));
    }

    @Test
    void addBeneficiary_ifscCodeStoredAsUpperCase() {
        BeneficiaryRequest lowerCaseIfsc = new BeneficiaryRequest(
                "Jane Smith", "ACC654321", "SBI", "sbin0005678",
                "jane@example.com", null
        );

        Beneficiary persisted = new Beneficiary();
        persisted.setBeneficiaryId(UUID.randomUUID());
        persisted.setName("Jane Smith");
        persisted.setAccountNumber("ACC654321");
        persisted.setBankName("SBI");
        persisted.setIfscCode("SBIN0005678");
        persisted.setEmail("jane@example.com");
        persisted.setPhone(null);

        when(repository.findByAccountNumber("ACC654321")).thenReturn(Optional.empty());
        when(repository.save(any(Beneficiary.class))).thenAnswer(inv -> {
            Beneficiary b = inv.getArgument(0);
            assertThat(b.getIfscCode()).isEqualTo("SBIN0005678");
            return persisted;
        });

        BeneficiaryResponse response = service.addBeneficiary(lowerCaseIfsc);

        assertThat(response.ifscCode()).isEqualTo("SBIN0005678");
    }

    @Test
    void addBeneficiary_duplicateAccountNumber_throwsBadRequestException() {
        when(repository.findByAccountNumber("ACC123456")).thenReturn(Optional.of(savedBeneficiary));

        assertThatThrownBy(() -> service.addBeneficiary(validRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // listBeneficiaries
    // -------------------------------------------------------------------------

    @Test
    void listBeneficiaries_returnsMappedList() {
        Beneficiary second = new Beneficiary();
        second.setBeneficiaryId(UUID.randomUUID());
        second.setName("Alice");
        second.setAccountNumber("ACC999888");
        second.setBankName("Axis");
        second.setIfscCode("UTIB0000123");
        second.setEmail("alice@example.com");
        second.setPhone(null);

        when(repository.findAll()).thenReturn(List.of(savedBeneficiary, second));

        List<BeneficiaryResponse> result = service.listBeneficiaries();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BeneficiaryResponse::name)
                .containsExactly("John Doe", "Alice");

        verify(repository).findAll();
    }

    @Test
    void listBeneficiaries_emptyRepository_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<BeneficiaryResponse> result = service.listBeneficiaries();

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getBeneficiary
    // -------------------------------------------------------------------------

    @Test
    void getBeneficiary_existingId_returnsMappedResponse() {
        when(repository.findById(beneficiaryId)).thenReturn(Optional.of(savedBeneficiary));

        BeneficiaryResponse response = service.getBeneficiary(beneficiaryId);

        assertThat(response.beneficiaryId()).isEqualTo(beneficiaryId);
        assertThat(response.name()).isEqualTo("John Doe");

        verify(repository).findById(beneficiaryId);
    }

    @Test
    void getBeneficiary_unknownId_throwsNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBeneficiary(unknownId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }
}
