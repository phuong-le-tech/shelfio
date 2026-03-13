import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  User,
  Lock,
  Download,
  Trash2,
  Crown,
  Calendar,
  Shield,
  AlertCircle,
  Eye,
  EyeOff,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { authApi } from '../services/authApi';
import {
  changePasswordSchema,
  ChangePasswordFormData,
} from '../schemas/auth.schemas';
import { getApiErrorMessage } from '../utils/errorUtils';
import { useToast } from '../components/Toast';
import ConfirmModal from '../components/ConfirmModal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';
import { BlurFade } from '@/components/effects/blur-fade';

export default function SettingsPage() {
  const { user, deleteAccount, isPremium } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<ChangePasswordFormData>({
    resolver: zodResolver(changePasswordSchema),
  });

  const handlePasswordChange = async (data: ChangePasswordFormData) => {
    setPasswordLoading(true);
    try {
      await authApi.changePassword(data.currentPassword, data.newPassword);
      showToast('Mot de passe modifié avec succès', 'success');
      reset();
    } catch (err: unknown) {
      showToast(
        getApiErrorMessage(err, 'Erreur lors du changement de mot de passe'),
        'error'
      );
    } finally {
      setPasswordLoading(false);
    }
  };

  const handleExportData = async () => {
    setExportLoading(true);
    try {
      const data = await authApi.exportData();
      const blob = new Blob([JSON.stringify(data, null, 2)], {
        type: 'application/json',
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `mes-donnees-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      showToast('Données exportées avec succès', 'success');
    } catch (err: unknown) {
      showToast(
        getApiErrorMessage(err, "Erreur lors de l'exportation des données"),
        'error'
      );
    } finally {
      setExportLoading(false);
    }
  };

  const handleDeleteAccount = async () => {
    await deleteAccount();
    navigate('/login');
  };

  if (!user) return null;

  return (
    <div className="max-w-3xl mx-auto">
      <BlurFade delay={0.1}>
        <h1 className="font-display text-4xl font-semibold tracking-tight mb-8">
          Paramètres
        </h1>
      </BlurFade>

      <div className="space-y-6">
        {/* Profile Information */}
        <BlurFade delay={0.15}>
          <section className="rounded-2xl border bg-card p-8 shadow-sm">
            <h2 className="font-display text-lg font-semibold mb-6 flex items-center gap-2">
              <User className="h-5 w-5" />
              Informations du profil
            </h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <Label className="text-muted-foreground text-xs uppercase tracking-wider">
                  Email
                </Label>
                <p className="text-foreground mt-1">{user.email}</p>
              </div>
              <div>
                <Label className="text-muted-foreground text-xs uppercase tracking-wider">
                  Rôle
                </Label>
                <div className="mt-1">
                  {user.role === 'PREMIUM_USER' && (
                    <Badge variant="secondary">
                      <Crown className="h-3 w-3 mr-1" /> Premium
                    </Badge>
                  )}
                  {user.role === 'ADMIN' && (
                    <Badge variant="secondary">
                      <Shield className="h-3 w-3 mr-1" /> Admin
                    </Badge>
                  )}
                  {user.role === 'USER' && (
                    <Badge variant="outline">Utilisateur</Badge>
                  )}
                </div>
              </div>
              <div>
                <Label className="text-muted-foreground text-xs uppercase tracking-wider">
                  Type de compte
                </Label>
                <p className="text-foreground mt-1">
                  {user.hasGoogleAccount
                    ? 'Compte Google'
                    : 'Email / Mot de passe'}
                </p>
              </div>
              <div>
                <Label className="text-muted-foreground text-xs uppercase tracking-wider">
                  Membre depuis
                </Label>
                <p className="text-foreground mt-1 flex items-center gap-1.5">
                  <Calendar className="h-4 w-4 text-muted-foreground" />
                  {new Date(user.createdAt).toLocaleDateString('fr-FR', {
                    day: 'numeric',
                    month: 'long',
                    year: 'numeric',
                  })}
                </p>
              </div>
            </div>
          </section>
        </BlurFade>

        {/* Change Password */}
        <BlurFade delay={0.2}>
          <section className="rounded-2xl border bg-card p-8 shadow-sm">
            <h2 className="font-display text-lg font-semibold mb-6 flex items-center gap-2">
              <Lock className="h-5 w-5" />
              Changer le mot de passe
            </h2>
            {user.hasGoogleAccount ? (
              <p className="text-muted-foreground text-sm">
                Votre compte est lié à Google. Utilisez les paramètres de votre
                compte Google pour gérer votre mot de passe.
              </p>
            ) : (
              <form
                onSubmit={handleSubmit(handlePasswordChange)}
                className="space-y-4"
              >
                <div className="space-y-2">
                  <Label htmlFor="currentPassword">Mot de passe actuel</Label>
                  <div className="relative">
                    <Input
                      id="currentPassword"
                      type={showCurrentPassword ? 'text' : 'password'}
                      {...register('currentPassword')}
                      className={
                        errors.currentPassword
                          ? 'border-destructive focus-visible:ring-destructive pr-10'
                          : 'pr-10'
                      }
                      aria-invalid={!!errors.currentPassword}
                      aria-describedby={
                        errors.currentPassword
                          ? 'currentPassword-error'
                          : undefined
                      }
                    />
                    <button
                      type="button"
                      onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                      aria-label={showCurrentPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                    >
                      {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {errors.currentPassword && (
                    <p
                      id="currentPassword-error"
                      role="alert"
                      className="text-sm text-destructive flex items-center gap-1.5"
                    >
                      <AlertCircle className="h-4 w-4 flex-shrink-0" />
                      {errors.currentPassword.message}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="newPassword">Nouveau mot de passe</Label>
                  <div className="relative">
                    <Input
                      id="newPassword"
                      type={showNewPassword ? 'text' : 'password'}
                      {...register('newPassword')}
                      className={
                        errors.newPassword
                          ? 'border-destructive focus-visible:ring-destructive pr-10'
                          : 'pr-10'
                      }
                      placeholder="Minimum 12 caractères"
                      aria-invalid={!!errors.newPassword}
                      aria-describedby={
                        errors.newPassword ? 'newPassword-error' : undefined
                      }
                    />
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                      aria-label={showNewPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                    >
                      {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {errors.newPassword && (
                    <p
                      id="newPassword-error"
                      role="alert"
                      className="text-sm text-destructive flex items-center gap-1.5"
                    >
                      <AlertCircle className="h-4 w-4 flex-shrink-0" />
                      {errors.newPassword.message}
                    </p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">
                    Confirmer le nouveau mot de passe
                  </Label>
                  <div className="relative">
                    <Input
                      id="confirmPassword"
                      type={showConfirmPassword ? 'text' : 'password'}
                      {...register('confirmPassword')}
                      className={
                        errors.confirmPassword
                          ? 'border-destructive focus-visible:ring-destructive pr-10'
                          : 'pr-10'
                      }
                      aria-invalid={!!errors.confirmPassword}
                      aria-describedby={
                        errors.confirmPassword
                          ? 'confirmPassword-error'
                          : undefined
                      }
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
                    <p
                      id="confirmPassword-error"
                      role="alert"
                      className="text-sm text-destructive flex items-center gap-1.5"
                    >
                      <AlertCircle className="h-4 w-4 flex-shrink-0" />
                      {errors.confirmPassword.message}
                    </p>
                  )}
                </div>

                <Button type="submit" disabled={passwordLoading}>
                  {passwordLoading
                    ? 'Modification...'
                    : 'Modifier le mot de passe'}
                </Button>
              </form>
            )}
          </section>
        </BlurFade>

        {/* Subscription */}
        <BlurFade delay={0.25}>
          <section className="rounded-2xl border bg-card p-8 shadow-sm">
            <h2 className="font-display text-lg font-semibold mb-6 flex items-center gap-2">
              <Crown className="h-5 w-5" />
              Abonnement
            </h2>
            <div>
              <Label className="text-muted-foreground text-xs uppercase tracking-wider">
                Plan actuel
              </Label>
              <p className="text-foreground mt-1 mb-4">
                {isPremium
                  ? 'Premium — Listes illimitées'
                  : 'Gratuit — 5 listes maximum'}
              </p>
              {!isPremium && user.role !== 'ADMIN' && (
                <Button onClick={() => navigate('/upgrade')}>
                  <Crown className="h-4 w-4 mr-2" />
                  Passer en Premium
                </Button>
              )}
            </div>
          </section>
        </BlurFade>

        {/* Data & Privacy */}
        <BlurFade delay={0.3}>
          <section className="rounded-2xl border bg-card p-8 shadow-sm">
            <h2 className="font-display text-lg font-semibold mb-6">
              Données & Confidentialité
            </h2>
            <div className="space-y-6">
              <div>
                <p className="text-sm text-muted-foreground mb-3">
                  Téléchargez toutes vos données au format JSON (RGPD).
                </p>
                <Button
                  onClick={handleExportData}
                  variant="outline"
                  disabled={exportLoading}
                >
                  <Download className="h-4 w-4 mr-2" />
                  {exportLoading
                    ? 'Exportation...'
                    : 'Exporter mes données'}
                </Button>
              </div>
              <Separator />
              <div>
                <p className="text-sm text-muted-foreground mb-3">
                  Cette action est irréversible. Toutes vos listes et articles
                  seront supprimés.
                </p>
                <Button
                  onClick={() => setShowDeleteModal(true)}
                  variant="destructive"
                >
                  <Trash2 className="h-4 w-4 mr-2" />
                  Supprimer mon compte
                </Button>
              </div>
            </div>
          </section>
        </BlurFade>
      </div>

      <ConfirmModal
        isOpen={showDeleteModal}
        title="Supprimer votre compte"
        message="Cette action est irréversible. Toutes vos listes et vos objets seront définitivement supprimés."
        confirmLabel="Supprimer définitivement"
        requireTyping="SUPPRIMER"
        onConfirm={handleDeleteAccount}
        onCancel={() => setShowDeleteModal(false)}
      />
    </div>
  );
}
