import { useState } from 'react';
import { useNavigate, useLocation, useSearchParams, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, CheckCircle, Eye, EyeOff } from 'lucide-react';
import { motion } from 'motion/react';
import { useAuth } from '../contexts/AuthContext';
import { loginSchema, LoginFormData } from '../schemas/auth.schemas';
import { getApiErrorMessage, getApiErrorStatus } from '../utils/errorUtils';
import { GoogleAuthButton, GoogleDivider } from '../components/GoogleAuthButton';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function LoginPage() {
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [isUnverified, setIsUnverified] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  const verified = searchParams.get('verified') === 'true';
  const reset = searchParams.get('reset') === 'true';
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/dashboard';

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (data: LoginFormData) => {
    setServerError('');
    setIsUnverified(false);
    setLoading(true);
    try {
      await login(data);
      navigate(from, { replace: true });
    } catch (err: unknown) {
      if (getApiErrorStatus(err) === 403) {
        setIsUnverified(true);
        setServerError('Votre compte n\'est pas encore vérifié. Consultez votre email.');
      } else {
        setServerError(getApiErrorMessage(err, 'Email ou mot de passe invalide'));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
        className="w-full max-w-[420px]"
      >
        <div className="rounded-[20px] border p-10">
          {/* Header */}
          <div className="text-center mb-7">
            <h1 className="font-display text-[28px] font-bold tracking-tight">Shelfio</h1>
            <p className="text-base text-muted-foreground mt-2">Connexion à votre compte</p>
          </div>

          {verified && (
            <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-xl px-4 py-3 text-green-700 dark:text-green-400 text-sm flex items-center gap-2 mb-4">
              <CheckCircle className="h-4 w-4 flex-shrink-0" />
              Email vérifié avec succès ! Vous pouvez maintenant vous connecter.
            </div>
          )}

          {reset && (
            <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-xl px-4 py-3 text-green-700 dark:text-green-400 text-sm flex items-center gap-2 mb-4">
              <CheckCircle className="h-4 w-4 flex-shrink-0" />
              Mot de passe réinitialisé avec succès !
            </div>
          )}

          {serverError && (
            <div className="bg-destructive/10 border border-destructive/20 rounded-xl px-4 py-3 text-destructive text-sm mb-4">
              {serverError}
              {isUnverified && (
                <Link to="/verify-email" className="block mt-1 font-medium underline">
                  Renvoyer l'email de vérification
                </Link>
              )}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email" className="text-[13px] font-medium">Email</Label>
              <Input
                id="email"
                type="email"
                {...register('email')}
                className={`rounded-xl bg-card ${errors.email ? 'border-destructive focus-visible:ring-destructive' : 'border-transparent'}`}
                placeholder="vous@exemple.com"
                aria-invalid={!!errors.email}
                aria-describedby={errors.email ? 'email-error' : undefined}
              />
              {errors.email && (
                <p id="email-error" role="alert" className="text-sm text-destructive flex items-center gap-1.5">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  {errors.email.message}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <Label htmlFor="password" className="text-[13px] font-medium">Mot de passe</Label>
                <Link
                  to="/forgot-password"
                  className="text-[13px] font-medium text-brand hover:opacity-80 transition-opacity"
                >
                  Mot de passe oublié ?
                </Link>
              </div>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  {...register('password')}
                  className={`rounded-xl bg-card pr-10 ${errors.password ? 'border-destructive focus-visible:ring-destructive' : 'border-transparent'}`}
                  placeholder="Entrez votre mot de passe"
                  aria-invalid={!!errors.password}
                  aria-describedby={errors.password ? 'password-error' : undefined}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                  aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {errors.password && (
                <p id="password-error" role="alert" className="text-sm text-destructive flex items-center gap-1.5">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  {errors.password.message}
                </p>
              )}
            </div>

            <div className="space-y-3 pt-3">
              <Button
                type="submit"
                disabled={loading}
                className="w-full rounded-xl h-[46px] text-[15px] font-semibold"
              >
                {loading ? (
                  <span className="flex items-center justify-center gap-2">
                    <svg aria-hidden="true" className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    Connexion...
                  </span>
                ) : (
                  'Connexion'
                )}
              </Button>

              <GoogleDivider />
              <GoogleAuthButton
                label="Continuer avec Google"
                onSuccess={() => navigate(from, { replace: true })}
                onError={(msg) => setServerError(msg)}
                disabled={loading}
              />
            </div>
          </form>

          <p className="mt-7 text-center text-sm text-muted-foreground">
            Pas encore de compte ?{' '}
            <Link to="/signup" className="text-brand font-semibold hover:opacity-80 transition-opacity">
              Créer un compte
            </Link>
          </p>
        </div>
      </motion.div>
    </div>
  );
}
