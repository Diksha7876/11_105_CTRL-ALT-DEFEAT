package com.finance.PaymentProcessing.controller;

import com.finance.PaymentProcessing.dto.BankAccountRequest;
import com.finance.PaymentProcessing.dto.BankAccountResponse;
import com.finance.PaymentProcessing.exception.BadRequestException;
import com.finance.PaymentProcessing.model.BankAccount;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountControllerTest {

    private static final UUID DEFAULT_PAYER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private BankAccountRepository repository;

    @InjectMocks
    private BankAccountController controller;

    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BankAccount savedAccount(UUID id, String accountNumber, String holderName,
            UUID payerId, String accountType, BigDecimal balance) {
        BankAccount a = new BankAccount();
        a.setAccountId(id);
        a.setAccountNumber(accountNumber);
        a.setAccountHolderName(holderName);
        a.setPayerId(payerId);
        a.setAccountType(accountType);
        a.setBalanceInInr(balance);
        a.setActive(true);
        return a;
    }

    private BankAccountRequest request(String accountNumber, String holderName,
            UUID payerId, BigDecimal openingBalance, String accountType) {
        return new BankAccountRequest(accountNumber, holderName, payerId, openingBalance, null, accountType);
    }

    // =========================================================================
    // create – success paths
    // =========================================================================

    @Test
    void create_minimal_request_returns201WithLocation() {
        BankAccountRequest req = request("ACC123456", "Alice", null, null, null);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId(accountId);
            return a;
        });

        ResponseEntity<BankAccountResponse> response = controller.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/accounts/" + accountId);
    }

    @Test
    void create_minimal_request_defaultsPayerIdAndBalance() {
        BankAccountRequest req = request("ACC123456", "Alice", null, null, null);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId(accountId);
            return a;
        });

        ResponseEntity<BankAccountResponse> response = controller.create(req);

        BankAccountResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.payerId()).isEqualTo(DEFAULT_PAYER_ID);
        assertThat(body.balanceInInr()).isEqualByComparingTo("50000.00");
    }

    @Test
    void create_minimal_request_defaultsAccountTypeToSavings() {
        BankAccountRequest req = request("ACC123456", "Alice", null, null, null);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId(accountId);
            return a;
        });

        controller.create(req);

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAccountType()).isEqualTo("SAVINGS");
    }

    @Test
    void create_explicit_payerId_balance_accountType_storedCorrectly() {
        UUID payerId = UUID.randomUUID();
        BankAccountRequest req = request("ACC999888", "Bob", payerId, new BigDecimal("10000.00"), "CURRENT");
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId(accountId);
            return a;
        });

        ResponseEntity<BankAccountResponse> response = controller.create(req);

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(repository).save(captor.capture());
        BankAccount saved = captor.getValue();
        assertThat(saved.getPayerId()).isEqualTo(payerId);
        assertThat(saved.getBalanceInInr()).isEqualByComparingTo("10000.00");
        assertThat(saved.getAccountType()).isEqualTo("CURRENT");
        assertThat(response.getBody().accountHolderName()).isEqualTo("Bob");
    }

    @ParameterizedTest(name = "accountType ''{0}'' stored as uppercase")
    @ValueSource(strings = {"savings", "Savings", "SAVINGS", "current", "CURRENT", "salary", "SALARY"})
    void create_accountType_normalizedToUpperCase(String accountType) {
        BankAccountRequest req = request("ACC123456", "Carol", null, null, accountType);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId(accountId);
            return a;
        });

        controller.create(req);

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAccountType()).isIn("SAVINGS", "CURRENT", "SALARY");
    }

    @Test
    void create_responseBodyMapsAllFields() {
        UUID payerId = UUID.randomUUID();
        BankAccountRequest req = request("ACCSALARY1", "Dave", payerId, new BigDecimal("5000"), "SALARY");
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId(accountId);
            return a;
        });

        BankAccountResponse body = controller.create(req).getBody();

        assertThat(body).isNotNull();
        assertThat(body.accountId()).isEqualTo(accountId);
        assertThat(body.accountNumber()).isEqualTo("ACCSALARY1");
        assertThat(body.accountHolderName()).isEqualTo("Dave");
        assertThat(body.payerId()).isEqualTo(payerId);
        assertThat(body.accountType()).isEqualTo("SALARY");
        assertThat(body.balanceInInr()).isEqualByComparingTo("5000");
        assertThat(body.active()).isTrue();
    }

    // =========================================================================
    // create – error paths
    // =========================================================================

    @Test
    void create_duplicateAccountNumber_throwsBadRequestException() {
        BankAccount existing = savedAccount(UUID.randomUUID(), "ACC123456", "Eve",
                UUID.randomUUID(), "SAVINGS", new BigDecimal("1000"));
        when(repository.findAll()).thenReturn(List.of(existing));

        BankAccountRequest req = request("ACC123456", "Frank", null, null, null);

        assertThatThrownBy(() -> controller.create(req))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_ACCOUNT"))
                .hasMessageContaining("already exists");
        verify(repository, never()).save(any());
    }

    @Test
    void create_negativeOpeningBalance_throwsBadRequestException() {
        when(repository.findAll()).thenReturn(List.of());

        BankAccountRequest req = request("ACC123456", "Grace", null, new BigDecimal("-0.01"), null);

        assertThatThrownBy(() -> controller.create(req))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_BALANCE"))
                .hasMessageContaining("negative");
        verify(repository, never()).save(any());
    }

    @Test
    void create_zeroOpeningBalance_isAllowed() {
        BankAccountRequest req = request("ACC123456", "Henry", null, BigDecimal.ZERO, null);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId(accountId);
            return a;
        });

        ResponseEntity<BankAccountResponse> response = controller.create(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().balanceInInr()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest(name = "unsupported accountType ''{0}'' throws")
    @ValueSource(strings = {"CHECKING", "FIXED_DEPOSIT", "LOAN", "unknown"})
    void create_unsupportedAccountType_throwsBadRequestException(String accountType) {
        when(repository.findAll()).thenReturn(List.of());

        BankAccountRequest req = request("ACC123456", "Ivan", null, null, accountType);

        assertThatThrownBy(() -> controller.create(req))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_ACCOUNT_TYPE"))
                .hasMessageContaining("SAVINGS, CURRENT, SALARY");
        verify(repository, never()).save(any());
    }

    @Test
    void create_nullAccountType_defaultsToSavings() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setAccountId(accountId);
            return a;
        });

        controller.create(request("ACC123456", "Judy", null, null, null));

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAccountType()).isEqualTo("SAVINGS");
    }

    @ParameterizedTest(name = "blank accountType ''{0}'' treated as unsupported → throws")
    @ValueSource(strings = {"", "   "})
    void create_blankAccountType_throwsBadRequestException(String accountType) {
        when(repository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> controller.create(request("ACC123456", "Judy", null, null, accountType)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getErrorCode())
                        .isEqualTo("INVALID_ACCOUNT_TYPE"));
        verify(repository, never()).save(any());
    }

    // =========================================================================
    // list
    // =========================================================================

    @Test
    void list_emptyRepository_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<BankAccountResponse> result = controller.list();

        assertThat(result).isEmpty();
    }

    @Test
    void list_multipleAccounts_returnsMappedResponses() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        BankAccount a1 = savedAccount(id1, "ACC001", "Karl", UUID.randomUUID(), "SAVINGS", new BigDecimal("1000"));
        BankAccount a2 = savedAccount(id2, "ACC002", "Lena", UUID.randomUUID(), "CURRENT", new BigDecimal("2000"));
        when(repository.findAll()).thenReturn(List.of(a1, a2));

        List<BankAccountResponse> result = controller.list();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(BankAccountResponse::accountNumber)
                .containsExactly("ACC001", "ACC002");
        assertThat(result).extracting(BankAccountResponse::accountType)
                .containsExactly("SAVINGS", "CURRENT");
    }

    @Test
    void list_responseFieldsMappedCorrectly() {
        UUID payerId = UUID.randomUUID();
        BankAccount account = savedAccount(accountId, "ACCTEST", "Mike", payerId, "SALARY", new BigDecimal("9999"));
        when(repository.findAll()).thenReturn(List.of(account));

        BankAccountResponse response = controller.list().get(0);

        assertThat(response.accountId()).isEqualTo(accountId);
        assertThat(response.accountNumber()).isEqualTo("ACCTEST");
        assertThat(response.accountHolderName()).isEqualTo("Mike");
        assertThat(response.payerId()).isEqualTo(payerId);
        assertThat(response.accountType()).isEqualTo("SALARY");
        assertThat(response.balanceInInr()).isEqualByComparingTo("9999");
        assertThat(response.active()).isTrue();
    }
}
