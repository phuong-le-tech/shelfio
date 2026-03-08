import { Navigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import {
  LandingNav,
  HeroSection,
  FeaturesSection,
  ListsPreviewSection,
  CTASection,
  LandingFooter,
} from "./landing/sections";

export default function LandingPage() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div role="status" aria-label="Chargement">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-foreground" />
          <span className="sr-only">Chargement...</span>
        </div>
      </div>
    );
  }

  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="min-h-screen bg-background">
      <LandingNav />
      <HeroSection />
      <FeaturesSection />
      <ListsPreviewSection />
      <CTASection />
      <LandingFooter />
    </div>
  );
}
