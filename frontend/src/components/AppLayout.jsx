import { Menu, MoonStar, Sun, WalletCards, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useCurrentUser } from "../context/UserContext";
import { getStoredTheme, setStoredTheme } from "../lib/storage";

const navItems = [
  { to: "/", label: "Dashboard" },
  { to: "/payment", label: "Payment" },
  { to: "/incoming-payments", label: "Incoming Payments" },
  { to: "/payment-history", label: "Payment History" },
];

export default function AppLayout() {
  const [isSidebarOpen, setSidebarOpen] = useState(false);
  const [theme, setTheme] = useState(() => getStoredTheme() || "dark");
  const { currentUser } = useCurrentUser();

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
    setStoredTheme(theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === "dark" ? "light" : "dark"));
  };

  const rootBackground = useMemo(
    () =>
      theme === "dark"
        ? "bg-[radial-gradient(circle_at_top,_#6d28d933,_transparent_40%),radial-gradient(circle_at_80%_20%,_#7c3aed22,_transparent_35%),linear-gradient(180deg,_#0f0f12,_#18181f)]"
        : "bg-[radial-gradient(circle_at_top,_#a855f744,_transparent_40%),radial-gradient(circle_at_80%_20%,_#11182711,_transparent_35%),linear-gradient(180deg,_#faf5ff,_#ffffff)]",
    [theme]
  );

  return (
    <div className={`min-h-screen ${rootBackground} text-zinc-900 dark:text-zinc-100`}>
      <div className="mx-auto flex min-h-screen max-w-7xl">
        <aside
          className={`fixed inset-y-0 left-0 z-30 w-72 border-r border-white/15 bg-black/85 p-6 text-white backdrop-blur md:static md:translate-x-0 ${
            isSidebarOpen ? "translate-x-0" : "-translate-x-full"
          } transition-transform duration-300`}
        >
          <div className="mb-8 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="rounded-xl bg-fuchsia-500/90 p-2 text-white">
                <WalletCards size={18} />
              </span>
              <div>
                <p className="text-xs uppercase tracking-[0.24em] text-fuchsia-200">
                  Payment Hub
                </p>
                <p className="text-sm font-semibold">Processing Console</p>
              </div>
            </div>
            <button
              type="button"
              className="rounded-md p-1 hover:bg-white/10 md:hidden"
              onClick={() => setSidebarOpen(false)}
              aria-label="Close sidebar"
            >
              <X size={18} />
            </button>
          </div>

          <nav className="space-y-2">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) =>
                  `block rounded-lg px-3 py-2 text-sm transition ${
                    isActive
                      ? "bg-fuchsia-600 text-white"
                      : "text-zinc-300 hover:bg-white/10 hover:text-white"
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <div className="flex min-h-screen flex-1 flex-col">
          <header className="sticky top-0 z-20 border-b border-black/10 bg-white/80 px-4 py-3 backdrop-blur dark:border-white/10 dark:bg-zinc-950/70 md:px-8">
            <div className="flex items-center justify-between">
              <button
                type="button"
                onClick={() => setSidebarOpen(true)}
                className="rounded-md border border-zinc-300 bg-white p-2 text-zinc-700 hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200 md:hidden"
                aria-label="Open sidebar"
              >
                <Menu size={18} />
              </button>
              <div className="hidden md:flex md:flex-col md:items-start">
                <p className="text-xs uppercase tracking-[0.2em] text-fuchsia-600 dark:text-fuchsia-300">
                  Payment Processing Dashboard
                </p>
                {currentUser && (
                  <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
                    Welcome User &nbsp;·&nbsp; Payer ID:{" "}
                    <span className="font-mono text-zinc-700 dark:text-zinc-300">
                      {currentUser.payerId}
                    </span>
                  </p>
                )}
              </div>
              <button
                type="button"
                onClick={toggleTheme}
                className="inline-flex items-center gap-2 rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm font-medium text-zinc-700 hover:bg-zinc-100 dark:border-zinc-700 dark:bg-zinc-900 dark:text-zinc-200"
                aria-label="Toggle theme"
              >
                {theme === "dark" ? <Sun size={16} /> : <MoonStar size={16} />}
                {theme === "dark" ? "Light" : "Dark"}
              </button>
            </div>
          </header>

          <main className="flex-1 px-4 py-6 md:px-8 md:py-8">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
