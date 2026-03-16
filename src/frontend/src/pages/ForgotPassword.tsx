import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AlertCircle, Mail } from 'lucide-react';
import { motion } from 'motion/react';
import { forgotPasswordSchema, ForgotPasswordFormData } from '../schemas/auth.schemas';
import { authApi } from '../services/authApi';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export function ForgotPassword() {
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormData>({ resolver: zodResolver(forgotPasswordSchema) });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    setLoading(true);
    try {
      await authApi.forgotPassword(data.email);
    } catch {
      // Always show submitted state to prevent email enumeration
    }
    setSubmitted(true);
    setLoading(false);
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
          {submitted ? (
            <div className="text-center">
              <div className="w-14 h-14 rounded-2xl bg-green-50 dark:bg-green-900/20 flex items-center justify-center mx-auto mb-4">
                <Mail className="h-7 w-7 text-green-600 dark:text-green-400" />
              </div>
              <h2 className="font-display text-lg font-semibold mb-2">Email envoyé</h2>
              <p className="text-sm text-muted-foreground mb-6">
                Si un compte existe avec cet email, vous recevrez un lien de réinitialisation dans quelques instants.
              </p>
              <Button asChild variant="outline" className="w-full rounded-xl h-[46px] text-[15px] font-medium">
                <Link to="/login">Retour à la connexion</Link>
              </Button>
            </div>
          ) : (
            <>
              <div className="text-center mb-7">
                <div className="w-14 h-14 rounded-2xl bg-brand/10 flex items-center justify-center mx-auto mb-4">
                  <Mail className="h-7 w-7 text-brand" />
                </div>
                <h1 className="font-display text-[24px] font-bold tracking-tight">Mot de passe oublié ?</h1>
                <p className="text-sm text-muted-foreground mt-2 max-w-[300px] mx-auto">
                  Entrez votre email pour recevoir un lien de réinitialisation
                </p>
              </div>

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
                        Envoi en cours...
                      </span>
                    ) : (
                      'Envoyer le lien'
                    )}
                  </Button>
                </div>
              </form>
            </>
          )}

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
