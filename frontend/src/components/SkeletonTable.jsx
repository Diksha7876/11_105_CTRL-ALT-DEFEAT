export default function SkeletonTable({ rows = 5, columns = 5 }) {
  return (
    <div className="overflow-hidden rounded-xl border border-zinc-300/70 dark:border-zinc-700">
      <div className="animate-pulse bg-white dark:bg-zinc-900">
        {Array.from({ length: rows }).map((_, rowIndex) => (
          <div
            key={rowIndex}
            className="grid gap-3 border-b border-zinc-200/80 p-4 dark:border-zinc-800"
            style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}
          >
            {Array.from({ length: columns }).map((__, colIndex) => (
              <span
                key={colIndex}
                className="h-4 rounded bg-zinc-200 dark:bg-zinc-700"
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
