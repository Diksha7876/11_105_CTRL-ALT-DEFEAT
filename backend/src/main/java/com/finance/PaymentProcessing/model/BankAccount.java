package com.finance.PaymentProcessing.model;

import java.math.BigDecimal;

public class BankAccount {
    private String accountId;
    private String accountNumber;
    private String accountHolderName;
    private String payerId;
    private String accountType = "SAVINGS";
    private BigDecimal balanceInInr = BigDecimal.ZERO;
    private BigDecimal maxTransactionLimitInInr = new BigDecimal("1000000.00");
    private boolean active = true;

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public BigDecimal getBalanceInInr() { return balanceInInr; }
    public void setBalanceInInr(BigDecimal balanceInInr) { this.balanceInInr = balanceInInr; }
    public BigDecimal getMaxTransactionLimitInInr() { return maxTransactionLimitInInr; }
    public void setMaxTransactionLimitInInr(BigDecimal maxTransactionLimitInInr) { this.maxTransactionLimitInInr = maxTransactionLimitInInr; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}