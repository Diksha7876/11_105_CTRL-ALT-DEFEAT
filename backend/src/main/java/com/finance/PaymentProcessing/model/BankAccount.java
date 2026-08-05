package com.finance.PaymentProcessing.model;

import java.math.BigDecimal;
import java.util.UUID;

public class BankAccount {
    private UUID accountId;
    private String accountNumber;
    private String accountHolderName;
    private UUID payerId;
    private String accountType = "SAVINGS";
    private BigDecimal balanceInInr = BigDecimal.ZERO;
    private boolean active = true;

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public UUID getPayerId() { return payerId; }
    public void setPayerId(UUID payerId) { this.payerId = payerId; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public BigDecimal getBalanceInInr() { return balanceInInr; }
    public void setBalanceInInr(BigDecimal balanceInInr) { this.balanceInInr = balanceInInr; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}