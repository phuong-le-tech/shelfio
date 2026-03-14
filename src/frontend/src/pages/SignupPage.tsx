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
import { DotPattern } from '@/components/effects/dot-pattern';
import { BlurFade } from '@/components/effects/blur-fade';

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
    <div className="min-h-screen flex relative overflow-hidden bg-background">
      {/* Left side - decorative */}
      <div className="hidden lg:flex lg:w-[45%] relative items-center justify-center overflow-hidden">
        <DotPattern className="opacity-40" width={20} height={20} cr={1} />
        <BlurFade delay={0.1} duration={0.8} blur="20px">
          <h1
            className="font-display text-[8rem] xl:text-[10rem] font-bold text-foreground/[0.04] select-none leading-none -rotate-3"
          >
            Inven
            <br />
            tory
          </h1>
        </BlurFade>
      </div>

      {/* Right side - form */}
      <div className="flex-1 flex items-center justify-center px-6 lg:px-16">
        <motion.div
          initial={{ opacity: 0, x: 30 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
          className="w-full max-w-md"
        >
          <div className="mb-10">
            <BlurFade delay={0.2}>
              <div className="w-12 h-12 bg-brand/10 rounded-xl flex items-center justify-center mb-6">
                <svg aria-hidden="true" className="w-6 h-6 text-brand" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                </svg>
              </div>
            </BlurFade>
            <BlurFade delay={0.3}>
              <h1 className="font-display text-4xl font-semibold tracking-tight mb-2">Créer un compte</h1>
            </BlurFade>
            <BlurFade delay={0.4}>
              <p className="text-muted-foreground">Commencez à gérer votre inventaire</p>
            </BlurFade>
          </div>

          <div className="rounded-2xl border bg-card p-8 shadow-float">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              {serverError && (
                <div className="bg-destructive/10 border border-destructive/20 rounded-lg px-4 py-3 text-destructive text-sm">
                  {serverError}
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  {...register('email')}
                  className={errors.email ? 'border-destructive focus-visible:ring-destructive' : ''}
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

              <div className="space-y-2">
                <Label htmlFor="password">Mot de passe</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    {...register('password')}
                    className={errors.password ? 'border-destructive focus-visible:ring-destructive pr-10' : 'pr-10'}
                    placeholder="Minimum 12 caractères"
                    aria-invalid={!!errors.password}
                    aria-describedby={errors.password ? 'password-error' : undefined}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
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

              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirmer le mot de passe</Label>
                <div className="relative">
                  <Input
                    id="confirmPassword"
                    type={showConfirmPassword ? 'text' : 'password'}
                    {...register('confirmPassword')}
                    className={errors.confirmPassword ? 'border-destructive focus-visible:ring-destructive pr-10' : 'pr-10'}
                    placeholder="Confirmez votre mot de passe"
                    aria-invalid={!!errors.confirmPassword}
                    aria-describedby={errors.confirmPassword ? 'confirmPassword-error' : undefined}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
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

              <Button
                type="submit"
                disabled={loading}
                className="w-full h-11"
                size="lg"
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
            </form>

            <GoogleDivider />
            <GoogleAuthButton
              label="S'inscrire avec Google"
              onSuccess={() => navigate('/dashboard', { replace: true })}
              onError={(message) => setServerError(message)}
              disabled={loading}
            />
          </div>

          <p className="mt-6 text-center text-muted-foreground text-sm">
            Vous avez déjà un compte ?{' '}
            <Link to="/login" className="text-foreground font-medium hover:underline">
              Se connecter
            </Link>
          </p>
        </motion.div>
      </div>
    </div>
  );
}
