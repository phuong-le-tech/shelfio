import { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, KeyRound } from 'lucide-react';
import { motion } from 'motion/react';
import { resetPasswordSchema, ResetPasswordFormData } from '../schemas/auth.schemas';
import { authApi } from '../services/authApi';
import { getApiErrorMessage } from '../utils/errorUtils';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function ResetPassword() {
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormData>({ resolver: zodResolver(resetPasswordSchema) });

  const onSubmit = async (data: ResetPasswordFormData) => {
    if (!token) return;
    setServerError('');
    setLoading(true);
    try {
      await authApi.resetPassword(token, data.password);
      navigate('/login?reset=true', { replace: true });
    } catch (err: unknown) {
      setServerError(getApiErrorMessage(err, 'Le lien est invalide ou a expiré'));
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
          className="w-full max-w-[420px]"
        >
          <div className="rounded-[20px] border p-10">
            <div className="text-center">
              <div className="w-14 h-14 rounded-2xl bg-destructive/10 flex items-center justify-center mx-auto mb-4">
                <AlertCircle className="h-7 w-7 text-destructive" />
              </div>
              <h2 className="font-display text-[24px] font-bold tracking-tight mb-2">Lien invalide</h2>
              <p className="text-sm text-muted-foreground mb-6">Ce lien de réinitialisation est invalide.</p>
              <Button asChild className="w-full rounded-xl h-[46px] text-[15px] font-semibold">
                <Link to="/forgot-password">Demander un nouveau lien</Link>
              </Button>
            </div>
          </div>
        </motion.div>
      </div>
    );
  }

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
            <div className="w-14 h-14 rounded-2xl bg-brand/10 flex items-center justify-center mx-auto mb-4">
              <KeyRound className="h-7 w-7 text-brand" />
            </div>
            <h1 className="font-display text-[24px] font-bold tracking-tight">Nouveau mot de passe</h1>
            <p className="text-sm text-muted-foreground mt-2 max-w-[300px] mx-auto">
              Choisissez un nouveau mot de passe pour votre compte
            </p>
          </div>

          {serverError && (
            <div className="bg-destructive/10 border border-destructive/20 rounded-xl px-4 py-3 text-destructive text-sm mb-4">
              {serverError}
              <Link to="/forgot-password" className="block mt-1 font-medium underline">
                Demander un nouveau lien
              </Link>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="password" className="text-[13px] font-medium">Nouveau mot de passe</Label>
              <Input
                id="password"
                type="password"
                {...register('password')}
                className={`rounded-xl bg-card ${errors.password ? 'border-destructive focus-visible:ring-destructive' : 'border-transparent'}`}
                placeholder="Minimum 12 caractères"
                aria-invalid={!!errors.password}
                aria-describedby={errors.password ? 'password-error' : undefined}
              />
              {errors.password && (
                <p id="password-error" role="alert" className="text-sm text-destructive flex items-center gap-1.5">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  {errors.password.message}
                </p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="confirmPassword" className="text-[13px] font-medium">Confirmer le mot de passe</Label>
              <Input
                id="confirmPassword"
                type="password"
                {...register('confirmPassword')}
                className={`rounded-xl bg-card ${errors.confirmPassword ? 'border-destructive focus-visible:ring-destructive' : 'border-transparent'}`}
                placeholder="Confirmez votre mot de passe"
                aria-invalid={!!errors.confirmPassword}
                aria-describedby={errors.confirmPassword ? 'confirmPassword-error' : undefined}
              />
              {errors.confirmPassword && (
                <p id="confirmPassword-error" role="alert" className="text-sm text-destructive flex items-center gap-1.5">
                  <AlertCircle className="h-4 w-4 flex-shrink-0" />
                  {errors.confirmPassword.message}
                </p>
              )}
            </div>

            <div className="pt-3">
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
                    Réinitialisation...
                  </span>
                ) : (
                  'Réinitialiser le mot de passe'
                )}
              </Button>
            </div>
          </form>

          <p className="mt-7 text-center text-sm text-muted-foreground">
            <Link to="/login" className="text-brand font-semibold hover:opacity-80 transition-opacity">
              ← Retour à la connexion
            </Link>
          </p>
        </div>
      </motion.div>
    </div>
  );
}
