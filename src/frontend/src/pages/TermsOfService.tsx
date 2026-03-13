import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { motion } from "motion/react";
import { Button } from "@/components/ui/button";

export function TermsOfService() {
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
            Conditions générales d'utilisation
          </h1>

          <div className="prose prose-sm text-muted-foreground space-y-6">
            <p className="text-sm text-muted-foreground/70">
              Dernière mise à jour : mars 2026
            </p>

            {/* 1. Identification du professionnel */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                1. Identification du professionnel
              </h2>
              <p>Le service Inventory est édité par :</p>
              <ul className="list-none pl-0 space-y-1 mt-2">
                <li>
                  <strong>Nom :</strong> Phuong LE{" "}
                </li>
                <li>
                  <strong>Adresse :</strong> 6 avenue de la concorde, Meaux,
                  France
                </li>
                <li>
                  <strong>Email :</strong> phuongle.tech@gmail.com
                </li>
              </ul>
            </section>

            {/* 2. Objet et description du service */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                2. Objet et description du service
              </h2>
              <p>
                Inventory est un service en ligne de gestion d'inventaire
                permettant aux utilisateurs de créer des listes, d'y ajouter des
                articles avec des champs personnalisés, et de suivre leur stock
                et leur état.
              </p>
              <h3 className="text-base font-semibold text-foreground mt-4 mb-2">
                Offre gratuite
              </h3>
              <ul className="list-disc pl-5 space-y-1">
                <li>Création de compte avec email ou Google OAuth</li>
                <li>Jusqu'à 5 listes d'inventaire</li>
                <li>Articles illimités par liste</li>
                <li>Champs personnalisés (texte, nombre, date, booléen)</li>
                <li>Images par article</li>
              </ul>
              <h3 className="text-base font-semibold text-foreground mt-4 mb-2">
                Offre Premium
              </h3>
              <ul className="list-disc pl-5 space-y-1">
                <li>Toutes les fonctionnalités de l'offre gratuite</li>
                <li>Listes d'inventaire illimitées</li>
                <li>Paiement unique (voir section 5)</li>
              </ul>
              <p className="mt-2">
                Le service est fourni en tant que contenu numérique au sens de
                la Directive (UE) 2019/770.
              </p>
            </section>

            {/* 3. Acceptation des conditions */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                3. Acceptation des conditions
              </h2>
              <p>
                L'utilisation du service implique l'acceptation pleine et
                entière des présentes conditions générales d'utilisation. Si
                vous n'acceptez pas ces conditions, vous ne devez pas utiliser
                le service.
              </p>
              <p className="mt-2">
                L'inscription est réservée aux personnes physiques âgées d'au
                moins 15 ans, conformément à l'article 7-1 de la loi
                Informatique et Libertés (âge du consentement numérique en
                France).
              </p>
            </section>

            {/* 4. Compte utilisateur */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                4. Compte utilisateur
              </h2>
              <p>
                Pour accéder au service, vous devez créer un compte en
                fournissant une adresse email valide et un mot de passe, ou en
                vous connectant via Google OAuth. Vous êtes responsable de :
              </p>
              <ul className="list-disc pl-5 space-y-1 mt-2">
                <li>La confidentialité de vos identifiants de connexion</li>
                <li>Toute activité effectuée sous votre compte</li>
                <li>
                  La notification immédiate en cas d'utilisation non autorisée
                  de votre compte
                </li>
              </ul>
              <p className="mt-2">
                Nous nous réservons le droit de suspendre ou supprimer un compte
                en cas de violation des présentes conditions, après notification
                préalable sauf en cas d'urgence ou de violation grave.
              </p>
            </section>

            {/* 5. Prix et paiement */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                5. Prix et paiement
              </h2>
              <p>
                La mise à niveau Premium est proposée au prix de{" "}
                <strong>2,00 EUR TTC</strong> (toutes taxes comprises), en
                paiement unique et non récurrent.
              </p>
              <p className="mt-2">
                Le paiement est traité par <strong>Stripe</strong>, notre
                prestataire de paiement sécurisé. Vos données bancaires ne
                transitent jamais par nos serveurs. Les moyens de paiement
                acceptés sont ceux proposés par Stripe (carte bancaire
                notamment).
              </p>
              <p className="mt-2">
                Le prix est indiqué en euros et inclut toutes les taxes
                applicables. Aucun frais supplémentaire n'est appliqué au-delà
                du prix affiché.
              </p>
            </section>

            {/* 6. Droit de rétractation */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                6. Droit de rétractation
              </h2>
              <p>
                Conformément aux articles L221-18 et suivants du Code de la
                consommation, vous disposez d'un délai de 14 jours à compter de
                la conclusion du contrat pour exercer votre droit de
                rétractation, sans avoir à justifier de motifs ni à payer de
                pénalités.
              </p>
              <p className="mt-3">
                <strong>Exception pour le contenu numérique :</strong>{" "}
                Conformément à l'article L221-28, 13° du Code de la
                consommation, le droit de rétractation ne peut être exercé pour
                les contrats de fourniture de contenu numérique non fourni sur
                un support matériel dont l'exécution a commencé avec votre
                accord préalable et exprès et pour lequel vous avez renoncé à
                votre droit de rétractation.
              </p>
              <p className="mt-2">
                Lors de l'achat Premium, vous serez invité à cocher une case
                confirmant que vous souhaitez que le service commence
                immédiatement et que vous renoncez à votre droit de rétractation
                de 14 jours. Sans cette confirmation, le paiement ne pourra pas
                être effectué.
              </p>
              <p className="mt-2">
                Un email de confirmation vous sera envoyé après l'achat,
                récapitulant votre commande et la renonciation au droit de
                rétractation.
              </p>
            </section>

            {/* 7. Garantie de conformité du contenu numérique */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                7. Garantie de conformité du contenu numérique
              </h2>
              <p>
                Conformément aux articles L224-25-1 et suivants du Code de la
                consommation (transposition de la Directive (UE) 2019/770), le
                service Premium est garanti conforme au contrat. En cas de
                défaut de conformité, vous avez droit à la mise en conformité du
                service ou, à défaut, à une réduction de prix ou à la résolution
                du contrat.
              </p>
              <p className="mt-2">Le service est considéré conforme s'il :</p>
              <ul className="list-disc pl-5 space-y-1 mt-1">
                <li>Correspond à la description donnée (listes illimitées)</li>
                <li>
                  Est propre à l'usage habituellement attendu d'un service
                  similaire
                </li>
                <li>
                  Est fourni avec les mises à jour nécessaires pendant la durée
                  du contrat
                </li>
              </ul>
              <p className="mt-2">
                En cas de défaut de conformité constaté, contactez-nous à
                l'adresse email indiquée en section 1.
              </p>
            </section>

            {/* 8. Remboursement */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                8. Politique de remboursement
              </h2>
              <p>
                Si un remboursement est accordé (hors exercice du droit de
                rétractation auquel vous avez renoncé conformément à la section
                6), votre compte sera rétrogradé de Premium à l'offre gratuite.
                Vous conserverez vos données existantes mais serez soumis à la
                limite de 5 listes pour toute nouvelle création.
              </p>
              <p className="mt-2">
                Le remboursement sera effectué via Stripe sur le moyen de
                paiement utilisé lors de l'achat.
              </p>
            </section>

            {/* 9. Propriété du contenu */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                9. Propriété du contenu utilisateur
              </h2>
              <p>
                Vous conservez l'intégralité des droits de propriété
                intellectuelle sur les données, images et contenus que vous
                ajoutez au service. Nous ne revendiquons aucun droit sur votre
                contenu.
              </p>
              <p className="mt-2">
                Vous nous accordez une licence limitée, non exclusive, pour
                héberger et afficher votre contenu dans le seul but de vous
                fournir le service. Cette licence prend fin à la suppression de
                votre compte.
              </p>
            </section>

            {/* 10. Utilisation acceptable */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                10. Utilisation acceptable
              </h2>
              <p>
                Ce service est destiné à la gestion d'inventaire personnel ou
                professionnel. Vous vous engagez à ne pas :
              </p>
              <ul className="list-disc pl-5 space-y-1 mt-2">
                <li>
                  Stocker du contenu illégal, offensant ou portant atteinte aux
                  droits d'autrui
                </li>
                <li>Tenter d'accéder aux données d'autres utilisateurs</li>
                <li>
                  Utiliser le service de manière à compromettre sa sécurité ou
                  sa disponibilité
                </li>
                <li>
                  Contourner les limitations techniques du service (notamment la
                  limite de listes)
                </li>
              </ul>
            </section>

            {/* 11. Disponibilité et maintenance */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                11. Disponibilité et maintenance
              </h2>
              <p>
                Nous nous efforçons de maintenir le service disponible 24h/24 et
                7j/7. Toutefois, le service peut être temporairement interrompu
                pour des raisons de maintenance, de mise à jour ou en cas de
                force majeure. Nous nous engageons à informer les utilisateurs
                en cas d'interruption programmée dans la mesure du possible.
              </p>
            </section>

            {/* 12. Données personnelles */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                12. Données personnelles
              </h2>
              <p>
                Le traitement de vos données personnelles est régi par notre{" "}
                <a
                  href="/privacy"
                  className="text-foreground underline underline-offset-2 hover:text-brand"
                >
                  Politique de confidentialité
                </a>
                , qui fait partie intégrante des présentes conditions.
              </p>
            </section>

            {/* 13. Limitation de responsabilité */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                13. Limitation de responsabilité
              </h2>
              <p>
                Dans les limites autorisées par la loi et sans préjudice des
                droits des consommateurs prévus par le Code de la consommation :
              </p>
              <ul className="list-disc pl-5 space-y-1 mt-2">
                <li>
                  Le service est fourni « en l'état ». Nous ne garantissons pas
                  que le service sera exempt d'erreurs ou disponible sans
                  interruption.
                </li>
                <li>
                  Notre responsabilité est limitée au montant que vous avez payé
                  pour le service (2,00 EUR pour les utilisateurs Premium, 0 EUR
                  pour les utilisateurs gratuits).
                </li>
                <li>
                  Nous ne sommes pas responsables des dommages indirects, sauf
                  en cas de faute lourde ou intentionnelle.
                </li>
              </ul>
              <p className="mt-2">
                Aucune clause des présentes conditions ne limite votre droit à
                la garantie légale de conformité ni vos droits en tant que
                consommateur.
              </p>
            </section>

            {/* 14. Suppression de compte */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                14. Suppression de compte et portabilité
              </h2>
              <p>
                Vous pouvez supprimer votre compte à tout moment depuis
                l'application. La suppression entraîne l'effacement définitif et
                irréversible de toutes vos données (listes, articles, images).
              </p>
              <p className="mt-2">
                Avant la suppression, vous pouvez exporter vos données dans un
                format structuré via la fonction d'export disponible dans votre
                compte, conformément à votre droit à la portabilité des données
                (Art. 20 RGPD).
              </p>
            </section>

            {/* 15. Force majeure */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                15. Force majeure
              </h2>
              <p>
                Nous ne saurions être tenus responsables de l'inexécution ou du
                retard dans l'exécution de nos obligations en cas de force
                majeure au sens de l'article 1218 du Code civil, notamment en
                cas de catastrophe naturelle, pandémie, panne d'infrastructure
                tierce, ou décision gouvernementale.
              </p>
            </section>

            {/* 16. Droit applicable et litiges */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                16. Droit applicable et règlement des litiges
              </h2>
              <p>
                Les présentes conditions sont régies par le droit français. Tout
                litige relatif à leur interprétation ou à leur exécution relève
                de la compétence des tribunaux français.
              </p>
              <p className="mt-2">
                En cas de litige, vous pouvez recourir gratuitement à un
                médiateur de la consommation. Conformément au Règlement (UE) n°
                524/2013, la Commission européenne met à disposition une
                plateforme de règlement en ligne des litiges :{" "}
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

            {/* 17. Divisibilité */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                17. Divisibilité
              </h2>
              <p>
                Si une disposition des présentes conditions est jugée invalide
                ou inapplicable par un tribunal compétent, les autres
                dispositions resteront en vigueur. La disposition invalide sera
                remplacée par une disposition valide se rapprochant le plus
                possible de l'intention originale.
              </p>
            </section>

            {/* 18. Modifications des conditions */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                18. Modifications des conditions
              </h2>
              <p>
                Nous nous réservons le droit de modifier les présentes
                conditions. En cas de modification substantielle, nous vous en
                informerons par email ou par une notification dans l'application
                au moins 30 jours avant l'entrée en vigueur des nouvelles
                conditions.
              </p>
              <p className="mt-2">
                Si vous n'acceptez pas les nouvelles conditions, vous pouvez
                supprimer votre compte avant leur entrée en vigueur.
                L'utilisation continue du service après la date d'entrée en
                vigueur vaut acceptation des nouvelles conditions.
              </p>
            </section>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
