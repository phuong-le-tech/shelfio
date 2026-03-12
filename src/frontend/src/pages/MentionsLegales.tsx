import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { motion } from "motion/react";
import { Button } from "@/components/ui/button";

export function MentionsLegales() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background">
      <div className="container max-w-prose mx-auto px-6 py-12">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
        >
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate(-1)}
            className="mb-8 -ml-2"
          >
            <ArrowLeft className="w-4 h-4 mr-1.5" />
            Retour
          </Button>

          <h1 className="font-display text-3xl font-semibold tracking-tight mb-8">
            Mentions légales
          </h1>

          <div className="prose prose-sm text-muted-foreground space-y-6">
            <p className="text-sm text-muted-foreground/70">
              Dernière mise à jour : mars 2026
            </p>

            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                Éditeur du site
              </h2>
              <p>Le site Inventory est édité par une personne physique :</p>
              <ul className="list-none pl-0 space-y-1 mt-2">
                <li>
                  <strong>Nom :</strong> Phuong LE
                </li>
                <li>
                  <strong>
                    Adresse : 6 avenue de la concorde, Meaux, France
                  </strong>{" "}
                </li>
                <li>
                  <strong>Email :</strong> phuongle.tech@gmail.com
                </li>
                <li>
                  <strong>Téléphone :</strong> +33 6 95 32 43 98
                </li>
              </ul>
              <p className="mt-2">Directeur de la publication : Phuong LE</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                Hébergement
              </h2>
              <p>Le site est hébergé par :</p>
              <ul className="list-none pl-0 space-y-1 mt-2">
                <li>
                  <strong>Raison sociale :</strong> Railway Corporation
                </li>
                <li>
                  <strong>Adresse :</strong> 548 Market St, Suite 45984, San
                  Francisco, CA 94104, USA
                </li>
                <li>
                  <strong>Site web :</strong> https://railway.app
                </li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                Propriété intellectuelle
              </h2>
              <p>
                L'ensemble du contenu du site (textes, images, éléments
                graphiques, logo, icônes, logiciels) est la propriété de
                l'éditeur ou fait l'objet d'une autorisation d'utilisation.
                Toute reproduction, représentation, modification ou adaptation
                de tout ou partie du site sans autorisation préalable est
                interdite.
              </p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                Données personnelles
              </h2>
              <p>
                Le traitement de vos données personnelles est régi par notre{" "}
                <a
                  href="/privacy"
                  className="text-foreground underline underline-offset-2 hover:text-brand"
                >
                  Politique de confidentialité
                </a>
                . Conformément au Règlement Général sur la Protection des
                Données (RGPD) et à la loi Informatique et Libertés, vous
                disposez de droits sur vos données personnelles détaillés dans
                cette politique.
              </p>
              <p className="mt-2">
                Pour toute question relative à vos données personnelles, vous
                pouvez contacter l'éditeur à l'adresse email indiquée ci-dessus,
                ou adresser une réclamation à la CNIL :
              </p>
              <ul className="list-none pl-0 space-y-1 mt-2">
                <li>
                  <strong>CNIL</strong> — Commission Nationale de l'Informatique
                  et des Libertés
                </li>
                <li>3 Place de Fontenoy, TSA 80715, 75334 Paris Cedex 07</li>
                <li>https://www.cnil.fr</li>
              </ul>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                Règlement des litiges
              </h2>
              <p>
                En cas de litige, le consommateur peut recourir à une médiation
                conventionnelle ou à tout mode alternatif de règlement des
                différends. Conformément au Règlement (UE) n° 524/2013, la
                Commission européenne met à disposition une plateforme de
                règlement en ligne des litiges :{" "}
                <a
                  href="https://ec.europa.eu/consumers/odr/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-foreground underline underline-offset-2 hover:text-brand"
                >
                  https://ec.europa.eu/consumers/odr/
                </a>
              </p>
            </section>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
