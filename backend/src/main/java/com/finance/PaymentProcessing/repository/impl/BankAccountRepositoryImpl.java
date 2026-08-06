package com.finance.PaymentProcessing.repository.impl;

import com.finance.PaymentProcessing.model.BankAccount;
import com.finance.PaymentProcessing.repository.BankAccountRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BankAccountRepositoryImpl implements BankAccountRepository {

    private final JdbcTemplate jdbc;

    public BankAccountRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // RowMapper: converts a MySQL result row → BankAccount POJO
    private static final RowMapper<BankAccount> ROW_MAPPER = (rs, rowNum) -> map(rs);

    private static BankAccount map(ResultSet rs) throws SQLException {
        BankAccount a = new BankAccount();
        a.setAccountId(UUID.fromString(rs.getString("account_id")));
        a.setAccountNumber(rs.getString("account_number"));
        a.setAccountHolderName(rs.getString("account_holder_name"));
        String payerId = rs.getString("payer_id");
        a.setPayerId(payerId != null ? UUID.fromString(payerId) : null);
        a.setAccountType(rs.getString("account_type"));
        BigDecimal balanceInInr = rs.getBigDecimal("balance_in_inr");
        a.setBalanceInInr(balanceInInr != null ? balanceInInr : BigDecimal.ZERO);
        BigDecimal maxTransactionLimit = rs.getBigDecimal("max_transaction_limit_in_inr");
        a.setMaxTransactionLimitInInr(maxTransactionLimit != null ? maxTransactionLimit : new BigDecimal("1000000.00"));
        a.setActive(rs.getBoolean("active"));
        return a;
    }

    @Override
    public BankAccount save(BankAccount account) {
        if (account.getAccountId() == null) {
            // INSERT: generate UUID here so MySQL receives it as a CHAR(36)
            account.setAccountId(UUID.randomUUID());
            jdbc.update(
                "INSERT INTO bank_accounts (account_id, account_number, account_holder_name, payer_id, account_type, balance_in_inr, max_transaction_limit_in_inr, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                account.getAccountId().toString(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getPayerId() != null ? account.getPayerId().toString() : null,
                account.getAccountType(),
                account.getBalanceInInr() != null ? account.getBalanceInInr() : BigDecimal.ZERO,
                account.getMaxTransactionLimitInInr() != null ? account.getMaxTransactionLimitInInr() : new BigDecimal("1000000.00"),
                account.isActive()
            );
        } else {
            // UPDATE
            jdbc.update(
                "UPDATE bank_accounts SET account_number = ?, account_holder_name = ?, payer_id = ?, account_type = ?, balance_in_inr = ?, max_transaction_limit_in_inr = ?, active = ? WHERE account_id = ?",
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getPayerId() != null ? account.getPayerId().toString() : null,
                account.getAccountType(),
                account.getBalanceInInr() != null ? account.getBalanceInInr() : BigDecimal.ZERO,
                account.getMaxTransactionLimitInInr() != null ? account.getMaxTransactionLimitInInr() : new BigDecimal("1000000.00"),
                account.isActive(),
                account.getAccountId().toString()
            );
        }
        return account;
    }

    @Override
    public Optional<BankAccount> findById(UUID id) {
        List<BankAccount> results = jdbc.query(
            "SELECT * FROM bank_accounts WHERE account_id = ?",
            ROW_MAPPER,
            id.toString()
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<BankAccount> findAll() {
        return jdbc.query("SELECT * FROM bank_accounts", ROW_MAPPER);
    }

    @Override
    public boolean existsById(UUID id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM bank_accounts WHERE account_id = ?",
            Integer.class,
            id.toString()
        );
        return count != null && count > 0;
    }
}