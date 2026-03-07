import { useNavigate } from 'react-router-dom';
import { XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { BlurFade } from '@/components/effects/blur-fade';

export default function PaymentCancel() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      <BlurFade delay={0.1}>
        <div className="w-20 h-20 bg-muted rounded-full flex items-center justify-center mb-6">
          <XCircle className="h-10 w-10 text-muted-foreground" />
        </div>
      </BlurFade>
      <BlurFade delay={0.2}>
        <h1 className="font-display text-3xl font-semibold tracking-tight mb-2">
          Paiement annule
        </h1>
      </BlurFade>
      <BlurFade delay={0.3}>
        <p className="text-muted-foreground mb-8 max-w-md">
          Le paiement a ete annule. Vous pouvez reessayer a tout moment.
        </p>
      </BlurFade>
      <BlurFade delay={0.4}>
        <div className="flex gap-3">
          <Button onClick={() => navigate('/upgrade')} aria-label="Reessayer le paiement">
            Reessayer
          </Button>
          <Button variant="outline" onClick={() => navigate('/lists')} aria-label="Retour aux listes">
            Retour aux listes
          </Button>
        </div>
      </BlurFade>
    </div>
  );
}
