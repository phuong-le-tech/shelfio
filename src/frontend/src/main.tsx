import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { GoogleOAuthProvider } from "@react-oauth/google";
import * as Sentry from "@sentry/react";
import App from "./App";
import { ErrorBoundary } from "./components/ErrorBoundary";
import { ToastProvider } from "./components/Toast";
import { AuthProvider } from "./contexts/AuthContext";
import "./index.css";

const sentryDsn = import.meta.env.VITE_SENTRY_DSN;
if (sentryDsn) {
  Sentry.init({
    dsn: sentryDsn,
    integrations: [Sentry.browserTracingIntegration()],
    tracesSampleRate: 0.1,
  });
}

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || "";

const appContent = (
  <BrowserRouter>
    <AuthProvider>
      <ToastProvider>
        <App />
      </ToastProvider>
    </AuthProvider>
  </BrowserRouter>
);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ErrorBoundary>
      {googleClientId ? (
        <GoogleOAuthProvider clientId={googleClientId}>
          {appContent}
        </GoogleOAuthProvider>
      ) : (
        appContent
      )}
    </ErrorBoundary>
  </React.StrictMode>,
);
