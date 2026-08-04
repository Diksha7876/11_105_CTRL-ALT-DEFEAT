package com.finance.PaymentProcessing.model;

import java.util.UUID;

public class BankAccount {
    private UUID accountId;
    private String accountNumber;
    private String accountHolderName;
    private boolean active = true;

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}