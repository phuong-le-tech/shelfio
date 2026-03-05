import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { BlurFade } from "@/components/effects/blur-fade";
import { DotPattern } from "@/components/effects/dot-pattern";

export function HeroSection() {
  return (
    <section aria-labelledby="hero-heading" className="relative overflow-hidden py-24 md:py-36 lg:py-44">
      <DotPattern
        className="opacity-30 [mask-image:radial-gradient(ellipse_at_center,black_30%,transparent_70%)]"
        width={20}
        height={20}
        cr={1}
      />
      <div className="relative max-w-6xl mx-auto px-6 text-center">
        <BlurFade delay={0.1} duration={0.6}>
          <p className="text-sm font-medium text-muted-foreground tracking-widest uppercase mb-6">
            Gestion d'inventaire simplifiee
          </p>
        </BlurFade>
        <BlurFade delay={0.2} duration={0.6}>
          <h1 id="hero-heading" className="font-display text-4xl md:text-5xl lg:text-6xl xl:text-7xl font-bold tracking-tight max-w-4xl mx-auto leading-[1.1]">
            Gerez votre inventaire{" "}
            <span className="text-muted-foreground">en toute simplicite</span>
          </h1>
        </BlurFade>
        <BlurFade delay={0.3} duration={0.6}>
          <p className="text-lg md:text-xl text-muted-foreground mt-6 max-w-2xl mx-auto leading-relaxed">
            Organisez vos articles, suivez vos niveaux de stock et gardez le
            controle de votre inventaire avec un tableau de bord intuitif.
          </p>
        </BlurFade>
        <BlurFade delay={0.4} duration={0.6}>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mt-10">
            <Button size="lg" className="h-12 px-8 text-base" asChild>
              <Link to="/signup">
                Commencer gratuitement
                <ArrowRight className="h-4 w-4 ml-2" aria-hidden="true" />
              </Link>
            </Button>
            <button
              onClick={() =>
                document
                  .getElementById("features")
                  ?.scrollIntoView({ behavior: "smooth" })
              }
              className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
            >
              Decouvrir les fonctionnalites
            </button>
          </div>
        </BlurFade>
      </div>
    </section>
  );
}

export function CTASection() {
  return (
    <section aria-labelledby="cta-heading" className="relative overflow-hidden py-24 md:py-32">
      <DotPattern
        className="opacity-20 [mask-image:radial-gradient(ellipse_at_center,black_20%,transparent_70%)]"
        width={20}
        height={20}
        cr={1}
      />
      <div className="relative max-w-6xl mx-auto px-6 text-center">
        <BlurFade delay={0.1} inView>
          <h2 id="cta-heading" className="font-display text-3xl md:text-4xl font-semibold tracking-tight mb-4">
            Pret a organiser votre inventaire ?
          </h2>
        </BlurFade>
        <BlurFade delay={0.2} inView>
          <p className="text-muted-foreground text-lg max-w-xl mx-auto mb-8">
            Creez votre compte gratuitement et commencez a gerer votre inventaire
            des aujourd'hui.
          </p>
        </BlurFade>
        <BlurFade delay={0.3} inView>
          <div className="flex flex-col items-center gap-4">
            <Button size="lg" className="h-12 px-8 text-base" asChild>
              <Link to="/signup">
                Commencer gratuitement
                <ArrowRight className="h-4 w-4 ml-2" aria-hidden="true" />
              </Link>
            </Button>
            <Link
              to="/login"
              className="text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              Deja un compte ? Se connecter
            </Link>
          </div>
        </BlurFade>
      </div>
    </section>
  );
}
