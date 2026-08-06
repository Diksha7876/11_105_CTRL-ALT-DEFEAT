package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.model.BankAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private BankAccountRepositoryImpl repository;

    private UUID accountId;
    private BankAccount account;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        account = new BankAccount();
        account.setAccountId(accountId);
        account.setAccountNumber("ACC123456");
        account.setAccountHolderName("Alice");
        account.setPayerId(UUID.randomUUID());
        account.setAccountType("SAVINGS");
        account.setBalanceInInr(new BigDecimal("5000.00"));
        account.setActive(true);
    }

    // =========================================================================
    // save – INSERT (accountId is null)
    // =========================================================================

    @Test
    void save_newAccount_assignsUuidBeforeInsert() {
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber("ACCNEW");
        newAccount.setAccountHolderName("Bob");
        newAccount.setAccountType("CURRENT");
        newAccount.setBalanceInInr(BigDecimal.ZERO);
        // accountId is null → INSERT path

        BankAccount result = repository.save(newAccount);

        assertThat(result.getAccountId()).isNotNull();
    }

    @Test
    void save_newAccount_executesInsertSql() {
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber("ACCNEW");
        newAccount.setAccountHolderName("Bob");
        newAccount.setAccountType("SAVINGS");
        newAccount.setBalanceInInr(new BigDecimal("1000"));

        repository.save(newAccount);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).containsIgnoringCase("INSERT INTO bank_accounts");
    }

    @Test
    void save_newAccount_returnsAccountWithSameReference() {
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber("ACCNEW");
        newAccount.setAccountHolderName("Bob");
        newAccount.setAccountType("SAVINGS");
        newAccount.setBalanceInInr(BigDecimal.ZERO);

        BankAccount result = repository.save(newAccount);

        assertThat(result).isSameAs(newAccount);
    }

    @Test
    void save_newAccount_nullPayerId_passedAsNullToJdbc() {
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber("ACCNEW");
        newAccount.setAccountHolderName("Bob");
        newAccount.setAccountType("SAVINGS");
        newAccount.setBalanceInInr(BigDecimal.ZERO);
        // payerId is null

        repository.save(newAccount);

        // Verify update was called; null payerId should be passed as null (4th arg)
        ArgumentCaptor<Object> arg4 = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), any(), any(), any(), arg4.capture(), any(), any(), any());
        assertThat(arg4.getValue()).isNull();
    }

    @Test
    void save_newAccount_nullBalance_defaultsToZero() {
        BankAccount newAccount = new BankAccount();
        newAccount.setAccountNumber("ACCNEW");
        newAccount.setAccountHolderName("Bob");
        newAccount.setAccountType("SAVINGS");
        newAccount.setBalanceInInr(null);

        repository.save(newAccount);

        ArgumentCaptor<Object> balanceCaptor = ArgumentCaptor.forClass(Object.class);
        // balance is the 6th argument: id, number, holder, payerId, type, balance, active
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), balanceCaptor.capture(), any());
        assertThat(balanceCaptor.getValue()).isEqualTo(BigDecimal.ZERO);
    }

    // =========================================================================
    // save – UPDATE (accountId is set)
    // =========================================================================

    @Test
    void save_existingAccount_executesUpdateSql() {
        repository.save(account);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sqlCaptor.capture(), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).containsIgnoringCase("UPDATE bank_accounts");
    }

    @Test
    void save_existingAccount_returnsAccountWithOriginalId() {
        BankAccount result = repository.save(account);

        assertThat(result.getAccountId()).isEqualTo(accountId);
    }

    @Test
    void save_existingAccount_accountIdUsedAsWhereClause() {
        repository.save(account);

        // Last argument to UPDATE is the account_id for WHERE clause
        ArgumentCaptor<Object> lastArg = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any(), lastArg.capture());
        assertThat(lastArg.getValue()).isEqualTo(accountId.toString());
    }

    // =========================================================================
    // findById
    // =========================================================================

    @SuppressWarnings("unchecked")
    @Test
    void findById_found_returnsOptionalWithAccount() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(account));

        Optional<BankAccount> result = repository.findById(accountId);

        assertThat(result).isPresent().contains(account);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findById_notFound_returnsEmpty() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of());

        Optional<BankAccount> result = repository.findById(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findById_passesIdAsString() {
        when(jdbc.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(account));

        repository.findById(accountId);

        verify(jdbc).query(anyString(), any(RowMapper.class), eq(accountId.toString()));
    }

    // =========================================================================
    // findAll
    // =========================================================================

    @SuppressWarnings("unchecked")
    @Test
    void findAll_returnsAllAccounts() {
        BankAccount second = new BankAccount();
        second.setAccountId(UUID.randomUUID());
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(account, second));

        List<BankAccount> result = repository.findAll();

        assertThat(result).hasSize(2).containsExactly(account, second);
    }

    @SuppressWarnings("unchecked")
    @Test
    void findAll_empty_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<BankAccount> result = repository.findAll();

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // existsById
    // =========================================================================

    @Test
    void existsById_countOne_returnsTrue() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);

        assertThat(repository.existsById(accountId)).isTrue();
    }

    @Test
    void existsById_countZero_returnsFalse() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);

        assertThat(repository.existsById(accountId)).isFalse();
    }

    @Test
    void existsById_countNull_returnsFalse() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(null);

        assertThat(repository.existsById(accountId)).isFalse();
    }

    @Test
    void existsById_passesIdAsString() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(1);

        repository.existsById(accountId);

        verify(jdbc).queryForObject(anyString(), eq(Integer.class), eq(accountId.toString()));
    }
}
