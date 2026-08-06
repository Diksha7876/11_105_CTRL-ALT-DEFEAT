export default function BlockingIssuesModal({ open, title = "Cannot submit payment", issues = [], onClose }) {
  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="dialog"
      aria-modal="true"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md rounded-2xl border border-zinc-300/70 bg-white p-5 shadow-xl dark:border-zinc-700 dark:bg-zinc-900"
        onClick={(event) => event.stopPropagation()}
      >
        <h3 className="text-lg font-semibold text-rose-600 dark:text-rose-400">{title}</h3>
        <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-zinc-700 dark:text-zinc-300">
          {issues.map((issue, index) => (
            <li key={index}>{issue}</li>
          ))}
        </ul>
        <div className="mt-5 flex justify-end">
          <button type="button" className="btn-primary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
