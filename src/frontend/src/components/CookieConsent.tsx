import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import * as Sentry from '@sentry/react';

const COOKIE_CONSENT_KEY = 'cookie_consent';

export function getCookieConsent(): string | null {
  return localStorage.getItem(COOKIE_CONSENT_KEY);
}

export function initSentryIfConsented() {
  const sentryDsn = import.meta.env.VITE_SENTRY_DSN;
  if (sentryDsn && getCookieConsent() === 'accepted') {
    Sentry.init({
      dsn: sentryDsn,
      integrations: [Sentry.browserTracingIntegration()],
      tracesSampleRate: 0.1,
    });
  }
}

export function CookieConsent() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (!getCookieConsent()) {
      setVisible(true);
    }
  }, []);

  if (!visible) return null;

  const handleAccept = () => {
    localStorage.setItem(COOKIE_CONSENT_KEY, 'accepted');
    setVisible(false);
    initSentryIfConsented();
  };

  const handleRefuse = () => {
    localStorage.setItem(COOKIE_CONSENT_KEY, 'refused');
    setVisible(false);
  };

  return (
    <div className="fixed bottom-0 inset-x-0 z-50 p-4">
      <div className="mx-auto max-w-2xl rounded-xl border bg-background/95 backdrop-blur-sm p-4 shadow-lg">
        <p className="text-sm text-muted-foreground mb-3">
          Ce site utilise un cookie d'authentification (strictement nécessaire) et un cookie de
          suivi d'erreurs (Sentry) pour améliorer la qualité du service.{' '}
          <Link to="/privacy" className="text-foreground underline underline-offset-2">
            Politique de confidentialité
          </Link>
        </p>
        <div className="flex gap-2 justify-end">
          <button
            onClick={handleRefuse}
            className="px-4 py-2 text-sm font-medium rounded-lg border text-muted-foreground hover:text-foreground hover:bg-secondary transition-colors"
          >
            Refuser
          </button>
          <button
            onClick={handleAccept}
            className="px-4 py-2 text-sm font-medium rounded-lg bg-foreground text-background hover:bg-foreground/90 transition-colors"
          >
            Accepter
          </button>
        </div>
      </div>
    </div>
  );
}
