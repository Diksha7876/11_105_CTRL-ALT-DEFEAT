<a id="top"></a>

# Frontend README

## Overview

This frontend is a React 19 + Vite single-page application for the Payment Processing System. It provides a dashboard for incoming payment trends, payment creation workflows, beneficiary and source account management, payment history browsing, and payment status tracking.

## Quick Navigation

- [Tech Stack](#tech-stack)
- [Project Location](#project-location)
- [Run Locally](#run-locally)
- [Environment Configuration](#environment-configuration)
- [Application Structure](#application-structure)
- [Routing](#routing)
- [Main Features](#main-features)
- [Backend API Integration](#backend-api-integration)
- [Shared UI and State](#shared-ui-and-state)
- [Constants and Rules](#constants-and-rules)
- [Error Handling](#error-handling)
- [Notes](#notes)
- [Suggested Startup Order](#suggested-startup-order)
- [Future Documentation Extensions](#future-documentation-extensions)

<a id="tech-stack"></a>
<details open>
<summary><strong>Tech Stack</strong></summary>

- React 19
- Vite 8
- React Router DOM 7
- Axios
- Tailwind CSS 4
- Recharts
- React Hot Toast
- React Hook Form, Zod, and Hookform Resolvers available in dependencies

[Back to top](#top)

</details>

<a id="project-location"></a>
<details>
<summary><strong>Project Location</strong></summary>

- Frontend app: `frontend/`
- This documentation file: `readme/README_Frontend.md`

[Back to top](#top)

</details>

<a id="run-locally"></a>
<details>
<summary><strong>Run Locally</strong></summary>

Run all commands from the `frontend` folder.

### Install dependencies

```powershell
cd frontend
npm install
```

### Start development server

```powershell
cd frontend
npm run dev
```

### Build for production

```powershell
cd frontend
npm run build
```

### Preview production build

```powershell
cd frontend
npm run preview
```

### Lint

```powershell
cd frontend
npm run lint
```

[Back to top](#top)

</details>

<a id="environment-configuration"></a>
<details>
<summary><strong>Environment Configuration</strong></summary>

The frontend uses the following environment variable:

```env
VITE_API_BASE_URL=http://localhost:8080
```

If this variable is not set, the frontend defaults to `http://localhost:8080`.

[Back to top](#top)

</details>

<a id="application-structure"></a>
<details>
<summary><strong>Application Structure</strong></summary>

```text
frontend/
  src/
    components/
    context/
    lib/
    pages/
```

### Important folders

- `src/components/`: layout, shared UI, async state, tables, badges, and headers
- `src/context/`: current-user state and provider logic
- `src/lib/`: API client, constants, formatters, and theme storage helpers
- `src/pages/`: route-level screens for dashboard, payments, history, and incoming flows

[Back to top](#top)

</details>

<a id="routing"></a>
<details>
<summary><strong>Routing</strong></summary>

The app uses `BrowserRouter` and defines the following routes:

| Route | Screen | Purpose |
| --- | --- | --- |
| `/` | Dashboard | Shows summary actions and incoming totals chart |
| `/payment` | Payment | Create payment, manage source accounts, manage beneficiaries |
| `/incoming-payments` | Incoming Payments | View incoming rows and create incoming payment entries |
| `/payment-history` | Payment History | Combined incoming and outgoing activity listing |
| `/payment-history/:paymentId` | Payment Details | Inspect one outgoing payment and update status |
| `/payment-history/:paymentId/history` | Payment Audit Trail | View detailed lifecycle history for a payment |
| `/dashboard` | Redirect | Redirects to `/` |
| `*` | Not Found | Fallback route |

[Back to top](#top)

</details>

<a id="main-features"></a>
<details open>
<summary><strong>Main Features</strong></summary>

### 1. Dashboard

The dashboard is the landing page and provides:

- quick navigation cards to payment creation, incoming payments, and payment history
- daily and weekly bar chart views for incoming totals
- total incoming amount summary
- current user context in the top navigation

### 2. Payment Workspace

The payment page contains three tabs:

- `Create Payment`
- `Source Accounts`
- `Beneficiaries`

#### Create Payment

Supports three outgoing payment methods:

- `CARD`
- `NET_BANKING`
- `UPI`

Implemented behavior:

- client-side validation for amount, card data, expiry, CVV, and UPI format
- fetches beneficiaries for net banking selection
- generates or reuses an idempotency key when submitting payments
- posts outgoing payments to the backend with the `Idempotency-Key` header
- shows created payment details after successful submission

#### Source Accounts

Supports:

- generating a unique account ID in the UI
- creating source accounts
- listing existing source accounts

#### Beneficiaries

Supports:

- creating beneficiaries with form validation
- listing existing beneficiaries
- viewing beneficiary details in a modal

### 3. Incoming Payments

The incoming payments page has two sections:

- `Incoming History`
- `Create Incoming`

Implemented behavior:

- displays incoming payment rows in a table
- displays daily and weekly incoming trends using Recharts
- supports manual creation of incoming payment entries
- refreshes the dataset after successful creation

### 4. Payment History

The payment history page merges two data sources:

- outgoing payments from `/api/payments`
- incoming payments from `/api/incoming-payments`

Implemented behavior:

- status filter including `RECEIVED` for incoming entries
- timeline filters: `ALL`, `24H`, `7D`, `1M`, `3M`, `6M`, `1Y`
- sorting by latest, oldest, amount descending, and amount ascending
- pagination in the UI
- row click navigation to either payment details or incoming payments

### 5. Payment Details and Audit Trail

The payment details view supports:

- loading one payment record
- loading payment history records
- visual stage progress for `CREATED`, `VALIDATED`, `SENT`, `COMPLETED`, and `FAILED`
- valid next-status actions based on the current status
- updating status with remarks, actor, and optional failure code

The dedicated audit trail page shows:

- lifecycle stage progression
- detailed timeline entries
- actor, remarks, timestamps, and failure code information

[Back to top](#top)

</details>

<a id="backend-api-integration"></a>
<details>
<summary><strong>Backend API Integration</strong></summary>

The frontend communicates with the backend using a shared Axios client configured in `src/lib/api.js`.

### Endpoints used by the frontend

| Method | Endpoint | Used For |
| --- | --- | --- |
| `GET` | `/api/users/current` | Load current payer context |
| `GET` | `/api/incoming-payments` | Load incoming payment rows |
| `POST` | `/api/incoming-payments` | Create incoming payment |
| `GET` | `/api/beneficiaries` | Load beneficiary list |
| `POST` | `/api/beneficiaries` | Create beneficiary |
| `GET` | `/api/beneficiaries/:id` | Load beneficiary details |
| `GET` | `/api/accounts` | Load source accounts |
| `POST` | `/api/accounts` | Create source account |
| `POST` | `/api/payments` | Create outgoing payment |
| `GET` | `/api/payments` | List outgoing payments |
| `GET` | `/api/payments/:id` | Get one outgoing payment |
| `PATCH` | `/api/payments/:id/status` | Update payment status |
| `GET` | `/api/payments/:id/history` | Load payment history |

[Back to top](#top)

</details>

<a id="shared-ui-and-state"></a>
<details>
<summary><strong>Shared UI and State</strong></summary>

### Layout

The application shell includes:

- responsive sidebar navigation
- sticky header
- theme toggle for dark and light mode
- current payer display when available

### Current user context

`UserContext` fetches the current payer from the backend on load and exposes:

- `currentUser`
- `loading`
- `error`

### Feedback and loading states

The UI uses:

- `react-hot-toast` for API success and error feedback
- `AsyncState` for loading, retry, and empty-state rendering
- `SkeletonTable` for loading placeholders

[Back to top](#top)

</details>

<a id="constants-and-rules"></a>
<details>
<summary><strong>Constants and Rules</strong></summary>

Frontend constants currently include:

- payment statuses: `CREATED`, `VALIDATED`, `SENT`, `COMPLETED`, `FAILED`
- currencies: `INR`, `USD`, `EUR`, `GBP`
- payment types: `BILL_PAYMENT`, `BENEFICIARY_TRANSFER`
- next status flow:
- `CREATED -> VALIDATED | FAILED`
- `VALIDATED -> SENT | FAILED`
- `SENT -> COMPLETED | FAILED`

[Back to top](#top)

</details>

<a id="error-handling"></a>
<details>
<summary><strong>Error Handling</strong></summary>

API errors are normalized through helper functions in `src/lib/api.js`.

Handled cases include:

- backend JSON error payloads with `status`, `errorCode`, and `message`
- request timeout errors
- generic network failures

Displayed error format:

```text
status | errorCode | message
```

[Back to top](#top)

</details>

<a id="notes"></a>
<details>
<summary><strong>Notes</strong></summary>

- The frontend assumes the backend is available on `http://localhost:8080` unless overridden through `VITE_API_BASE_URL`.
- Incoming payments are backed by an in-memory backend endpoint, so those records may reset when the backend restarts.
- Payment history combines incoming and outgoing records into one UI table for easier activity tracking.
- Theme preference is persisted through local storage helpers.

[Back to top](#top)

</details>

<a id="suggested-startup-order"></a>
<details>
<summary><strong>Suggested Startup Order</strong></summary>

1. Start the backend service.
2. Confirm the backend is reachable at the configured base URL.
3. Start the frontend with `npm run dev`.
4. Open the local Vite URL shown in the terminal.

[Back to top](#top)

</details>

<a id="future-documentation-extensions"></a>
<details>
<summary><strong>Future Documentation Extensions</strong></summary>

This README can be extended later with:

- screenshots for each screen
- frontend architecture diagrams
- component-level documentation
- testing strategy and deployment notes

[Back to top](#top)

</details>