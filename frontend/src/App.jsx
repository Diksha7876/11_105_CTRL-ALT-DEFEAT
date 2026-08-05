import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout";
import { UserProvider } from "./context/UserContext";
import DashboardPage from "./pages/DashboardPage";
import IncomingPaymentsPage from "./pages/IncomingPaymentsPage";
import NotFoundPage from "./pages/NotFoundPage";
import PaymentDetailsPage from "./pages/PaymentDetailsPage";
import PaymentHistoryPage from "./pages/PaymentHistoryPage";
import PaymentPage from "./pages/PaymentPage";
import PaymentsPage from "./pages/PaymentsPage";

function App() {
  return (
    <UserProvider>
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="payment" element={<PaymentPage />} />
        <Route path="incoming-payments" element={<IncomingPaymentsPage />} />
        <Route path="payment-history" element={<PaymentsPage />} />
        <Route path="payment-history/:paymentId" element={<PaymentDetailsPage />} />
        <Route path="payment-history/:paymentId/history" element={<PaymentHistoryPage />} />
        <Route path="dashboard" element={<Navigate to="/" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
    </UserProvider>
  );
}

export default App;
