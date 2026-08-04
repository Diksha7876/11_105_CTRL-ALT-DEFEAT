package com.finance.PaymentProcessing.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID accountId;
    @Column(nullable = false, unique = true, length = 34) private String accountNumber;
    @Column(nullable = false) private String accountHolderName;
    @Column(nullable = false) private boolean active = true;
    public UUID getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}