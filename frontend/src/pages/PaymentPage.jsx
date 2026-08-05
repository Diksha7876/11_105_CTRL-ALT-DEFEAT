import { v4 as uuidv4 } from "uuid";
import { useCallback, useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import SkeletonTable from "../components/SkeletonTable";
import { useCurrentUser } from "../context/UserContext";
import { api, getErrorText } from "../lib/api";
import { CURRENCIES } from "../lib/constants";

const accountNumberRegex = /^[A-Za-z0-9]{6,34}$/;
const ifscRegex = /^[A-Z]{4}0[A-Z0-9]{6}$/;

const TABS = [
  { key: "create", label: "Create Payment" },
  { key: "accounts", label: "Source Accounts" },
  { key: "beneficiaries", label: "Beneficiaries" },
];

// =========================================================
// Create Payment Section
// =========================================================
const initialPaymentForm = {
  sourceAccountId: "",
  beneficiaryId: "",
  amount: "",
  currency: "INR",
  reference: "",
  paymentType: "BILL_PAYMENT",
  invoiceId: "",
};

function CreatePaymentSection() {
  const { currentUser } = useCurrentUser();
  const payerId = currentUser?.payerId ?? null;
  const [form, setForm] = useState(initialPaymentForm);
  const [formErrors, setFormErrors] = useState({});
  const [accounts, setAccounts] = useState([]);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [idempotencyKey, setIdempotencyKey] = useState("");
  const [createdPayment, setCreatedPayment] = useState(null);

  const loadDependencies = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [accountsRes, beneficiariesRes] = await Promise.all([
        api.get("/api/accounts"),
        api.get("/api/beneficiaries"),
      ]);
      setAccounts(accountsRes.data ?? []);
      setBeneficiaries(beneficiariesRes.data ?? []);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadDependencies();
  }, [loadDependencies]);

  const ownedAccounts = useMemo(() => {
    return accounts.filter((account) => {
      if (!payerId) {
        return true;
      }
      const ownerPayerId =
        account.payerId ?? account.ownerPayerId ?? account.userPayerId ?? null;
      if (!ownerPayerId) {
        return true;
      }
      return String(ownerPayerId) === String(payerId);
    });
  }, [accounts, payerId]);

  const validate = () => {
    const errors = {};
    const amount = Number(form.amount);
    if (!form.sourceAccountId) errors.sourceAccountId = "Select source account";
    if (
      form.sourceAccountId &&
      !ownedAccounts.some((account) => account.accountId === form.sourceAccountId)
    ) {
      errors.sourceAccountId = "Select a source account owned by the current user";
    }
    if (!form.beneficiaryId) errors.beneficiaryId = "Select beneficiary";
    if (!form.amount) {
      errors.amount = "Amount is required";
    } else if (!/^\d+(\.\d{1,2})?$/.test(form.amount)) {
      errors.amount = "Maximum 2 decimal places allowed";
    } else if (!(amount > 0)) {
      errors.amount = "Amount must be greater than 0";
    } else if (amount > 1000000) {
      errors.amount = "Amount cannot exceed 1000000";
    }
    if (!form.reference.trim()) errors.reference = "Reference is required";
    if (!form.currency) errors.currency = "Currency is required";
    if (!form.paymentType) errors.paymentType = "Payment type is required";
    if (form.paymentType === "BILL_PAYMENT" && !form.invoiceId.trim())
      errors.invoiceId = "Invoice ID is required for bill payment";
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    const retryKey = idempotencyKey || uuidv4();
    if (!idempotencyKey) setIdempotencyKey(retryKey);
    const body = {
      amount: Number(form.amount),
      currency: form.currency,
      reference: form.reference.trim(),
      beneficiaryId: form.beneficiaryId,
      sourceAccountId: form.sourceAccountId,
      paymentType: form.paymentType,
      ...(form.paymentType === "BILL_PAYMENT" ? { invoiceId: form.invoiceId.trim() } : {}),
    };
    try {
      const response = await api.post("/api/payments", body, {
        headers: { "Idempotency-Key": retryKey },
      });
      setCreatedPayment(response.data);
      toast.success("Payment created successfully");
      setIdempotencyKey("");
      setForm(initialPaymentForm);
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AsyncState
      loading={loading}
      error={error}
      onRetry={loadDependencies}
      isEmpty={!loading && !error && (ownedAccounts.length === 0 || beneficiaries.length === 0)}
      emptyTitle="Missing dependencies"
      emptyDescription="Ensure at least one owned source account and one beneficiary exists before payment creation."
    >
      <div className="grid gap-6 lg:grid-cols-5">
        <form
          className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-3"
          onSubmit={onSubmit}
          noValidate
        >
          <div className="grid gap-4 md:grid-cols-2">
            <label className="field md:col-span-2">
              <span>Source Account</span>
              <select
                className="input"
                value={form.sourceAccountId}
                onChange={(e) => setForm((p) => ({ ...p, sourceAccountId: e.target.value }))}
                required
              >
                <option value="">Select account</option>
                {ownedAccounts.map((a) => (
                  <option key={a.accountId} value={a.accountId}>
                    {a.accountId} - {a.accountNumber}
                  </option>
                ))}
              </select>
              {formErrors.sourceAccountId && (
                <p className="error-text">{formErrors.sourceAccountId}</p>
              )}
            </label>

            <label className="field md:col-span-2">
              <span>Beneficiary</span>
              <select
                className="input"
                value={form.beneficiaryId}
                onChange={(e) => setForm((p) => ({ ...p, beneficiaryId: e.target.value }))}
                required
              >
                <option value="">Select beneficiary</option>
                {beneficiaries.map((b) => (
                  <option key={b.beneficiaryId} value={b.beneficiaryId}>
                    {b.beneficiaryId} - {b.name}
                  </option>
                ))}
              </select>
              {formErrors.beneficiaryId && (
                <p className="error-text">{formErrors.beneficiaryId}</p>
              )}
            </label>

            <label className="field">
              <span>Amount</span>
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={form.amount}
                onChange={(e) => setForm((p) => ({ ...p, amount: e.target.value }))}
                required
              />
              {formErrors.amount && <p className="error-text">{formErrors.amount}</p>}
            </label>

            <label className="field">
              <span>Currency</span>
              <select
                className="input"
                value={form.currency}
                onChange={(e) => setForm((p) => ({ ...p, currency: e.target.value }))}
              >
                {CURRENCIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </label>

            <label className="field md:col-span-2">
              <span>Reference</span>
              <input
                className="input"
                value={form.reference}
                onChange={(e) => setForm((p) => ({ ...p, reference: e.target.value }))}
                required
              />
              {formErrors.reference && <p className="error-text">{formErrors.reference}</p>}
            </label>

            <fieldset className="field md:col-span-2">
              <legend>Payment Type</legend>
              <div className="mt-2 flex flex-wrap gap-4">
                {[
                  ["BILL_PAYMENT", "Bill Payment"],
                  ["BENEFICIARY_TRANSFER", "Beneficiary Transfer"],
                ].map(([value, label]) => (
                  <label key={value} className="inline-flex items-center gap-2 text-sm">
                    <input
                      type="radio"
                      name="paymentType"
                      value={value}
                      checked={form.paymentType === value}
                      onChange={(e) => setForm((p) => ({ ...p, paymentType: e.target.value }))}
                    />
                    {label}
                  </label>
                ))}
              </div>
            </fieldset>

            {form.paymentType === "BILL_PAYMENT" && (
              <label className="field md:col-span-2">
                <span>Invoice ID</span>
                <input
                  className="input"
                  value={form.invoiceId}
                  onChange={(e) => setForm((p) => ({ ...p, invoiceId: e.target.value }))}
                  required
                />
                {formErrors.invoiceId && <p className="error-text">{formErrors.invoiceId}</p>}
              </label>
            )}

            <label className="field md:col-span-2">
              <span>Payer ID</span>
              <input className="input" value={payerId} readOnly />
            </label>

            <div className="md:col-span-2 flex flex-wrap items-center gap-2">
              <button className="btn-primary" type="submit" disabled={submitting}>
                {submitting ? "Submitting..." : "Submit Payment"}
              </button>
              {idempotencyKey && (
                <p className="text-xs text-zinc-500 dark:text-zinc-400">
                  Retry key in use: {idempotencyKey}
                </p>
              )}
            </div>
          </div>
        </form>

        <aside className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-2">
          <h3 className="text-lg font-semibold">Last Created Payment</h3>
          {!createdPayment ? (
            <p className="mt-3 text-sm text-zinc-600 dark:text-zinc-400">
              Submit a payment to view returned details.
            </p>
          ) : (
            <dl className="mt-4 grid gap-2 text-sm">
              {[
                "paymentId",
                "status",
                "createdAt",
                "updatedAt",
                "paymentType",
                "sourceAccountId",
                "beneficiaryId",
                "payerId",
                "invoiceId",
              ].map((key) => (
                <div key={key} className="grid grid-cols-[150px_1fr] gap-2">
                  <dt className="font-semibold text-zinc-500 dark:text-zinc-400">{key}</dt>
                  <dd className="break-all">{String(createdPayment?.[key] ?? "-")}</dd>
                </div>
              ))}
            </dl>
          )}
        </aside>
      </div>
    </AsyncState>
  );
}

// =========================================================
// Source Accounts Section
// =========================================================
const initialAccountForm = { accountId: "", accountNumber: "", accountHolderName: "" };

function SourceAccountsSection() {
  const [form, setForm] = useState(initialAccountForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadAccounts = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get("/api/accounts");
      setAccounts(response.data ?? []);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  const validate = () => {
    const errors = {};
    if (!form.accountId.trim()) {
      errors.accountId = "Account ID is required — click Generate Unique ID";
    }
    if (!form.accountNumber.trim()) {
      errors.accountNumber = "Account number is required";
    } else if (!accountNumberRegex.test(form.accountNumber.trim())) {
      errors.accountNumber = "Use 6-34 alphanumeric characters";
    }
    if (!form.accountHolderName.trim()) {
      errors.accountHolderName = "Account holder name is required";
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const onCreateAccount = async (event) => {
    event.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      await api.post("/api/accounts", {
        accountId: form.accountId.trim(),
        accountNumber: form.accountNumber.trim(),
        accountHolderName: form.accountHolderName.trim(),
      });
      toast.success("Source account created");
      setForm(initialAccountForm);
      await loadAccounts();
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="mb-6 rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
        <h2 className="mb-4 text-lg font-semibold">Create Source Account</h2>
        <form className="grid gap-4 md:grid-cols-2" onSubmit={onCreateAccount} noValidate>
          <div className="field md:col-span-2">
            <span className="mb-1 block text-sm font-medium">Account ID</span>
            <div className="flex gap-2">
              <input
                value={form.accountId}
                readOnly
                placeholder="Click Generate Unique ID"
                className="input flex-1 bg-zinc-50 dark:bg-zinc-800/60"
              />
              <button
                type="button"
                className="btn-primary whitespace-nowrap"
                onClick={() =>
                  setForm((p) => ({ ...p, accountId: crypto.randomUUID() }))
                }
              >
                Generate Unique ID
              </button>
            </div>
            {formErrors.accountId && <p className="error-text">{formErrors.accountId}</p>}
          </div>
          <label className="field">
            <span>Account Number</span>
            <input
              value={form.accountNumber}
              onChange={(e) => setForm((p) => ({ ...p, accountNumber: e.target.value }))}
              placeholder="987654321012"
              className="input"
              required
            />
            {formErrors.accountNumber && (
              <p className="error-text">{formErrors.accountNumber}</p>
            )}
          </label>
          <label className="field">
            <span>Account Holder Name</span>
            <input
              value={form.accountHolderName}
              onChange={(e) => setForm((p) => ({ ...p, accountHolderName: e.target.value }))}
              placeholder="Rishabh Singh"
              className="input"
              required
            />
            {formErrors.accountHolderName && (
              <p className="error-text">{formErrors.accountHolderName}</p>
            )}
          </label>
          <div className="md:col-span-2">
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? "Creating..." : "Create Account"}
            </button>
          </div>
        </form>
      </div>

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadAccounts}
        isEmpty={!loading && !error && accounts.length === 0}
        emptyTitle="No source accounts"
        emptyDescription="Create a source account to begin processing payments."
        loadingView={<SkeletonTable rows={4} columns={4} />}
      >
        <div className="overflow-auto rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-zinc-500 dark:text-zinc-400">
                <th className="pb-2">Account ID</th>
                <th className="pb-2">Account Number</th>
                <th className="pb-2">Holder Name</th>
                <th className="pb-2">Active</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr
                  key={account.accountId}
                  className="border-t border-zinc-200/80 dark:border-zinc-800"
                >
                  <td className="py-3 font-medium">{account.accountId}</td>
                  <td className="py-3">{account.accountNumber}</td>
                  <td className="py-3">{account.accountHolderName}</td>
                  <td className="py-3">{String(account.active ?? account.isActive ?? true)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>
    </div>
  );
}

// =========================================================
// Beneficiaries Section
// =========================================================
const initialBeneficiaryForm = {
  name: "",
  accountNumber: "",
  bankName: "",
  ifscCode: "",
  email: "",
  phone: "",
};

function BeneficiariesSection() {
  const [form, setForm] = useState(initialBeneficiaryForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [beneficiaries, setBeneficiaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedBeneficiary, setSelectedBeneficiary] = useState(null);

  const loadBeneficiaries = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get("/api/beneficiaries");
      setBeneficiaries(response.data ?? []);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadBeneficiaries();
  }, [loadBeneficiaries]);

  const validate = () => {
    const errors = {};
    if (!form.name.trim()) errors.name = "Name is required";
    if (!form.accountNumber.trim()) {
      errors.accountNumber = "Account number is required";
    } else if (!accountNumberRegex.test(form.accountNumber.trim())) {
      errors.accountNumber = "Use 6-34 alphanumeric characters";
    }
    if (!form.bankName.trim()) errors.bankName = "Bank name is required";
    if (!form.ifscCode.trim()) {
      errors.ifscCode = "IFSC code is required";
    } else if (!ifscRegex.test(form.ifscCode.trim().toUpperCase())) {
      errors.ifscCode = "Format must be ABCD0XXXXXX";
    }
    if (!form.email.trim()) {
      errors.email = "Email is required";
    } else if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) {
      errors.email = "Use a valid email";
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const onCreateBeneficiary = async (event) => {
    event.preventDefault();
    if (!validate()) return;
    setSubmitting(true);
    try {
      await api.post("/api/beneficiaries", {
        ...form,
        ifscCode: form.ifscCode.trim().toUpperCase(),
      });
      toast.success("Beneficiary created");
      setForm(initialBeneficiaryForm);
      await loadBeneficiaries();
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setSubmitting(false);
    }
  };

  const onViewDetails = async (beneficiaryId) => {
    try {
      const response = await api.get(`/api/beneficiaries/${beneficiaryId}`);
      setSelectedBeneficiary(response.data);
    } catch (err) {
      toast.error(getErrorText(err));
    }
  };

  return (
    <div>
      <div className="mb-6 rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
        <h2 className="mb-4 text-lg font-semibold">Create Beneficiary</h2>
        <form className="grid gap-4 md:grid-cols-2" onSubmit={onCreateBeneficiary} noValidate>
          {[
            ["name", "Name", "Asha Sharma"],
            ["accountNumber", "Account Number", "123456789012"],
            ["bankName", "Bank Name", "Example Bank"],
            ["ifscCode", "IFSC Code", "HDFC0123456"],
            ["email", "Email", "asha@example.com"],
            ["phone", "Phone (optional)", "9999999999"],
          ].map(([key, label, placeholder]) => (
            <label className="field" key={key}>
              <span>{label}</span>
              <input
                value={form[key]}
                onChange={(e) => setForm((p) => ({ ...p, [key]: e.target.value }))}
                placeholder={placeholder}
                className="input"
                required={key !== "phone"}
              />
              {formErrors[key] && <p className="error-text">{formErrors[key]}</p>}
            </label>
          ))}
          <div className="md:col-span-2">
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? "Creating..." : "Create Beneficiary"}
            </button>
          </div>
        </form>
      </div>

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadBeneficiaries}
        isEmpty={!loading && !error && beneficiaries.length === 0}
        emptyTitle="No beneficiaries"
        emptyDescription="Add beneficiaries so they appear in payment selection."
        loadingView={<SkeletonTable rows={4} columns={6} />}
      >
        <div className="overflow-auto rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-zinc-500 dark:text-zinc-400">
                <th className="pb-2">Beneficiary ID</th>
                <th className="pb-2">Name</th>
                <th className="pb-2">Account Number</th>
                <th className="pb-2">Bank</th>
                <th className="pb-2">IFSC</th>
                <th className="pb-2">Action</th>
              </tr>
            </thead>
            <tbody>
              {beneficiaries.map((b) => (
                <tr
                  key={b.beneficiaryId}
                  className="border-t border-zinc-200/80 dark:border-zinc-800"
                >
                  <td className="py-3 font-medium">{b.beneficiaryId}</td>
                  <td className="py-3">{b.name}</td>
                  <td className="py-3">{b.accountNumber}</td>
                  <td className="py-3">{b.bankName}</td>
                  <td className="py-3">{b.ifscCode}</td>
                  <td className="py-3">
                    <button
                      type="button"
                      className="btn-outline"
                      onClick={() => onViewDetails(b.beneficiaryId)}
                    >
                      Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      {selectedBeneficiary && (
        <div className="fixed inset-0 z-40 grid place-items-center bg-black/50 p-4">
          <div className="w-full max-w-xl rounded-2xl border border-zinc-300 bg-white p-6 shadow-xl dark:border-zinc-700 dark:bg-zinc-900">
            <h3 className="text-lg font-semibold">Beneficiary Details</h3>
            <dl className="mt-4 grid gap-2 text-sm">
              {Object.entries(selectedBeneficiary).map(([key, value]) => (
                <div key={key} className="grid grid-cols-[160px_1fr] gap-3">
                  <dt className="font-semibold text-zinc-500 dark:text-zinc-400">{key}</dt>
                  <dd className="break-all">{String(value ?? "-")}</dd>
                </div>
              ))}
            </dl>
            <button
              type="button"
              className="btn-primary mt-5"
              onClick={() => setSelectedBeneficiary(null)}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// =========================================================
// PaymentPage (main)
// =========================================================
export default function PaymentPage() {
  const [activeTab, setActiveTab] = useState("create");

  return (
    <div>
      <PageHeader
        title="Payment"
        description="Create payments, manage source accounts and beneficiaries."
      />

      <div className="mb-6 flex flex-wrap gap-2">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setActiveTab(tab.key)}
            className={activeTab === tab.key ? "btn-primary" : "btn-outline"}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "create" && <CreatePaymentSection />}
      {activeTab === "accounts" && <SourceAccountsSection />}
      {activeTab === "beneficiaries" && <BeneficiariesSection />}
    </div>
  );
}
