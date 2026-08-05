import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import { api, getErrorText } from "../lib/api";
import { NEXT_STATUS_FLOW } from "../lib/constants";
import { formatDateTime } from "../lib/formatters";

export default function PaymentDetailsPage() {
  const { paymentId } = useParams();
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [nextStatus, setNextStatus] = useState("");
  const [remarks, setRemarks] = useState("");
  const [errorCode, setErrorCode] = useState("");
  const [actor, setActor] = useState("PAYMENT_OPERATIONS");
  const [updating, setUpdating] = useState(false);

  const loadPayment = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get(`/api/payments/${paymentId}`);
      setPayment(response.data);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, [paymentId]);

  useEffect(() => {
    loadPayment();
  }, [loadPayment]);

  const allowedNextStatuses = useMemo(
    () => NEXT_STATUS_FLOW[payment?.status] ?? [],
    [payment?.status]
  );

  useEffect(() => {
    setNextStatus(allowedNextStatuses[0] ?? "");
  }, [allowedNextStatuses]);

  const onStatusUpdate = async (status) => {
    const targetStatus = status || nextStatus;
    if (!targetStatus) {
      return;
    }

    if (targetStatus === "FAILED" && !errorCode.trim()) {
      toast.error("errorCode is required when status is FAILED");
      return;
    }

    const confirmed = window.confirm(`Change status to ${targetStatus}?`);
    if (!confirmed) {
      return;
    }

    setUpdating(true);
    try {
      await api.patch(`/api/payments/${paymentId}/status`, {
        status: targetStatus,
        remarks: remarks.trim() || null,
        errorCode: targetStatus === "FAILED" ? errorCode.trim() : null,
        actor: actor.trim() || "PAYMENT_OPERATIONS",
      });
      toast.success("Payment status updated");
      await loadPayment();
    } catch (err) {
      toast.error(getErrorText(err));
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div>
      <PageHeader
        title={`Payment Details - ${paymentId}`}
        description="Inspect payment payload and perform valid status transitions."
        actions={
          <Link className="btn-outline" to={`/payment-history/${paymentId}/history`}>
            View History
          </Link>
        }
      />

      <AsyncState loading={loading} error={error} onRetry={loadPayment}>
        <div className="grid gap-6 lg:grid-cols-5">
          <section className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-3">
            <h2 className="mb-4 text-lg font-semibold">Payment Snapshot</h2>
            <dl className="grid gap-2 text-sm">
              {Object.entries(payment ?? {}).map(([key, value]) => (
                <div key={key} className="grid grid-cols-[180px_1fr] gap-3 border-b border-zinc-200/60 py-2 dark:border-zinc-800">
                  <dt className="font-semibold text-zinc-500 dark:text-zinc-400">{key}</dt>
                  <dd className="break-all">
                    {key.toLowerCase().includes("at")
                      ? formatDateTime(value)
                      : key === "status"
                        ? <StatusBadge status={value} />
                        : String(value ?? "-")}
                  </dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="rounded-2xl border border-zinc-300/70 bg-white/80 p-5 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70 lg:col-span-2">
            <h2 className="mb-4 text-lg font-semibold">Update Status</h2>
            {allowedNextStatuses.length === 0 ? (
              <p className="text-sm text-zinc-600 dark:text-zinc-400">
                No valid transitions available for {payment?.status}.
              </p>
            ) : (
              <div className="space-y-4">
                <label className="field">
                  <span>Status</span>
                  <select
                    className="input"
                    value={nextStatus}
                    onChange={(event) => setNextStatus(event.target.value)}
                  >
                    {allowedNextStatuses.map((value) => (
                      <option key={value} value={value}>
                        {value}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field">
                  <span>Remarks</span>
                  <textarea
                    className="input min-h-20"
                    value={remarks}
                    onChange={(event) => setRemarks(event.target.value)}
                  />
                </label>

                {nextStatus === "FAILED" && (
                  <label className="field">
                    <span>Error Code</span>
                    <input
                      className="input"
                      value={errorCode}
                      onChange={(event) => setErrorCode(event.target.value)}
                      required
                    />
                  </label>
                )}

                <label className="field">
                  <span>Actor</span>
                  <input
                    className="input"
                    value={actor}
                    onChange={(event) => setActor(event.target.value)}
                  />
                </label>

                <div className="flex flex-wrap gap-2">
                  {allowedNextStatuses.map((value) => (
                    <button
                      key={value}
                      type="button"
                      className="btn-primary"
                      onClick={() => onStatusUpdate(value)}
                      disabled={updating}
                    >
                      {updating ? "Updating..." : `Set ${value}`}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </section>
        </div>
      </AsyncState>
    </div>
  );
}
