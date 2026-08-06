package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.model.Beneficiary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryRepositoryImplTest {

    @Mock private JdbcTemplate jdbc;
    @InjectMocks private BeneficiaryRepositoryImpl repository;

    private String beneficiaryId;
    private Beneficiary beneficiary;

    @BeforeEach
    void setUp() {
        beneficiaryId = com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId();
        beneficiary = new Beneficiary();
        beneficiary.setBeneficiaryId(beneficiaryId);
        beneficiary.setName("John Doe");
        beneficiary.setAccountNumber("ACC123456");
        beneficiary.setBankName("HDFC");
        beneficiary.setIfscCode("HDFC0001234");
        beneficiary.setEmail("john@example.com");
        beneficiary.setPhone("9876543210");
    }

    // -------------------------------------------------------------------------
    // save – INSERT
    // -------------------------------------------------------------------------

    @Test
    void save_newBeneficiary_assignsUuidAndExecutesInsert() {
        Beneficiary newBene = new Beneficiary();
        newBene.setName("Alice");
        newBene.setAccountNumber("ACCNEW");
        newBene.setBankName("SBI");
        newBene.setIfscCode("SBIN0000001");
        newBene.setEmail("alice@example.com");

        repository.save(newBene);

        assertThat(newBene.getBeneficiaryId()).isNotNull();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).containsIgnoringCase("INSERT INTO beneficiaries");
    }

    @Test
    void save_newBeneficiary_returnsSameReference() {
        Beneficiary newBene = new Beneficiary();
        newBene.setName("Alice");
        newBene.setAccountNumber("ACCNEW");
        newBene.setBankName("SBI");
        newBene.setIfscCode("SBIN0000001");
        newBene.setEmail("alice@example.com");

        Beneficiary result = repository.save(newBene);

        assertThat(result).isSameAs(newBene);
    }

    // -------------------------------------------------------------------------
    // save – UPDATE
    // -------------------------------------------------------------------------

    @Test
    void save_existingBeneficiary_executesUpdateSql() {
        repository.save(beneficiary);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).containsIgnoringCase("UPDATE beneficiaries");
    }

    @Test
    void save_existingBeneficiary_idUsedInWhereClause() {
        repository.save(beneficiary);

        ArgumentCaptor<Object> lastArg = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any(), lastArg.capture());
        assertThat(lastArg.getValue()).isEqualTo(beneficiaryId.toString());
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void findById_found_returnsOptionalWithBeneficiary() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(beneficiary));

        Optional<Beneficiary> result = repository.findById(beneficiaryId);

        assertThat(result).isPresent().contains(beneficiary);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findById_notFound_returnsEmpty() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        assertThat(repository.findById(com.finance.PaymentProcessing.util.IdGenerator.generate9DigitId())).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findById_passesIdAsString() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(beneficiary));

        repository.findById(beneficiaryId);

        verify(jdbc).query(anyString(), any(RowMapper.class), eq(beneficiaryId.toString()));
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void findAll_returnsAllBeneficiaries() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(beneficiary));

        assertThat(repository.findAll()).containsExactly(beneficiary);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findAll_emptyTable_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        assertThat(repository.findAll()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // existsById
    // -------------------------------------------------------------------------

    @Test
    void existsById_countOne_returnsTrue() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);
        assertThat(repository.existsById(beneficiaryId)).isTrue();
    }

    @Test
    void existsById_countZero_returnsFalse() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        assertThat(repository.existsById(beneficiaryId)).isFalse();
    }

    @Test
    void existsById_nullCount_returnsFalse() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(null);
        assertThat(repository.existsById(beneficiaryId)).isFalse();
    }

    // -------------------------------------------------------------------------
    // findByAccountNumber
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    void findByAccountNumber_found_returnsOptional() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("ACC123456")))
                .thenReturn(List.of(beneficiary));

        Optional<Beneficiary> result = repository.findByAccountNumber("ACC123456");

        assertThat(result).isPresent().contains(beneficiary);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByAccountNumber_notFound_returnsEmpty() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        assertThat(repository.findByAccountNumber("UNKNOWN")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findByAccountNumber_passesAccountNumberAsArgument() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        repository.findByAccountNumber("ACC999");

        verify(jdbc).query(anyString(), any(RowMapper.class), eq("ACC999"));
    }
}
