import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import toast from "react-hot-toast";
import AsyncState from "../components/AsyncState";
import PageHeader from "../components/PageHeader";
import StatusBadge from "../components/StatusBadge";
import { api, getErrorText } from "../lib/api";
import { formatDateTime } from "../lib/formatters";

export default function PaymentHistoryPage() {
  const { paymentId } = useParams();
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await api.get(`/api/payments/${paymentId}/history`);
      setHistory(response.data ?? []);
    } catch (err) {
      const text = getErrorText(err);
      setError(text);
      toast.error(text);
    } finally {
      setLoading(false);
    }
  }, [paymentId]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  return (
    <div>
      <PageHeader
        title={`Payment History - ${paymentId}`}
        description="Timeline of status transitions and operational remarks."
        actions={
          <Link className="btn-outline" to={`/payment-history/${paymentId}`}>
            Back to Details
          </Link>
        }
      />

      <AsyncState
        loading={loading}
        error={error}
        onRetry={loadHistory}
        isEmpty={!loading && !error && history.length === 0}
        emptyTitle="No history entries"
        emptyDescription="Status transitions will appear here after updates."
      >
        <div className="rounded-2xl border border-zinc-300/70 bg-white/80 p-6 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
          <ol className="relative ml-3 border-l border-fuchsia-300 dark:border-fuchsia-800">
            {history.map((item, index) => (
              <li key={index} className="mb-8 ml-5">
                <span className="absolute -left-2.5 mt-1 h-4 w-4 rounded-full border border-fuchsia-300 bg-fuchsia-500 dark:border-fuchsia-700" />
                <div className="rounded-xl border border-zinc-200/80 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-950/60">
                  <div className="mb-2 flex flex-wrap items-center gap-2">
                    <StatusBadge status={item.oldStatus || "NA"} />
                    <span className="text-sm text-zinc-500">to</span>
                    <StatusBadge status={item.newStatus || "NA"} />
                  </div>
                  <p className="text-xs text-zinc-500 dark:text-zinc-400">
                    {formatDateTime(item.timestamp || item.updatedAt)}
                  </p>
                  <p className="mt-2 text-sm">Actor: {item.actor || "-"}</p>
                  <p className="mt-1 text-sm">Remarks: {item.remarks || "-"}</p>
                  <p className="mt-1 text-sm">Error Code: {item.errorCode || "-"}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </AsyncState>
    </div>
  );
}
