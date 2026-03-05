import { useState } from 'react';
import { useNavigate, useLocation, useSearchParams, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, CheckCircle } from 'lucide-react';
import { motion } from 'motion/react';
import { useAuth } from '../contexts/AuthContext';
import { loginSchema, LoginFormData } from '../schemas/auth.schemas';
import { getApiErrorMessage, getApiErrorStatus } from '../utils/errorUtils';
import { GoogleAuthButton, GoogleDivider } from '../components/GoogleAuthButton';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { DotPattern } from '@/components/effects/dot-pattern';
import { BlurFade } from '@/components/effects/blur-fade';

export function LoginPage() {
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

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
        setServerError('Votre compte n\'est pas encore verifie. Consultez votre email.');
      } else {
        setServerError(getApiErrorMessage(err, 'Email ou mot de passe invalide'));
      }
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
              <h1 className="font-display text-4xl font-semibold tracking-tight mb-2">Inventory</h1>
            </BlurFade>
            <BlurFade delay={0.4}>
              <p className="text-muted-foreground">Connectez-vous pour gerer votre inventaire</p>
            </BlurFade>
          </div>

          <div className="rounded-2xl border bg-card p-8 shadow-float">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              {verified && (
                <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg px-4 py-3 text-green-700 dark:text-green-400 text-sm flex items-center gap-2">
                  <CheckCircle className="h-4 w-4 flex-shrink-0" />
                  Email verifie avec succes ! Vous pouvez maintenant vous connecter.
                </div>
              )}

              {reset && (
                <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg px-4 py-3 text-green-700 dark:text-green-400 text-sm flex items-center gap-2">
                  <CheckCircle className="h-4 w-4 flex-shrink-0" />
                  Mot de passe reinitialise avec succes ! Connectez-vous avec votre nouveau mot de passe.
                </div>
              )}

              {serverError && (
                <div className="bg-destructive/10 border border-destructive/20 rounded-lg px-4 py-3 text-destructive text-sm">
                  {serverError}
                  {isUnverified && (
                    <Link to="/verify-email" className="block mt-1 font-medium underline">
                      Renvoyer l'email de verification
                    </Link>
                  )}
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
                />
                {errors.email && (
                  <p className="text-sm text-destructive flex items-center gap-1.5">
                    <AlertCircle className="h-4 w-4 flex-shrink-0" />
                    {errors.email.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="password">Mot de passe</Label>
                  <Link to="/forgot-password" className="text-xs text-muted-foreground hover:text-foreground hover:underline">
                    Mot de passe oublie ?
                  </Link>
                </div>
                <Input
                  id="password"
                  type="password"
                  {...register('password')}
                  className={errors.password ? 'border-destructive focus-visible:ring-destructive' : ''}
                  placeholder="Entrez votre mot de passe"
                />
                {errors.password && (
                  <p className="text-sm text-destructive flex items-center gap-1.5">
                    <AlertCircle className="h-4 w-4 flex-shrink-0" />
                    {errors.password.message}
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
                    Connexion...
                  </span>
                ) : (
                  'Se connecter'
                )}
              </Button>
            </form>

            <GoogleDivider />
            <GoogleAuthButton
              label="Se connecter avec Google"
              onSuccess={() => navigate(from, { replace: true })}
              onError={(msg) => setServerError(msg)}
              disabled={loading}
            />
          </div>

          <p className="mt-6 text-center text-muted-foreground text-sm">
            Nouveau sur Inventory ?{' '}
            <Link to="/signup" className="text-foreground font-medium hover:underline">
              Creer un compte
            </Link>
          </p>
        </motion.div>
      </div>
    </div>
  );
}
