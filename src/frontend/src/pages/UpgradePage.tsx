import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Crown, Check, Sparkles } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { stripeApi } from '../services/stripeApi';
import { useToast } from '../components/Toast';
import { getApiErrorStatus } from '../utils/errorUtils';
import { Button } from '@/components/ui/button';
import { BlurFade } from '@/components/effects/blur-fade';

export default function UpgradePage() {
  const { isPremium } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [loading, setLoading] = useState(false);
  const [waiverAccepted, setWaiverAccepted] = useState(false);

  const handleUpgrade = async () => {
    setLoading(true);
    try {
      const { url } = await stripeApi.createCheckoutSession({ withdrawalWaiverAccepted: true });
      if (!url || !url.startsWith('https://checkout.stripe.com/')) {
        showToast('URL de paiement invalide', 'error');
        setLoading(false);
        return;
      }
      window.location.href = url;
    } catch (error) {
      const status = getApiErrorStatus(error);
      if (status === 403) {
        showToast('Les comptes admin ne peuvent pas acheter Premium', 'error');
      } else if (status === 400) {
        showToast('Vous êtes déjà un utilisateur Premium', 'error');
      } else {
        showToast('Erreur lors de la création de la session de paiement', 'error');
      }
      setLoading(false);
    }
  };

  if (isPremium) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center">
        <BlurFade delay={0.1}>
          <div className="w-16 h-16 bg-brand-light rounded-2xl flex items-center justify-center mb-4">
            <Crown className="h-8 w-8 text-brand-dark" />
          </div>
        </BlurFade>
        <BlurFade delay={0.2}>
          <h1 className="font-display text-3xl font-semibold tracking-tight mb-2">
            Vous êtes déjà Premium
          </h1>
        </BlurFade>
        <BlurFade delay={0.3}>
          <p className="text-muted-foreground mb-6">
            Vous bénéficiez déjà de listes illimitées.
          </p>
        </BlurFade>
        <BlurFade delay={0.4}>
          <Button onClick={() => navigate('/lists')}>Retour aux listes</Button>
        </BlurFade>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto py-12">
      <BlurFade delay={0.1}>
        <div className="text-center mb-10">
          <h1 className="font-display text-4xl font-semibold tracking-tight mb-3">
            Passez en Premium
          </h1>
          <p className="text-muted-foreground text-lg">
            Débloquez des listes illimitées avec un paiement unique
          </p>
        </div>
      </BlurFade>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Free plan */}
        <BlurFade delay={0.2}>
          <div className="rounded-2xl border bg-card p-8 shadow-sm h-full">
            <h2 className="font-display text-xl font-semibold mb-1">Gratuit</h2>
            <p className="text-muted-foreground text-sm mb-6">Pour bien commencer</p>
            <div className="text-4xl font-display font-bold mb-6">
              0<span className="text-lg font-normal text-muted-foreground">€</span>
            </div>
            <ul className="space-y-3 mb-8">
              <li className="flex items-center gap-2 text-sm">
                <Check className="h-4 w-4 text-muted-foreground" />
                <span>Jusqu'à 5 listes</span>
              </li>
              <li className="flex items-center gap-2 text-sm">
                <Check className="h-4 w-4 text-muted-foreground" />
                <span>Articles illimités par liste</span>
              </li>
              <li className="flex items-center gap-2 text-sm">
                <Check className="h-4 w-4 text-muted-foreground" />
                <span>Champs personnalisés</span>
              </li>
            </ul>
            <Button variant="outline" className="w-full" disabled>
              Plan actuel
            </Button>
          </div>
        </BlurFade>

        {/* Premium plan */}
        <BlurFade delay={0.3}>
          <div className="rounded-2xl border-2 border-brand bg-card p-8 shadow-sm relative h-full">
            <div className="absolute -top-3 left-1/2 -translate-x-1/2">
              <span className="bg-brand text-foreground text-xs font-medium px-3 py-1 rounded-full flex items-center gap-1">
                <Sparkles className="h-3 w-3" />
                Recommandé
              </span>
            </div>
            <h2 className="font-display text-xl font-semibold mb-1">Premium</h2>
            <p className="text-muted-foreground text-sm mb-6">Pour les utilisateurs avancés</p>
            <div className="text-4xl font-display font-bold mb-1">
              2<span className="text-lg font-normal text-muted-foreground">€</span>
            </div>
            <p className="text-xs text-muted-foreground mb-6">Paiement unique, à vie</p>
            <ul className="space-y-3 mb-8">
              <li className="flex items-center gap-2 text-sm">
                <Check className="h-4 w-4 text-brand-dark" />
                <span className="font-medium">Listes illimitées</span>
              </li>
              <li className="flex items-center gap-2 text-sm">
                <Check className="h-4 w-4 text-brand-dark" />
                <span>Articles illimités par liste</span>
              </li>
              <li className="flex items-center gap-2 text-sm">
                <Check className="h-4 w-4 text-brand-dark" />
                <span>Champs personnalisés</span>
              </li>
              <li className="flex items-center gap-2 text-sm">
                <Check className="h-4 w-4 text-brand-dark" />
                <span>Accès à vie</span>
              </li>
            </ul>
            <Button className="w-full" onClick={handleUpgrade} disabled={loading || !waiverAccepted}>
              {loading ? 'Redirection...' : 'Payer 2 €'}
            </Button>
          </div>
        </BlurFade>
      </div>

      {/* Withdrawal waiver — Consumer Rights Directive Art. L221-28 */}
      <BlurFade delay={0.4}>
        <div className="mt-8 max-w-xl mx-auto">
          <label className="flex items-start gap-3 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={waiverAccepted}
              onChange={(e) => setWaiverAccepted(e.target.checked)}
              className="mt-1 h-4 w-4 rounded border-border accent-brand shrink-0"
            />
            <span className="text-sm text-muted-foreground leading-relaxed">
              J'accepte que le service Premium commence immédiatement et je reconnais
              perdre mon{' '}
              <Link to="/terms" className="underline underline-offset-2 hover:text-foreground">
                droit de rétractation de 14 jours
              </Link>.
            </span>
          </label>
        </div>
      </BlurFade>
    </div>
  );
}
