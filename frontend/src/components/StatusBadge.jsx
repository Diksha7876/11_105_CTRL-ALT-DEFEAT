import clsx from "clsx";
import { STATUS_BADGE_CLASS } from "../lib/constants";

export default function StatusBadge({ status }) {
  return (
    <span
      className={clsx(
        "inline-flex rounded-full px-2.5 py-1 text-xs font-semibold tracking-wide",
        STATUS_BADGE_CLASS[status] ?? "bg-zinc-100 text-zinc-900"
      )}
    >
      {status || "UNKNOWN"}
    </span>
  );
}
