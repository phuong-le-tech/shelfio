import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { BlurFade } from '@/components/effects/blur-fade';

export default function PaymentSuccess() {
  const { refreshUser } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <BlurFade delay={0.1}>
        <div className="w-20 h-20 bg-emerald-100 dark:bg-emerald-900/30 rounded-full flex items-center justify-center mb-6">
          <CheckCircle className="h-10 w-10 text-emerald-600" />
        </div>
      </BlurFade>
      <BlurFade delay={0.2}>
        <h1 className="font-display text-3xl font-semibold tracking-tight mb-2">
          Paiement reussi !
        </h1>
      </BlurFade>
      <BlurFade delay={0.3}>
        <p className="text-muted-foreground mb-8 max-w-md">
          Felicitations ! Vous etes maintenant un utilisateur Premium.
          Vous pouvez creer des listes illimitees.
        </p>
      </BlurFade>
      <BlurFade delay={0.4}>
        <Button onClick={() => navigate('/lists')}>
          Retour aux listes
        </Button>
      </BlurFade>
    </div>
  );
}
