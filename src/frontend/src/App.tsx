import { lazy, Suspense } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import Layout from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import Dashboard from "./pages/Dashboard";

const LandingPage = lazy(() => import("./pages/LandingPage"));
const ListsPage = lazy(() => import("./pages/ListsPage"));
const ListDetail = lazy(() => import("./pages/ListDetail"));
const ListForm = lazy(() => import("./pages/ListForm"));
const ItemForm = lazy(() => import("./pages/ItemForm"));
const UsersPage = lazy(() =>
  import("./pages/admin/UsersPage").then((m) => ({ default: m.UsersPage })),
);
const UserDetailPage = lazy(() =>
  import("./pages/admin/UserDetailPage").then((m) => ({
    default: m.UserDetailPage,
  })),
);
const AdminListsPage = lazy(() =>
  import("./pages/admin/AdminListsPage").then((m) => ({
    default: m.AdminListsPage,
  })),
);
const AdminListDetailPage = lazy(() =>
  import("./pages/admin/AdminListDetailPage").then((m) => ({
    default: m.AdminListDetailPage,
  })),
);
const AdminDashboardPage = lazy(() =>
  import("./pages/admin/AdminDashboardPage").then((m) => ({
    default: m.AdminDashboardPage,
  })),
);
const SettingsPage = lazy(() => import("./pages/SettingsPage"));
const UpgradePage = lazy(() => import("./pages/UpgradePage"));
const PaymentSuccess = lazy(() => import("./pages/PaymentSuccess"));
const PaymentCancel = lazy(() => import("./pages/PaymentCancel"));
const NotFound = lazy(() => import("./pages/NotFound"));
const PrivacyPolicy = lazy(() =>
  import("./pages/PrivacyPolicy").then((m) => ({ default: m.PrivacyPolicy })),
);
const TermsOfService = lazy(() =>
  import("./pages/TermsOfService").then((m) => ({ default: m.TermsOfService })),
);
const MentionsLegales = lazy(() =>
  import("./pages/MentionsLegales").then((m) => ({
    default: m.MentionsLegales,
  })),
);
const VerifyEmail = lazy(() =>
  import("./pages/VerifyEmail").then((m) => ({ default: m.VerifyEmail })),
);
const ForgotPassword = lazy(() =>
  import("./pages/ForgotPassword").then((m) => ({ default: m.ForgotPassword })),
);
const ResetPassword = lazy(() =>
  import("./pages/ResetPassword").then((m) => ({ default: m.ResetPassword })),
);

export default function App() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center bg-background">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-muted border-t-foreground" />
        </div>
      }
    >
      <Routes>
        {/* Public routes */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/privacy" element={<PrivacyPolicy />} />
        <Route path="/terms" element={<TermsOfService />} />
        <Route path="/mentions-legales" element={<MentionsLegales />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        {/* Protected routes */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Layout>
                <Dashboard />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/lists"
          element={
            <ProtectedRoute>
              <Layout>
                <ListsPage />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/lists/new"
          element={
            <ProtectedRoute>
              <Layout>
                <ListForm />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/lists/:id"
          element={
            <ProtectedRoute>
              <Layout>
                <ListDetail />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/lists/:id/edit"
          element={
            <ProtectedRoute>
              <Layout>
                <ListForm />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/lists/:listId/items/new"
          element={
            <ProtectedRoute>
              <Layout>
                <ItemForm />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/lists/:listId/items/:itemId/edit"
          element={
            <ProtectedRoute>
              <Layout>
                <ItemForm />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/settings"
          element={
            <ProtectedRoute>
              <Layout>
                <SettingsPage />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/upgrade"
          element={
            <ProtectedRoute>
              <Layout>
                <UpgradePage />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/payment/success"
          element={
            <ProtectedRoute>
              <Layout>
                <PaymentSuccess />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/payment/cancel"
          element={
            <ProtectedRoute>
              <Layout>
                <PaymentCancel />
              </Layout>
            </ProtectedRoute>
          }
        />

        {/* Admin routes */}
        <Route
          path="/admin/stats"
          element={
            <ProtectedRoute adminOnly>
              <Layout>
                <AdminDashboardPage />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute adminOnly>
              <Layout>
                <UsersPage />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/users/:id"
          element={
            <ProtectedRoute adminOnly>
              <Layout>
                <UserDetailPage />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/lists"
          element={
            <ProtectedRoute adminOnly>
              <Layout>
                <AdminListsPage />
              </Layout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/lists/:id"
          element={
            <ProtectedRoute adminOnly>
              <Layout>
                <AdminListDetailPage />
              </Layout>
            </ProtectedRoute>
          }
        />

        {/* Redirect old routes */}
        <Route path="/inventory" element={<Navigate to="/lists" replace />} />
        <Route path="/inventory/*" element={<Navigate to="/lists" replace />} />

        {/* 404 catch-all */}
        <Route
          path="*"
          element={
            <ProtectedRoute>
              <Layout>
                <NotFound />
              </Layout>
            </ProtectedRoute>
          }
        />
      </Routes>
    </Suspense>
  );
}
