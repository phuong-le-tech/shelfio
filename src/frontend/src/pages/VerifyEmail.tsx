import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { motion } from 'motion/react';
import { Mail, CheckCircle, XCircle, Loader2 } from 'lucide-react';
import { authApi } from '../services/authApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { getApiErrorMessage } from '@/utils/errorUtils';

type Status = 'pending' | 'verifying' | 'success' | 'error';

export function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [status, setStatus] = useState<Status>(token ? 'verifying' : 'pending');
  const [error, setError] = useState('');
  const [resendEmail, setResendEmail] = useState('');
  const [resendSent, setResendSent] = useState(false);
  const [resendLoading, setResendLoading] = useState(false);

  useEffect(() => {
    if (token) {
      authApi.verifyEmail(token)
        .then(() => setStatus('success'))
        .catch((err) => {
          setStatus('error');
          setError(getApiErrorMessage(err, 'Le lien de vérification est invalide ou a expiré.'));
        });
    }
  }, [token]);

  const handleResend = async () => {
    if (!resendEmail) return;
    setResendLoading(true);
    try {
      await authApi.resendVerification(resendEmail);
      setResendSent(true);
    } catch {
      // Always show success to prevent email enumeration
      setResendSent(true);
    } finally {
      setResendLoading(false);
    }
  };

  const resendSection = (
    <div className="rounded-xl bg-card p-5 text-left mt-6">
      <p className="text-sm text-muted-foreground mb-3">
        {status === 'pending'
          ? "Vous n'avez pas reçu l'email ? Entrez votre adresse pour le renvoyer :"
          : 'Entrez votre email pour recevoir un nouveau lien de vérification :'}
      </p>
      {resendSent ? (
        <p className="text-sm text-green-600 dark:text-green-400">
          Si un compte existe avec cet email, un nouveau lien de vérification a été envoyé.
        </p>
      ) : (
        <div className="flex gap-2">
          <Input
            type="email"
            placeholder="vous@exemple.com"
            value={resendEmail}
            onChange={(e) => setResendEmail(e.target.value)}
            className="rounded-xl bg-card border border-border"
          />
          <Button
            onClick={handleResend}
            disabled={resendLoading || !resendEmail}
            className="rounded-xl text-[13px] font-medium shrink-0"
          >
            {resendLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : 'Renvoyer'}
          </Button>
        </div>
      )}
    </div>
  );

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
        className="w-full max-w-[420px]"
      >
        <div className="rounded-[20px] border p-10">
          {status === 'verifying' && (
            <div className="text-center">
              <div className="w-14 h-14 rounded-2xl bg-brand/10 flex items-center justify-center mx-auto mb-4">
                <Loader2 className="h-7 w-7 animate-spin text-brand" />
              </div>
              <h2 className="font-display text-[24px] font-bold tracking-tight mb-2">Vérification en cours...</h2>
              <p className="text-sm text-muted-foreground">Veuillez patienter pendant que nous vérifions votre email.</p>
            </div>
          )}

          {status === 'success' && (
            <div className="text-center">
              <div className="w-14 h-14 rounded-2xl bg-green-50 dark:bg-green-900/20 flex items-center justify-center mx-auto mb-4">
                <CheckCircle className="h-7 w-7 text-green-600 dark:text-green-400" />
              </div>
              <h2 className="font-display text-[24px] font-bold tracking-tight mb-2">Email vérifié !</h2>
              <p className="text-sm text-muted-foreground mb-6">
                Votre adresse email a été vérifiée avec succès. Vous pouvez maintenant vous connecter.
              </p>
              <Button asChild className="w-full rounded-xl h-[46px] text-[15px] font-semibold">
                <Link to="/login?verified=true">Se connecter</Link>
              </Button>
            </div>
          )}

          {status === 'error' && (
            <div className="text-center">
              <div className="w-14 h-14 rounded-2xl bg-destructive/10 flex items-center justify-center mx-auto mb-4">
                <XCircle className="h-7 w-7 text-destructive" />
              </div>
              <h2 className="font-display text-[24px] font-bold tracking-tight mb-2">Échec de la vérification</h2>
              <p className="text-sm text-muted-foreground">{error}</p>
              {resendSection}
            </div>
          )}

          {status === 'pending' && (
            <div className="text-center">
              <div className="w-14 h-14 rounded-2xl bg-brand/10 flex items-center justify-center mx-auto mb-4">
                <Mail className="h-7 w-7 text-brand" />
              </div>
              <h2 className="font-display text-[24px] font-bold tracking-tight mb-2">Vérifiez votre email</h2>
              <p className="text-sm text-muted-foreground">
                Un email de vérification a été envoyé à votre adresse. Cliquez sur le lien dans l'email pour activer votre compte.
              </p>
              {resendSection}
            </div>
          )}

          {(status === 'error' || status === 'pending') && (
            <p className="mt-7 text-center text-sm text-muted-foreground">
              <Link to="/login" className="text-brand font-semibold hover:opacity-80 transition-opacity">
                ← Retour à la connexion
              </Link>
            </p>
          )}
        </div>
      </motion.div>
    </div>
  );
}
