import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, Check, Eye, EyeOff, X } from 'lucide-react';
import { motion } from 'motion/react';
import { signupSchema, SignupFormData } from '../schemas/auth.schemas';
import { getApiErrorMessage } from '../utils/errorUtils';
import { authApi } from '../services/authApi';
import { GoogleAuthButton, GoogleDivider } from '@/components/GoogleAuthButton';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function SignupPage() {
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<SignupFormData>({ resolver: zodResolver(signupSchema) });

  const password = watch('password', '');

  const onSubmit = async (data: SignupFormData) => {
    setServerError('');
    setLoading(true);
    try {
      await authApi.signup(data);
      navigate('/verify-email', { replace: true });
    } catch (err: unknown) {
      setServerError(getApiErrorMessage(err, 'Échec de la création du compte'));
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
            <p className="text-base text-muted-foreground mt-2">Créer votre compte</p>
          </div>

          {serverError && (
            <div className="bg-destructive/10 border border-destructive/20 rounded-xl px-4 py-3 text-destructive text-sm mb-4">
              {serverError}
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
              <Label htmlFor="password" className="text-[13px] font-medium">Mot de passe</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  {...register('password')}
                  className={`rounded-xl bg-card pr-10 ${errors.password ? 'border-destructive focus-visible:ring-destructive' : 'border-transparent'}`}
                  placeholder="Minimum 12 caractères"
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
              <ul className="space-y-1 text-xs" aria-label="Exigences du mot de passe">
                {[
                  { test: password.length >= 12, label: '12 caractères minimum' },
                  { test: /[a-z]/.test(password), label: 'Une lettre minuscule' },
                  { test: /[A-Z]/.test(password), label: 'Une lettre majuscule' },
                  { test: /[0-9]/.test(password), label: 'Un chiffre' },
                  { test: /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?~`]/.test(password), label: 'Un caractère spécial (!@#$%...)' },
                ].map(({ test, label }) => (
                  <li key={label} className={`flex items-center gap-1.5 ${password ? (test ? 'text-green-600 dark:text-green-400' : 'text-destructive') : 'text-muted-foreground'}`}>
                    {password ? (
                      test ? <Check className="h-3.5 w-3.5 flex-shrink-0" /> : <X className="h-3.5 w-3.5 flex-shrink-0" />
                    ) : (
                      <span className="h-3.5 w-3.5 flex-shrink-0 flex items-center justify-center">&#8226;</span>
                    )}
                    {label}
                  </li>
                ))}
              </ul>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="confirmPassword" className="text-[13px] font-medium">Confirmer le mot de passe</Label>
              <div className="relative">
                <Input
                  id="confirmPassword"
                  type={showConfirmPassword ? 'text' : 'password'}
                  {...register('confirmPassword')}
                  className={`rounded-xl bg-card pr-10 ${errors.confirmPassword ? 'border-destructive focus-visible:ring-destructive' : 'border-transparent'}`}
                  placeholder="Confirmez votre mot de passe"
                  aria-invalid={!!errors.confirmPassword}
                  aria-describedby={errors.confirmPassword ? 'confirmPassword-error' : undefined}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                  aria-label={showConfirmPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                >
                  {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {errors.confirmPassword && (
                <p id="confirmPassword-error" role="alert" className="text-sm text-destructive flex items-center gap-1.5">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  {errors.confirmPassword.message}
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
                    Création du compte...
                  </span>
                ) : (
                  'Créer mon compte'
                )}
              </Button>

              <p className="text-xs text-muted-foreground text-center">
                En vous inscrivant, vous acceptez nos{' '}
                <Link to="/terms" className="text-foreground font-medium hover:underline">
                  Conditions d'utilisation
                </Link>{' '}
                et notre{' '}
                <Link to="/privacy" className="text-foreground font-medium hover:underline">
                  Politique de confidentialité
                </Link>
                .
              </p>

              <GoogleDivider />
              <GoogleAuthButton
                label="Continuer avec Google"
                onSuccess={() => navigate('/dashboard', { replace: true })}
                onError={(message) => setServerError(message)}
                disabled={loading}
              />
            </div>
          </form>

          <p className="mt-7 text-center text-sm text-muted-foreground">
            Déjà un compte ?{' '}
            <Link to="/login" className="text-brand font-semibold hover:opacity-80 transition-opacity">
              Se connecter
            </Link>
          </p>
        </div>
      </motion.div>
    </div>
  );
}
