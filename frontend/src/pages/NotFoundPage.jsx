import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="rounded-2xl border border-zinc-300/70 bg-white/80 p-8 text-center shadow-sm dark:border-zinc-700 dark:bg-zinc-900/70">
      <h1 className="text-2xl font-bold">Page not found</h1>
      <p className="mt-2 text-sm text-zinc-600 dark:text-zinc-400">
        The page you are looking for does not exist.
      </p>
      <Link className="btn-primary mt-4 inline-flex" to="/">
        Go to Dashboard
      </Link>
    </div>
  );
}
