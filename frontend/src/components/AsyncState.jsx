export default function AsyncState({
  loading,
  error,
  isEmpty,
  onRetry,
  loadingView,
  emptyTitle = "No data found",
  emptyDescription = "Try adjusting filters or create a new record.",
  children,
}) {
  if (loading) {
    return loadingView ?? (
      <div className="rounded-xl border border-zinc-300/60 bg-white/70 p-6 text-sm text-zinc-600 shadow-sm dark:border-zinc-700 dark:bg-zinc-900/60 dark:text-zinc-300">
        Loading data...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-rose-300 bg-rose-50 p-6 dark:border-rose-900 dark:bg-rose-950/50">
        <p className="text-sm font-semibold text-rose-900 dark:text-rose-100">
          Unable to load data
        </p>
        <p className="mt-2 text-sm text-rose-700 dark:text-rose-300">{error}</p>
        {onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="mt-4 rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700"
          >
            Retry
          </button>
        )}
      </div>
    );
  }

  if (isEmpty) {
    return (
      <div className="rounded-xl border border-dashed border-zinc-300 bg-white/70 p-8 text-center dark:border-zinc-700 dark:bg-zinc-900/60">
        <p className="text-base font-semibold text-zinc-900 dark:text-zinc-100">
          {emptyTitle}
        </p>
        <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-400">
          {emptyDescription}
        </p>
      </div>
    );
  }

  return children;
}
