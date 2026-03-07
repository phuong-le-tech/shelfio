import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';
import Dashboard from './pages/Dashboard';

const LandingPage = lazy(() => import('./pages/LandingPage'));
const ListsPage = lazy(() => import('./pages/ListsPage'));
const ListDetail = lazy(() => import('./pages/ListDetail'));
const ListForm = lazy(() => import('./pages/ListForm'));
const ItemForm = lazy(() => import('./pages/ItemForm'));
const UsersPage = lazy(() => import('./pages/admin/UsersPage').then(m => ({ default: m.UsersPage })));
const UpgradePage = lazy(() => import('./pages/UpgradePage'));
const PaymentSuccess = lazy(() => import('./pages/PaymentSuccess'));
const PaymentCancel = lazy(() => import('./pages/PaymentCancel'));
const NotFound = lazy(() => import('./pages/NotFound'));
const PrivacyPolicy = lazy(() => import('./pages/PrivacyPolicy').then(m => ({ default: m.PrivacyPolicy })));
const TermsOfService = lazy(() => import('./pages/TermsOfService').then(m => ({ default: m.TermsOfService })));
const VerifyEmail = lazy(() => import('./pages/VerifyEmail').then(m => ({ default: m.VerifyEmail })));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword').then(m => ({ default: m.ForgotPassword })));
const ResetPassword = lazy(() => import('./pages/ResetPassword').then(m => ({ default: m.ResetPassword })));

export default function App() {
  return (
    <Suspense>
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/privacy" element={<PrivacyPolicy />} />
      <Route path="/terms" element={<TermsOfService />} />
      <Route path="/verify-email" element={<VerifyEmail />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <Layout>
              <Routes>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/lists" element={<ListsPage />} />
                <Route path="/lists/new" element={<ListForm />} />
                <Route path="/lists/:id" element={<ListDetail />} />
                <Route path="/lists/:id/edit" element={<ListForm />} />
                <Route path="/lists/:listId/items/new" element={<ItemForm />} />
                <Route path="/lists/:listId/items/:itemId/edit" element={<ItemForm />} />
                <Route path="/upgrade" element={<UpgradePage />} />
                <Route path="/payment/success" element={<PaymentSuccess />} />
                <Route path="/payment/cancel" element={<PaymentCancel />} />
                <Route
                  path="/admin/users"
                  element={
                    <ProtectedRoute adminOnly>
                      <UsersPage />
                    </ProtectedRoute>
                  }
                />
                {/* Redirect old routes */}
                <Route path="/inventory" element={<Navigate to="/lists" replace />} />
                <Route path="/inventory/*" element={<Navigate to="/lists" replace />} />
                <Route path="*" element={<NotFound />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />
    </Routes>
    </Suspense>
  );
}
