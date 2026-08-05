import { useCallback, useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import SkeletonTable from "../components/SkeletonTable";
import { useCurrentUser } from "../context/UserContext";
import { api, getErrorText } from "../lib/api";
import { formatCurrency, formatDateTime, toSentenceCase } from "../lib/formatters";

export default function UserProfilePage() {
  const { currentUser } = useCurrentUser();
  const payerId = currentUser?.payerId ?? null;

  const [accounts, setAccounts] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadProfileData = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [accountsResponse, paymentsResponse] = await Promise.all([
        api.get("/api/accounts"),
        api.get("/api/payments?page=0&size=200&sort=createdAt,desc"),
      ]);

      const accountRows = Array.isArray(accountsResponse.data) ? accountsResponse.data : [];
      const ownedAccounts = accountRows.filter((account) => {
        if (!payerId) {
          return true;
        }
        const owner = account.payerId ?? account.ownerPayerId ?? account.userPayerId ?? null;
        return !owner || String(owner) === String(payerId);
      });
      setAccounts(ownedAccounts);

      const paymentRows = paymentsResponse.data?.content ?? [];
      const ownedPayments = paymentRows.filter((payment) => {
        if (!payerId) {
          return true;
        }
        return !payment.payerId || String(payment.payerId) === String(payerId);
      });
      setPayments(ownedPayments);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, [payerId]);

  useEffect(() => {
    loadProfileData();
  }, [loadProfileData]);

  const totalBalanceInInr = useMemo(() => {
    return accounts.reduce((sum, account) => sum + (Number(account.balanceInInr) || 0), 0);
  }, [accounts]);

  const activeAccounts = useMemo(() => {
    return accounts.filter((account) => Boolean(account.active ?? account.isActive ?? true)).length;
  }, [accounts]);

  const paymentStats = useMemo(() => {
    return payments.reduce(
      (acc, payment) => {
        const status = String(payment.status || "").toUpperCase();
        if (status === "SENT") acc.sent += 1;
        if (status === "COMPLETED") acc.completed += 1;
        if (status === "FAILED") acc.failed += 1;
        return acc;
      },
      { sent: 0, completed: 0, failed: 0 }
    );
  }, [payments]);

  return (
    <div>
      <PageHeader
        title="User Profile"
        description="Account portfolio, INR balances, and payment health for the signed-in payer."
      />

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadProfileData}
        isEmpty={!loading && !error && accounts.length === 0}
        emptyTitle="No accounts found"
        emptyDescription="Create at least one source account to view your profile portfolio."
        loadingView={<SkeletonTable rows={4} columns={4} />}
      >
        <section className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <article className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
            <p className="text-xs uppercase tracking-[0.18em] text-zinc-500 dark:text-zinc-400">Payer ID</p>
            <p className="mt-2 break-all font-mono text-sm text-zinc-800 dark:text-zinc-200">{payerId || "-"}</p>
          </article>
          <article className="rounded-2xl border border-emerald-300/70 bg-emerald-50/70 p-5 shadow-sm dark:border-emerald-800/70 dark:bg-emerald-900/20">
            <p className="text-xs uppercase tracking-[0.18em] text-emerald-700 dark:text-emerald-300">Total Balance (INR)</p>
            <p className="mt-2 text-2xl font-semibold text-emerald-800 dark:text-emerald-200">{formatCurrency(totalBalanceInInr, "INR")}</p>
          </article>
          <article className="rounded-2xl border border-sky-300/70 bg-sky-50/70 p-5 shadow-sm dark:border-sky-800/70 dark:bg-sky-900/20">
            <p className="text-xs uppercase tracking-[0.18em] text-sky-700 dark:text-sky-300">Accounts</p>
            <p className="mt-2 text-2xl font-semibold text-sky-800 dark:text-sky-200">{accounts.length}</p>
            <p className="text-xs text-sky-700/80 dark:text-sky-300/80">Active: {activeAccounts}</p>
          </article>
          <article className="rounded-2xl border border-amber-300/70 bg-amber-50/70 p-5 shadow-sm dark:border-amber-800/70 dark:bg-amber-900/20">
            <p className="text-xs uppercase tracking-[0.18em] text-amber-700 dark:text-amber-300">Payments</p>
            <p className="mt-2 text-sm text-amber-800 dark:text-amber-200">
              Sent {paymentStats.sent} · Completed {paymentStats.completed} · Failed {paymentStats.failed}
            </p>
          </article>
        </section>

        <section className="mb-6 overflow-auto rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <h2 className="mb-4 text-lg font-semibold">Source Accounts</h2>
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-zinc-500 dark:text-zinc-400">
                <th className="pb-2 pr-4">Account Number</th>
                <th className="pb-2 pr-4">Holder Name</th>
                <th className="pb-2 pr-4">Type</th>
                <th className="pb-2 pr-4">Balance (INR)</th>
                <th className="pb-2">Status</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.accountId} className="border-t border-zinc-200/80 dark:border-zinc-800">
                  <td className="py-3 pr-4 font-medium">{account.accountNumber}</td>
                  <td className="py-3 pr-4">{account.accountHolderName}</td>
                  <td className="py-3 pr-4">{toSentenceCase(account.accountType || "SAVINGS")}</td>
                  <td className="py-3 pr-4">{formatCurrency(account.balanceInInr, "INR")}</td>
                  <td className="py-3">{String(account.active ?? account.isActive ?? true) === "true" ? "Active" : "Inactive"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section className="overflow-auto rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <h2 className="mb-4 text-lg font-semibold">Recent Payments</h2>
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-left text-zinc-500 dark:text-zinc-400">
                <th className="pb-2 pr-4">Payment ID</th>
                <th className="pb-2 pr-4">Amount</th>
                <th className="pb-2 pr-4">Currency</th>
                <th className="pb-2 pr-4">Method</th>
                <th className="pb-2 pr-4">Status</th>
                <th className="pb-2">Created</th>
              </tr>
            </thead>
            <tbody>
              {payments.slice(0, 10).map((payment) => (
                <tr key={payment.paymentId} className="border-t border-zinc-200/80 dark:border-zinc-800">
                  <td className="py-3 pr-4 font-medium">{payment.paymentId}</td>
                  <td className="py-3 pr-4">{formatCurrency(payment.amount, payment.currency)}</td>
                  <td className="py-3 pr-4">{payment.currency}</td>
                  <td className="py-3 pr-4">{toSentenceCase(payment.paymentMethod)}</td>
                  <td className="py-3 pr-4">{payment.status}</td>
                  <td className="py-3">{formatDateTime(payment.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </AsyncState>
    </div>
  );
}
