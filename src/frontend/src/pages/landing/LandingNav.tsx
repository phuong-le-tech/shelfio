import { Link } from "react-router-dom";
import { Package } from "lucide-react";
import { Button } from "@/components/ui/button";

export function LandingNav() {
  return (
    <nav aria-label="Navigation principale" className="sticky top-0 z-50 bg-background/95 backdrop-blur-sm border-b">
      <div className="max-w-6xl mx-auto px-6 flex items-center justify-between h-16">
        <Link to="/" className="flex items-center gap-3" aria-label="Inventory - Accueil">
          <div className="w-9 h-9 bg-foreground rounded-xl flex items-center justify-center">
            <Package className="h-[18px] w-[18px] text-background" aria-hidden="true" />
          </div>
          <span className="font-display text-xl font-semibold tracking-tight">
            Inventory
          </span>
        </Link>
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" asChild>
            <Link to="/login">Se connecter</Link>
          </Button>
          <Button size="sm" asChild>
            <Link to="/signup">Commencer</Link>
          </Button>
        </div>
      </div>
    </nav>
  );
}

export function LandingFooter() {
  return (
    <footer className="border-t py-8" role="contentinfo">
      <div className="max-w-6xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-4">
        <p className="text-sm text-muted-foreground">
          &copy; {new Date().getFullYear()} Inventory. Tous droits reserves.
        </p>
        <nav aria-label="Liens legaux" className="flex items-center gap-6">
          <Link
            to="/terms"
            className="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            Conditions d'utilisation
          </Link>
          <Link
            to="/privacy"
            className="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            Politique de confidentialite
          </Link>
        </nav>
      </div>
    </footer>
  );
}
