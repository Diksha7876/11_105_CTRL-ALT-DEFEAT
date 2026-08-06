<a id="top"></a>

# Payment Processing System

## Overview

This Spring Boot service handles outgoing and incoming payments, beneficiary management, bank account management, idempotent payment creation, payment status transitions, and payment history tracking.

## Quick Navigation

- [Run Locally](#run-locally)
- [API Conventions](#api-conventions)
- [API Reference](#api-reference)
- [Payments](#payments)
- [Beneficiaries](#beneficiaries)
- [Bank Accounts](#bank-accounts)
- [Payment History](#payment-history)
- [Incoming Payments](#incoming-payments)
- [Current User](#current-user)
- [Architecture Diagrams](#architecture-diagrams)
- [Notes](#notes)

## Run Locally

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

<a id="api-conventions"></a>

## API Conventions

- Base path: `/api`
- Content type: `application/json`
- Payment creation requires the `Idempotency-Key` header.
- Common currencies accepted by payment validation: `INR`, `USD`, `EUR`, `GBP`
- Payment methods: `CARD`, `NET_BANKING`, `UPI`
- Payment types: `BILL_PAYMENT`, `BENEFICIARY_TRANSFER`
- Payment statuses: `CREATED`, `VALIDATED`, `SENT`, `COMPLETED`, `FAILED`
- Bank account types: `SAVINGS`, `CURRENT`, `SALARY`

### Standard error response

```json
{
  "timestamp": "2026-08-06T09:12:45.240Z",
  "status": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "currency: must not be blank"
}
```

<a id="api-reference"></a>

## API Reference

Each title below is clickable and expands to the endpoint details.

<a id="payments"></a>
<details open>
<summary><strong>Payments</strong></summary>

### POST /api/payments

Creates a payment. If the same `Idempotency-Key` is reused, the API returns the original payment instead of creating a duplicate.

**Headers**

```http
Idempotency-Key: pay-20260806-001
Content-Type: application/json
```

**Example request: net banking transfer**

```json
{
  "amount": 2500.75,
  "currency": "INR",
  "reference": "Rent for August",
  "payerId": "11111111-1111-1111-1111-111111111111",
  "paymentMethod": "NET_BANKING",
  "sourceAccountId": "22222222-2222-2222-2222-222222222222",
  "beneficiaryId": "33333333-3333-3333-3333-333333333333",
  "paymentType": "BENEFICIARY_TRANSFER",
  "invoiceId": null
}
```

**Example request: card payment**

```json
{
  "amount": 1499.00,
  "currency": "INR",
  "reference": "Utility bill",
  "payerId": "11111111-1111-1111-1111-111111111111",
  "paymentMethod": "CARD",
  "cardType": "CREDIT_CARD",
  "cardHolderName": "Aarav Sharma",
  "cardNumber": "4111111111111111",
  "expiryMonth": "12",
  "expiryYear": "2030",
  "cvv": "123",
  "paymentType": "BILL_PAYMENT",
  "invoiceId": "INV-2026-1001"
}
```

**Example request: UPI payment**

```json
{
  "amount": 899.99,
  "currency": "INR",
  "reference": "Mobile recharge",
  "payerId": "11111111-1111-1111-1111-111111111111",
  "paymentMethod": "UPI",
  "upiId": "aarav.sharma@upi",
  "paymentType": "BILL_PAYMENT",
  "invoiceId": "INV-2026-1002"
}
```

**201 Created response**

```json
{
  "paymentId": "44444444-4444-4444-4444-444444444444",
  "amount": 2500.75,
  "currency": "INR",
  "reference": "Rent for August",
  "status": "CREATED",
  "paymentType": "BENEFICIARY_TRANSFER",
  "paymentMethod": "NET_BANKING",
  "cardType": null,
  "payerId": "11111111-1111-1111-1111-111111111111",
  "invoiceId": null,
  "sourceAccountId": "22222222-2222-2222-2222-222222222222",
  "beneficiaryId": "33333333-3333-3333-3333-333333333333",
  "cardLast4": null,
  "cardHolderName": null,
  "upiId": null,
  "createdAt": "2026-08-06T09:15:22.310Z",
  "updatedAt": "2026-08-06T09:15:22.310Z"
}
```

**200 OK response for reused idempotency key**

Returns the same shape as the `201 Created` response.

**Notes**

- `NET_BANKING` requires `beneficiaryId` and `sourceAccountId`.
- `UPI` requires `upiId` and must not include card fields or `beneficiaryId`.
- `CARD` requires `cardType`, `cardHolderName`, `cardNumber`, `expiryMonth`, `expiryYear`, and `cvv`.
- `BILL_PAYMENT` requires `invoiceId`.
- `BENEFICIARY_TRANSFER` must not include `invoiceId`.

### GET /api/payments/{id}

Returns one payment by ID.

**200 OK response**

```json
{
  "paymentId": "44444444-4444-4444-4444-444444444444",
  "amount": 2500.75,
  "currency": "INR",
  "reference": "Rent for August",
  "status": "CREATED",
  "paymentType": "BENEFICIARY_TRANSFER",
  "paymentMethod": "NET_BANKING",
  "cardType": null,
  "payerId": "11111111-1111-1111-1111-111111111111",
  "invoiceId": null,
  "sourceAccountId": "22222222-2222-2222-2222-222222222222",
  "beneficiaryId": "33333333-3333-3333-3333-333333333333",
  "cardLast4": null,
  "cardHolderName": null,
  "upiId": null,
  "createdAt": "2026-08-06T09:15:22.310Z",
  "updatedAt": "2026-08-06T09:15:22.310Z"
}
```

### PATCH /api/payments/{id}/status

Moves a payment through the supported status workflow.

**Example request**

```json
{
  "status": "VALIDATED",
  "remarks": "Payment validated by workflow engine",
  "errorCode": null,
  "actor": "SYSTEM"
}
```

**200 OK response**

```json
{
  "paymentId": "44444444-4444-4444-4444-444444444444",
  "amount": 2500.75,
  "currency": "INR",
  "reference": "Rent for August",
  "status": "VALIDATED",
  "paymentType": "BENEFICIARY_TRANSFER",
  "paymentMethod": "NET_BANKING",
  "cardType": null,
  "payerId": "11111111-1111-1111-1111-111111111111",
  "invoiceId": null,
  "sourceAccountId": "22222222-2222-2222-2222-222222222222",
  "beneficiaryId": "33333333-3333-3333-3333-333333333333",
  "cardLast4": null,
  "cardHolderName": null,
  "upiId": null,
  "createdAt": "2026-08-06T09:15:22.310Z",
  "updatedAt": "2026-08-06T09:17:10.904Z"
}
```

**Supported transitions**

- `CREATED -> VALIDATED` or `FAILED`
- `VALIDATED -> SENT` or `FAILED`
- `SENT -> COMPLETED` or `FAILED`

### GET /api/payments

Lists payments with optional status filtering and pageable query parameters.

**Query parameters**

- `status` optional
- `page` optional, default `0`
- `size` optional, default `20`
- `sort` optional, default `createdAt`

**Example request**

```http
GET /api/payments?status=CREATED&page=0&size=2&sort=createdAt,desc
```

**200 OK response**

```json
{
  "content": [
    {
      "paymentId": "44444444-4444-4444-4444-444444444444",
      "amount": 2500.75,
      "currency": "INR",
      "reference": "Rent for August",
      "status": "CREATED",
      "paymentType": "BENEFICIARY_TRANSFER",
      "paymentMethod": "NET_BANKING",
      "cardType": null,
      "payerId": "11111111-1111-1111-1111-111111111111",
      "invoiceId": null,
      "sourceAccountId": "22222222-2222-2222-2222-222222222222",
      "beneficiaryId": "33333333-3333-3333-3333-333333333333",
      "cardLast4": null,
      "cardHolderName": null,
      "upiId": null,
      "createdAt": "2026-08-06T09:15:22.310Z",
      "updatedAt": "2026-08-06T09:15:22.310Z"
    },
    {
      "paymentId": "55555555-5555-5555-5555-555555555555",
      "amount": 899.99,
      "currency": "INR",
      "reference": "Mobile recharge",
      "status": "CREATED",
      "paymentType": "BILL_PAYMENT",
      "paymentMethod": "UPI",
      "cardType": null,
      "payerId": "11111111-1111-1111-1111-111111111111",
      "invoiceId": "INV-2026-1002",
      "sourceAccountId": null,
      "beneficiaryId": null,
      "cardLast4": null,
      "cardHolderName": null,
      "upiId": "aarav.sharma@upi",
      "createdAt": "2026-08-06T09:16:40.512Z",
      "updatedAt": "2026-08-06T09:16:40.512Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 2
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "size": 2,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "first": true,
  "numberOfElements": 2,
  "empty": false
}
```

[Back to top](#top)

</details>

<a id="beneficiaries"></a>
<details>
<summary><strong>Beneficiaries</strong></summary>

### POST /api/beneficiaries

Creates a beneficiary record.

**Example request**

```json
{
  "name": "Ananya Patel",
  "accountNumber": "ANANYA123456",
  "bankName": "HDFC Bank",
  "ifscCode": "HDFC0123456",
  "email": "ananya.patel@example.com",
  "phone": "+91-9876543210"
}
```

**201 Created response**

```json
{
  "beneficiaryId": "33333333-3333-3333-3333-333333333333",
  "name": "Ananya Patel",
  "accountNumber": "ANANYA123456",
  "bankName": "HDFC Bank",
  "ifscCode": "HDFC0123456",
  "email": "ananya.patel@example.com",
  "phone": "+91-9876543210"
}
```

### GET /api/beneficiaries

Returns all beneficiaries.

**200 OK response**

```json
[
  {
    "beneficiaryId": "33333333-3333-3333-3333-333333333333",
    "name": "Ananya Patel",
    "accountNumber": "ANANYA123456",
    "bankName": "HDFC Bank",
    "ifscCode": "HDFC0123456",
    "email": "ananya.patel@example.com",
    "phone": "+91-9876543210"
  }
]
```

### GET /api/beneficiaries/{id}

Returns one beneficiary by ID.

**200 OK response**

```json
{
  "beneficiaryId": "33333333-3333-3333-3333-333333333333",
  "name": "Ananya Patel",
  "accountNumber": "ANANYA123456",
  "bankName": "HDFC Bank",
  "ifscCode": "HDFC0123456",
  "email": "ananya.patel@example.com",
  "phone": "+91-9876543210"
}
```

[Back to top](#top)

</details>

<a id="bank-accounts"></a>
<details>
<summary><strong>Bank Accounts</strong></summary>

### POST /api/accounts

Creates a source bank account. If `payerId` is omitted, the service uses `11111111-1111-1111-1111-111111111111`. If `openingBalanceInr` is omitted, the service uses `50000.00`.

**Example request**

```json
{
  "accountNumber": "CTRLALT567890",
  "accountHolderName": "Aarav Sharma",
  "payerId": "11111111-1111-1111-1111-111111111111",
  "openingBalanceInr": 75000.00,
  "accountType": "SAVINGS"
}
```

**201 Created response**

```json
{
  "accountId": "22222222-2222-2222-2222-222222222222",
  "accountNumber": "CTRLALT567890",
  "accountHolderName": "Aarav Sharma",
  "payerId": "11111111-1111-1111-1111-111111111111",
  "accountType": "SAVINGS",
  "balanceInInr": 75000.00,
  "active": true
}
```

### GET /api/accounts

Returns all bank accounts.

**200 OK response**

```json
[
  {
    "accountId": "22222222-2222-2222-2222-222222222222",
    "accountNumber": "CTRLALT567890",
    "accountHolderName": "Aarav Sharma",
    "payerId": "11111111-1111-1111-1111-111111111111",
    "accountType": "SAVINGS",
    "balanceInInr": 75000.00,
    "active": true
  }
]
```

[Back to top](#top)

</details>

<a id="payment-history"></a>
<details>
<summary><strong>Payment History</strong></summary>

### GET /api/payments/{paymentId}/history

Returns the chronological status history for a payment.

**200 OK response**

```json
[
  {
    "historyId": "66666666-6666-6666-6666-666666666666",
    "oldStatus": null,
    "newStatus": "CREATED",
    "timestamp": "2026-08-06T09:15:22.315Z",
    "remarks": "Payment created",
    "errorCode": null,
    "actor": "SYSTEM"
  },
  {
    "historyId": "77777777-7777-7777-7777-777777777777",
    "oldStatus": "CREATED",
    "newStatus": "VALIDATED",
    "timestamp": "2026-08-06T09:17:10.911Z",
    "remarks": "Payment validated by workflow engine",
    "errorCode": null,
    "actor": "SYSTEM"
  }
]
```

### GET /api/payments/{paymentId}/history/timeline

Returns the same data as the history endpoint, exposed through a timeline-friendly route.

**200 OK response**

```json
[
  {
    "historyId": "66666666-6666-6666-6666-666666666666",
    "oldStatus": null,
    "newStatus": "CREATED",
    "timestamp": "2026-08-06T09:15:22.315Z",
    "remarks": "Payment created",
    "errorCode": null,
    "actor": "SYSTEM"
  }
]
```

[Back to top](#top)

</details>

<a id="incoming-payments"></a>
<details>
<summary><strong>Incoming Payments</strong></summary>

### GET /api/incoming-payments

Returns the in-memory list of incoming payments.

**200 OK response**

```json
[
  {
    "incomingPaymentId": "88888888-8888-8888-8888-888888888888",
    "payerId": "11111111-1111-1111-1111-111111111111",
    "amount": 3250,
    "currency": "INR",
    "reference": "Salary credit",
    "sourceName": "Employer Payroll",
    "destinationAccountId": "22222222-2222-2222-2222-222222222222",
    "receivedAt": "2026-08-06T08:45:00Z",
    "createdAt": "2026-08-06T08:45:01Z",
    "updatedAt": "2026-08-06T08:45:01Z"
  }
]
```

### POST /api/incoming-payments

Adds an incoming payment entry to the in-memory store.

**Example request**

```json
{
  "amount": 3250.00,
  "currency": "INR",
  "reference": "Salary credit",
  "sourceName": "Employer Payroll",
  "destinationAccountId": "22222222-2222-2222-2222-222222222222",
  "receivedAt": "2026-08-06T08:45:00Z"
}
```

**201 Created response**

```json
{
  "incomingPaymentId": "88888888-8888-8888-8888-888888888888",
  "payerId": "11111111-1111-1111-1111-111111111111",
  "amount": 3250,
  "currency": "INR",
  "reference": "Salary credit",
  "sourceName": "Employer Payroll",
  "destinationAccountId": "22222222-2222-2222-2222-222222222222",
  "receivedAt": "2026-08-06T08:45:00Z",
  "createdAt": "2026-08-06T08:45:01Z",
  "updatedAt": "2026-08-06T08:45:01Z"
}
```

[Back to top](#top)

</details>

<a id="current-user"></a>
<details>
<summary><strong>Current User</strong></summary>

### GET /api/users/current

Returns the default payer context used by the backend.

**200 OK response**

```json
{
  "payerId": "11111111-1111-1111-1111-111111111111"
}
```

[Back to top](#top)

</details>

---

<a id="architecture-diagrams"></a>

## Architecture Diagrams

<details>
<summary><strong>Class Diagram</strong></summary>

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

</details>

<details>
<summary><strong>Sequence Diagrams</strong></summary>

### 1. Create Payment (Idempotent)

```mermaid
sequenceDiagram
    actor Client
    participant PC as PaymentController
    participant PS as PaymentServiceImpl
    participant PR as PaymentRepository
    participant VS as ValidationService
    participant HS as HistoryServiceImpl
    participant HR as PaymentHistoryRepository

    Client->>PC: POST /api/payments (request, Idempotency-Key)
    PC->>PS: createPayment(dto, idempotencyKey)
    PS->>PR: findByIdempotencyKey(key)
    alt Payment already exists
        PR-->>PS: Optional<Payment>
        PS-->>PC: PaymentCreationResult(created=false)
        PC-->>Client: 200 OK (existing payment)
    else New payment
        PR-->>PS: Optional.empty()
        PS->>VS: validate request
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

    Client->>PC: GET /api/payments/{id}
    PC->>PS: getPayment(id)
    PS->>PR: findById(id)
    alt Found
        PR-->>PS: Payment
        PS-->>PC: PaymentResponse
        PC-->>Client: 200 OK
    else Not found
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

    Client->>PC: PATCH /api/payments/{id}/status
    PC->>PS: updateStatus(id, request)
    PS->>PR: findById(id)
    PR-->>PS: Payment (oldStatus)
    PS->>VS: validateStatusTransition(oldStatus, newStatus)
    alt Valid transition
        PS->>PR: save(payment) [status=newStatus, updatedAt]
        PR-->>PS: Payment
        PS->>HS: recordTransition(payment, oldStatus, newStatus, remarks, errorCode, actor)
        HS->>HR: save(paymentHistory)
        PS-->>PC: PaymentResponse
        PC-->>Client: 200 OK
    else Invalid transition
        VS-->>PS: throw BadRequestException
        PS-->>PC: Exception
        PC-->>Client: 400 Bad Request
    end
```

### 4. List Payments (Filter + Pagination)

```mermaid
sequenceDiagram
    actor Client
    participant PC as PaymentController
    participant PS as PaymentServiceImpl
    participant PR as PaymentRepository

    Client->>PC: GET /api/payments?status=X&page=0&size=10
    PC->>PS: listPayments(status, pageable)
    alt Status provided
        PS->>PR: findByStatus(status, pageable)
    else No filter
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

    Client->>BC: POST /api/beneficiaries
    BC->>BS: addBeneficiary(dto)
    BS->>BR: findByAccountNumber(accountNumber)
    alt Already exists
        BR-->>BS: Optional<Beneficiary>
        BS-->>BC: throw exception
        BC-->>Client: 400 or 409
    else New beneficiary
        BR-->>BS: Optional.empty()
        BS->>BR: save(beneficiary)
        BR-->>BS: Beneficiary
        BS-->>BC: BeneficiaryResponse
        BC-->>Client: 201 Created
    end
```

### 6. Create and List Bank Accounts

```mermaid
sequenceDiagram
    actor Client
    participant BAC as BankAccountController
    participant BAR as BankAccountRepository

    Client->>BAC: POST /api/accounts
    BAC->>BAR: save(bankAccount)
    BAR-->>BAC: BankAccount
    BAC-->>Client: 201 Created (BankAccountResponse)

    Client->>BAC: GET /api/accounts
    BAC->>BAR: findAll()
    BAR-->>BAC: List<BankAccount>
    BAC-->>Client: 200 OK (List<BankAccountResponse>)
```

### 7. Get Payment History / Timeline

```mermaid
sequenceDiagram
    actor Client
    participant HC as HistoryController
    participant HS as HistoryServiceImpl
    participant HR as PaymentHistoryRepository
    participant PR as PaymentRepository

    Client->>HC: GET /api/payments/{paymentId}/history
    HC->>HS: getHistory(paymentId)
    HS->>PR: findById(paymentId)
    PR-->>HS: Payment
    HS->>HR: findByPaymentPaymentIdOrderByTimestampAsc(paymentId)
    HR-->>HS: List<PaymentHistory>
    HS-->>HC: List<PaymentHistoryResponse>
    HC-->>Client: 200 OK

    Client->>HC: GET /api/payments/{paymentId}/history/timeline
    HC->>HS: getHistory(paymentId)
    HS-->>HC: List<PaymentHistoryResponse>
    HC-->>Client: 200 OK
```

</details>

---

<a id="notes"></a>

## Notes

- The API examples above match the current controller routes and DTO field names in the backend source.
- Incoming payments are stored in memory and will reset when the application restarts.
- The timeline endpoint currently returns the same data as the main history endpoint.
- The diagrams were updated to reflect the current routes `/api/payments`, `/api/accounts`, and the `PATCH` status update endpoint.

[Back to top](#top)
