export const PAYMENT_STATUSES = [
  "CREATED",
  "VALIDATED",
  "SENT",
  "COMPLETED",
  "FAILED",
];

export const CURRENCIES = ["INR", "USD", "EUR", "GBP"];

// INR value for one unit of target currency.
export const INR_PER_CURRENCY = {
  INR: 1,
  USD: 83.1,
  EUR: 90.25,
  GBP: 105.4,
};

export const PAYMENT_TYPES = ["BILL_PAYMENT", "BENEFICIARY_TRANSFER"];

export const STATUS_BADGE_CLASS = {
  CREATED: "bg-zinc-200 text-zinc-900 dark:bg-zinc-700 dark:text-zinc-100",
  VALIDATED: "bg-blue-200 text-blue-900 dark:bg-blue-900/70 dark:text-blue-100",
  SENT: "bg-amber-200 text-amber-900 dark:bg-amber-900/70 dark:text-amber-100",
  COMPLETED: "bg-emerald-200 text-emerald-900 dark:bg-emerald-900/70 dark:text-emerald-100",
  FAILED: "bg-rose-200 text-rose-900 dark:bg-rose-900/70 dark:text-rose-100",
  RECEIVED: "bg-sky-200 text-sky-900 dark:bg-sky-900/70 dark:text-sky-100",
};

export const NEXT_STATUS_FLOW = {
  CREATED: ["VALIDATED", "FAILED"],
  VALIDATED: ["SENT", "FAILED"],
  SENT: ["COMPLETED", "FAILED"],
  COMPLETED: [],
  FAILED: [],
};
