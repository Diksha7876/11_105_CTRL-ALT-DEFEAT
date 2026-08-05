# Payment Processing System

## Overview
This project defines a payment processing system that supports multiple payment channels, prevents duplicate payments, validates transactions, handles failures reliably, and provides a simple shared interface for single user.

## Run After Folder Restructure

All backend commands should be run from the `backend` folder.

### Windows PowerShell

```powershell
cd backend
.\mvnw.cmd clean package -DskipTests
.\mvnw.cmd spring-boot:run
```

### Docker

```powershell
cd backend
docker-compose up --build
```

## Meeting Notes for 30-07-2026
Multiple payment channels need to be supported, including UPI, Card Payments, and NetBanking.

To avoid duplicate payments, the system should check whether the user is attempting to pay for the same invoice more than once.

Various validation rules were discussed to ensure secure and error‑free transactions.

A clear solution for handling payment failures is required, including user notifications and retry options.

The system will have a single user interface, but it must support multiple end users simultaneously.

The interface should be simple, intuitive, and ready to use, ensuring a smooth user experience.

<br>
<br>
------------------------------------------------------------------------------

# Payment Service - Visual Diagrams & Architecture

Spring Boot based Payment Service supporting payment creation (idempotent), status transitions, beneficiary management, bank account management, and full transaction history/audit trail.

## Table of Contents
- [Class Diagram](#class-diagram)
- [Sequence Diagrams](#sequence-diagrams)
  - [1. Create Payment (Idempotent)](#1-create-payment-idempotent)
  - [2. Get Payment by ID](#2-get-payment-by-id)
  - [3. Update Payment Status](#3-update-payment-status)
  - [4. List Payments (filter + pagination)](#4-list-payments-filter--pagination)
  - [5. Add Beneficiary](#5-add-beneficiary)
  - [6. List / Get Beneficiary](#6-list--get-beneficiary)
  - [7. Create / List Bank Account](#7-create--list-bank-account)
  - [8. Get Payment History / Transaction Timeline](#8-get-payment-history--transaction-timeline)

---

## Class Diagram

```mermaid
classDiagram
    class PaymentController {
        +createPayment(request, idempotencyKey) ResponseEntity
        +getPayment(id) PaymentResponse
        +updatePaymentStatus(id, request) PaymentResponse
        +listPayments(status, pageable) Page~PaymentResponse~
    }

    class BeneficiaryController {
        +addBeneficiary(request) ResponseEntity
        +listBeneficiaries() List~BeneficiaryResponse~
        +getBeneficiary(id) BeneficiaryResponse
    }

    class BankAccountController {
        +create(request) ResponseEntity
        +list() List~BankAccountResponse~
    }

    class HistoryController {
        +getPaymentHistory(paymentId) List~PaymentHistoryResponse~
        +getTransactionTimeline(paymentId) List~PaymentHistoryResponse~
    }

    class PaymentService {
        <<interface>>
        +createPayment(dto, idempotencyKey) PaymentCreationResult
        +getPayment(id) PaymentResponse
        +updateStatus(id, request) PaymentResponse
        +listPayments(status, pageable) Page~PaymentResponse~
    }

    class PaymentServiceImpl {
        -paymentRepository PaymentRepository
        -beneficiaryRepository BeneficiaryRepository
        -validationService ValidationService
        -historyService HistoryService
    }

    class BeneficiaryService {
        <<interface>>
        +addBeneficiary(dto) BeneficiaryResponse
        +listBeneficiaries() List~BeneficiaryResponse~
        +getBeneficiary(id) BeneficiaryResponse
    }

    class BeneficiaryServiceImpl {
        -repository BeneficiaryRepository
    }

    class HistoryService {
        <<interface>>
        +recordTransition(payment, old, new, remarks, errorCode, actor) void
        +getHistory(paymentId) List~PaymentHistoryResponse~
    }

    class HistoryServiceImpl {
        -historyRepository PaymentHistoryRepository
        -paymentRepository PaymentRepository
    }

    class ValidationService {
        +validateAmount(amount) void
        +validateCurrency(currency) void
        +validateBeneficiary(beneficiaryId) void
        +validateSourceAccount(sourceAccountId, beneficiaryId) void
        +validatePaymentDetails(paymentType, invoiceId) void
        +validateStatusTransition(old, new) void
    }

    class PaymentRepository {
        <<interface>>
        +findByIdempotencyKey(key) Optional~Payment~
        +findByPayerIdAndInvoiceId(payerId, invoiceId) Optional~Payment~
        +findByStatus(status, pageable) Page~Payment~
    }

    class BeneficiaryRepository {
        <<interface>>
        +findByAccountNumber(accountNumber) Optional~Beneficiary~
    }

    class BankAccountRepository {
        <<interface>>
    }

    class PaymentHistoryRepository {
        <<interface>>
        +findByPaymentPaymentIdOrderByTimestampAsc(paymentId) List~PaymentHistory~
    }

    class Payment {
        +paymentId UUID
        +amount BigDecimal
        +currency String
        +reference String
        +status PaymentStatus
        +paymentType PaymentType
        +payerId UUID
        +invoiceId String
        +idempotencyKey String
        +createdAt Instant
        +updatedAt Instant
        +version Long
    }

    class Beneficiary {
        +beneficiaryId UUID
        +name String
        +accountNumber String
        +bankName String
        +ifscCode String
        +email String
        +phone String
    }

    class BankAccount {
        +accountId UUID
        +accountNumber String
        +accountHolderName String
        +active boolean
    }

    class PaymentHistory {
        +historyId UUID
        +oldStatus PaymentStatus
        +newStatus PaymentStatus
        +timestamp Instant
        +remarks String
        +errorCode String
        +actor String
    }

    class PaymentStatus {
        <<enumeration>>
        CREATED
        VALIDATED
        SENT
        COMPLETED
        FAILED
    }

    class PaymentType {
        <<enumeration>>
        BILL_PAYMENT
        BENEFICIARY_TRANSFER
    }

    class PaymentCreationResult {
        +payment PaymentResponse
        +created boolean
    }

    PaymentService <|.. PaymentServiceImpl
    BeneficiaryService <|.. BeneficiaryServiceImpl
    HistoryService <|.. HistoryServiceImpl

    PaymentController --> PaymentService
    BeneficiaryController --> BeneficiaryService
    BankAccountController --> BankAccountRepository
    HistoryController --> HistoryService

    PaymentServiceImpl --> PaymentRepository
    PaymentServiceImpl --> BeneficiaryRepository
    PaymentServiceImpl --> ValidationService
    PaymentServiceImpl --> HistoryService
    BeneficiaryServiceImpl --> BeneficiaryRepository
    HistoryServiceImpl --> PaymentHistoryRepository
    HistoryServiceImpl --> PaymentRepository
    ValidationService --> BeneficiaryRepository
    ValidationService --> BankAccountRepository

    Payment "many" --> "1" Beneficiary : beneficiary
    Payment "many" --> "1" BankAccount : sourceAccount
    PaymentHistory "many" --> "1" Payment
    Payment --> PaymentStatus
    Payment --> PaymentType
    PaymentService --> PaymentCreationResult
```

---

## Sequence Diagrams

### 1. Create Payment (Idempotent)

```mermaid
sequenceDiagram
    actor Client
    participant PC as PaymentController
    participant PS as PaymentServiceImpl
    participant PR as PaymentRepository
    participant VS as ValidationService
    participant BR as BeneficiaryRepository
    participant HS as HistoryServiceImpl
    participant HR as PaymentHistoryRepository

    Client->>PC: POST /payments (request, idempotencyKey)
    PC->>PS: createPayment(dto, idempotencyKey)
    PS->>PR: findByIdempotencyKey(key)
    alt Payment already exists
        PR-->>PS: Optional<Payment>
        PS-->>PC: PaymentCreationResult(created=false)
        PC-->>Client: 200 OK (existing payment)
    else New Payment
        PR-->>PS: Optional.empty()
        PS->>VS: validateAmount(amount)
        PS->>VS: validateCurrency(currency)
        PS->>VS: validateBeneficiary(beneficiaryId)
        VS->>BR: findById(beneficiaryId)
        BR-->>VS: Beneficiary
        PS->>VS: validateSourceAccount(sourceAccountId, beneficiaryId)
        PS->>VS: validatePaymentDetails(paymentType, invoiceId)
        PS->>PR: save(payment) [status=CREATED]
        PR-->>PS: Payment
        PS->>HS: recordTransition(payment, null, CREATED, remarks, null, actor)
        HS->>HR: save(paymentHistory)
        PS-->>PC: PaymentCreationResult(created=true)
        PC-->>Client: 201 Created (PaymentResponse)
    end
```

### 2. Get Payment by ID

```mermaid
sequenceDiagram
    actor Client
    participant PC as PaymentController
    participant PS as PaymentServiceImpl
    participant PR as PaymentRepository

    Client->>PC: GET /payments/{id}
    PC->>PS: getPayment(id)
    PS->>PR: findById(id)
    alt Found
        PR-->>PS: Payment
        PS-->>PC: PaymentResponse
        PC-->>Client: 200 OK
    else Not Found
        PR-->>PS: Optional.empty()
        PS-->>PC: throw NotFoundException
        PC-->>Client: 404 Not Found
    end
```

### 3. Update Payment Status

```mermaid
sequenceDiagram
    actor Client
    participant PC as PaymentController
    participant PS as PaymentServiceImpl
    participant PR as PaymentRepository
    participant VS as ValidationService
    participant HS as HistoryServiceImpl
    participant HR as PaymentHistoryRepository

    Client->>PC: PUT /payments/{id}/status (request)
    PC->>PS: updateStatus(id, request)
    PS->>PR: findById(id)
    PR-->>PS: Payment (oldStatus)
    PS->>VS: validateStatusTransition(oldStatus, newStatus)
    alt Valid Transition
        VS-->>PS: OK
        PS->>PR: save(payment) [status=newStatus, updatedAt]
        PR-->>PS: Payment
        PS->>HS: recordTransition(payment, oldStatus, newStatus, remarks, errorCode, actor)
        HS->>HR: save(paymentHistory)
        PS-->>PC: PaymentResponse
        PC-->>Client: 200 OK
    else Invalid Transition
        VS-->>PS: throw InvalidTransitionException
        PS-->>PC: Exception
        PC-->>Client: 400 Bad Request
    end
```

### 4. List Payments (filter + pagination)

```mermaid
sequenceDiagram
    actor Client
    participant PC as PaymentController
    participant PS as PaymentServiceImpl
    participant PR as PaymentRepository

    Client->>PC: GET /payments?status=X&page=0&size=10
    PC->>PS: listPayments(status, pageable)
    alt status provided
        PS->>PR: findByStatus(status, pageable)
    else no filter
        PS->>PR: findAll(pageable)
    end
    PR-->>PS: Page<Payment>
    PS-->>PC: Page<PaymentResponse>
    PC-->>Client: 200 OK
```

### 5. Add Beneficiary

```mermaid
sequenceDiagram
    actor Client
    participant BC as BeneficiaryController
    participant BS as BeneficiaryServiceImpl
    participant BR as BeneficiaryRepository

    Client->>BC: POST /beneficiaries (request)
    BC->>BS: addBeneficiary(dto)
    BS->>BR: findByAccountNumber(accountNumber)
    alt Already exists
        BR-->>BS: Optional<Beneficiary>
        BS-->>BC: throw DuplicateBeneficiaryException
        BC-->>Client: 409 Conflict
    else New
        BR-->>BS: Optional.empty()
        BS->>BR: save(beneficiary)
        BR-->>BS: Beneficiary
        BS-->>BC: BeneficiaryResponse
        BC-->>Client: 201 Created
    end
```

### 6. List / Get Beneficiary

```mermaid
sequenceDiagram
    actor Client
    participant BC as BeneficiaryController
    participant BS as BeneficiaryServiceImpl
    participant BR as BeneficiaryRepository

    Client->>BC: GET /beneficiaries
    BC->>BS: listBeneficiaries()
    BS->>BR: findAll()
    BR-->>BS: List<Beneficiary>
    BS-->>BC: List<BeneficiaryResponse>
    BC-->>Client: 200 OK

    Client->>BC: GET /beneficiaries/{id}
    BC->>BS: getBeneficiary(id)
    BS->>BR: findById(id)
    BR-->>BS: Beneficiary
    BS-->>BC: BeneficiaryResponse
    BC-->>Client: 200 OK
```

### 7. Create / List Bank Account

```mermaid
sequenceDiagram
    actor Client
    participant BAC as BankAccountController
    participant BAR as BankAccountRepository

    Client->>BAC: POST /bank-accounts (request)
    BAC->>BAR: save(bankAccount)
    BAR-->>BAC: BankAccount
    BAC-->>Client: 201 Created (BankAccountResponse)

    Client->>BAC: GET /bank-accounts
    BAC->>BAR: findAll()
    BAR-->>BAC: List<BankAccount>
    BAC-->>Client: 200 OK (List<BankAccountResponse>)
```

### 8. Get Payment History / Transaction Timeline

```mermaid
sequenceDiagram
    actor Client
    participant HC as HistoryController
    participant HS as HistoryServiceImpl
    participant HR as PaymentHistoryRepository
    participant PR as PaymentRepository

    Client->>HC: GET /payments/{paymentId}/history
    HC->>HS: getHistory(paymentId)
    HS->>PR: findById(paymentId)
    PR-->>HS: Payment
    HS->>HR: findByPaymentPaymentIdOrderByTimestampAsc(paymentId)
    HR-->>HS: List<PaymentHistory>
    HS-->>HC: List<PaymentHistoryResponse>
    HC-->>Client: 200 OK

    Client->>HC: GET /payments/{paymentId}/timeline
    HC->>HS: getHistory(paymentId)
    Note over HS: Same underlying call,<br/>different endpoint/view
    HS-->>HC: List<PaymentHistoryResponse>
    HC-->>Client: 200 OK
```

---

## Notes
- All sequence diagrams follow the exact method signatures and dependencies defined in the class diagram.
- `HistoryServiceImpl` is invoked internally by `PaymentServiceImpl` to record transitions during payment creation and status updates.
- `ValidationService` performs no persistence of its own; it only reads from `BeneficiaryRepository` and `BankAccountRepository` for validation checks.
