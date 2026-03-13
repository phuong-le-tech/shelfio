import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { motion } from 'motion/react';
import { Button } from '@/components/ui/button';

export function PrivacyPolicy() {
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
            Politique de confidentialité
          </h1>

          <div className="prose prose-sm text-muted-foreground space-y-6">
            <p className="text-sm text-muted-foreground/70">
              Dernière mise à jour : mars 2026
            </p>

            {/* 1. Responsable du traitement */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                1. Responsable du traitement
              </h2>
              <p>
                Le responsable du traitement de vos données personnelles est :
              </p>
              <ul className="list-none pl-0 space-y-1 mt-2">
                <li><strong>Nom :</strong> Phuong LE</li>
                <li><strong>Adresse :</strong> 6 avenue de la concorde, Meaux, France</li>
                <li><strong>Email :</strong> phuongle.tech@gmail.com</li>
              </ul>
              <p className="mt-2">
                Pour toute question relative à la protection de vos données, vous pouvez
                contacter le responsable du traitement à l'adresse email indiquée ci-dessus.
              </p>
            </section>

            {/* 2. Données collectées et finalités */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                2. Données collectées et finalités
              </h2>
              <p>
                Nous collectons et traitons les données suivantes, chacune pour une finalité
                précise et sur une base légale définie :
              </p>

              <h3 className="text-base font-semibold text-foreground mt-4 mb-2">
                Données de compte
              </h3>
              <ul className="list-disc pl-5 space-y-1">
                <li>
                  <strong>Adresse email et mot de passe</strong> — Création et gestion de votre
                  compte. Base légale : exécution du contrat (Art. 6(1)(b) RGPD).
                </li>
                <li>
                  <strong>Données Google OAuth</strong> (nom, email, photo de profil) — Si vous
                  choisissez de vous connecter avec Google. Base légale : consentement
                  (Art. 6(1)(a) RGPD). Vous pouvez retirer ce consentement à tout moment en
                  supprimant votre compte.
                </li>
              </ul>

              <h3 className="text-base font-semibold text-foreground mt-4 mb-2">
                Données d'inventaire
              </h3>
              <ul className="list-disc pl-5 space-y-1">
                <li>
                  <strong>Listes, articles, champs personnalisés, images</strong> — Fourniture
                  du service de gestion d'inventaire. Base légale : exécution du contrat
                  (Art. 6(1)(b) RGPD).
                </li>
              </ul>

              <h3 className="text-base font-semibold text-foreground mt-4 mb-2">
                Données de paiement
              </h3>
              <ul className="list-disc pl-5 space-y-1">
                <li>
                  <strong>Identifiant client Stripe et identifiant de paiement</strong> — Traitement
                  du paiement unique pour la mise à niveau Premium. Les données bancaires sont
                  traitées exclusivement par Stripe et ne transitent jamais par nos serveurs.
                  Base légale : exécution du contrat (Art. 6(1)(b) RGPD).
                </li>
              </ul>

              <h3 className="text-base font-semibold text-foreground mt-4 mb-2">
                Données techniques
              </h3>
              <ul className="list-disc pl-5 space-y-1">
                <li>
                  <strong>Données de suivi d'erreurs (Sentry)</strong> — Identification et
                  correction des bugs. Collecte soumise à votre consentement via la bannière
                  de cookies. Base légale : consentement (Art. 6(1)(a) RGPD).
                </li>
              </ul>
            </section>

            {/* 3. Destinataires et sous-traitants */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                3. Destinataires et sous-traitants
              </h2>
              <p>
                Vos données peuvent être transmises aux sous-traitants suivants, chacun
                soumis à des obligations contractuelles de protection des données :
              </p>
              <div className="overflow-x-auto mt-3">
                <table className="w-full text-sm border-collapse">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-2 pr-4 font-semibold text-foreground">Sous-traitant</th>
                      <th className="text-left py-2 pr-4 font-semibold text-foreground">Finalité</th>
                      <th className="text-left py-2 font-semibold text-foreground">Localisation</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    <tr>
                      <td className="py-2 pr-4">Railway</td>
                      <td className="py-2 pr-4">Hébergement de l'application et de la base de données</td>
                      <td className="py-2">USA</td>
                    </tr>
                    <tr>
                      <td className="py-2 pr-4">Stripe</td>
                      <td className="py-2 pr-4">Traitement des paiements</td>
                      <td className="py-2">Irlande / USA</td>
                    </tr>
                    <tr>
                      <td className="py-2 pr-4">Google</td>
                      <td className="py-2 pr-4">Authentification OAuth</td>
                      <td className="py-2">USA</td>
                    </tr>
                    <tr>
                      <td className="py-2 pr-4">Sentry</td>
                      <td className="py-2 pr-4">Suivi des erreurs (avec votre consentement)</td>
                      <td className="py-2">USA</td>
                    </tr>
                    <tr>
                      <td className="py-2 pr-4">Resend</td>
                      <td className="py-2 pr-4">Envoi d'emails transactionnels</td>
                      <td className="py-2">USA</td>
                    </tr>
                    <tr>
                      <td className="py-2 pr-4">Fontshare (Indian Type Foundry)</td>
                      <td className="py-2 pr-4">Chargement de polices de caractères</td>
                      <td className="py-2">Inde</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            {/* 4. Transferts internationaux */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                4. Transferts internationaux de données
              </h2>
              <p>
                Certains de nos sous-traitants sont situés en dehors de l'Espace Économique
                Européen (EEE), notamment aux États-Unis. Ces transferts sont encadrés par :
              </p>
              <ul className="list-disc pl-5 space-y-1 mt-2">
                <li>
                  Le <strong>EU-U.S. Data Privacy Framework</strong> pour les entreprises
                  certifiées (Stripe, Google, Sentry)
                </li>
                <li>
                  Les <strong>Clauses Contractuelles Types (CCT)</strong> approuvées par la
                  Commission européenne
                </li>
              </ul>
            </section>

            {/* 5. Durées de conservation */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                5. Durées de conservation
              </h2>
              <ul className="list-disc pl-5 space-y-1">
                <li>
                  <strong>Données de compte et d'inventaire :</strong> conservées tant que
                  votre compte est actif, puis supprimées lors de la suppression de votre
                  compte.
                </li>
                <li>
                  <strong>Jetons de vérification :</strong> supprimés automatiquement après
                  expiration (24h pour la vérification email, 30min pour la réinitialisation
                  du mot de passe) et par un nettoyage quotidien.
                </li>
                <li>
                  <strong>Événements webhook Stripe :</strong> conservés 90 jours à des fins
                  de traçabilité, puis supprimés automatiquement.
                </li>
                <li>
                  <strong>Données Stripe :</strong> les données détenues par Stripe sont
                  soumises à leur propre politique de conservation, conformément à leurs
                  obligations légales.
                </li>
                <li>
                  <strong>Journaux applicatifs :</strong> conservés 30 jours maximum,
                  puis supprimés automatiquement.
                </li>
                <li>
                  <strong>Données Sentry :</strong> conservées selon la configuration de
                  rétention Sentry (90 jours par défaut).
                </li>
              </ul>
            </section>

            {/* 6. Cookies */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                6. Cookies et technologies similaires
              </h2>

              <h3 className="text-base font-semibold text-foreground mt-3 mb-2">
                Cookies strictement nécessaires (sans consentement)
              </h3>
              <ul className="list-disc pl-5 space-y-1">
                <li>
                  <strong>access_token</strong> — Cookie d'authentification HttpOnly, Secure,
                  SameSite=Lax. Expire après 24 heures. Indispensable au fonctionnement du
                  service.
                </li>
                <li>
                  <strong>cookie_consent</strong> — Stockage local (localStorage) enregistrant
                  votre choix de consentement aux cookies. Indispensable pour respecter votre
                  préférence.
                </li>
              </ul>

              <h3 className="text-base font-semibold text-foreground mt-3 mb-2">
                Cookies optionnels (avec consentement)
              </h3>
              <ul className="list-disc pl-5 space-y-1">
                <li>
                  <strong>Sentry</strong> — Cookies et données de performance pour le suivi
                  des erreurs. Activés uniquement si vous acceptez via la bannière de cookies.
                  Vous pouvez modifier votre choix à tout moment en supprimant le stockage
                  local « cookie_consent » et en rechargeant la page.
                </li>
              </ul>
              <p className="mt-2">
                Aucun cookie publicitaire ou de suivi marketing n'est utilisé.
              </p>
            </section>

            {/* 7. Vos droits */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                7. Vos droits
              </h2>
              <p>
                Conformément au RGPD et à la loi Informatique et Libertés, vous disposez des
                droits suivants :
              </p>
              <ul className="list-disc pl-5 space-y-1 mt-2">
                <li>
                  <strong>Droit d'accès</strong> (Art. 15) — Obtenir une copie de vos données
                  personnelles.
                </li>
                <li>
                  <strong>Droit de rectification</strong> (Art. 16) — Corriger des données
                  inexactes ou incomplètes.
                </li>
                <li>
                  <strong>Droit à l'effacement</strong> (Art. 17) — Demander la suppression de
                  vos données. Vous pouvez supprimer votre compte directement depuis
                  l'application.
                </li>
                <li>
                  <strong>Droit à la limitation du traitement</strong> (Art. 18) — Demander la
                  restriction du traitement de vos données dans certaines circonstances.
                </li>
                <li>
                  <strong>Droit à la portabilité</strong> (Art. 20) — Recevoir vos données dans
                  un format structuré et lisible par machine. Disponible via la fonction
                  d'export de votre compte.
                </li>
                <li>
                  <strong>Droit d'opposition</strong> (Art. 21) — Vous opposer au traitement de
                  vos données fondé sur un intérêt légitime.
                </li>
                <li>
                  <strong>Retrait du consentement</strong> (Art. 7(3)) — Retirer à tout moment
                  votre consentement pour les traitements fondés sur celui-ci (Google OAuth,
                  Sentry), sans affecter la licéité du traitement antérieur.
                </li>
              </ul>
              <p className="mt-3">
                Pour exercer ces droits, contactez-nous à l'adresse email indiquée en
                section 1. Nous répondrons dans un délai de 30 jours.
              </p>
              <p className="mt-2">
                Vous disposez également du droit d'introduire une réclamation auprès de la
                CNIL (Commission Nationale de l'Informatique et des Libertés) :
              </p>
              <ul className="list-none pl-0 mt-1">
                <li>3 Place de Fontenoy, TSA 80715, 75334 Paris Cedex 07</li>
                <li>
                  <a
                    href="https://www.cnil.fr"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-foreground underline underline-offset-2 hover:text-brand"
                  >
                    www.cnil.fr
                  </a>
                </li>
              </ul>
            </section>

            {/* 8. Décisions automatisées */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                8. Décisions automatisées
              </h2>
              <p>
                Aucune décision automatisée au sens de l'article 22 du RGPD (y compris le
                profilage) n'est mise en œuvre dans le cadre de ce service.
              </p>
            </section>

            {/* 9. Sécurité */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                9. Sécurité des données
              </h2>
              <p>
                Nous mettons en œuvre des mesures techniques et organisationnelles
                appropriées pour protéger vos données :
              </p>
              <ul className="list-disc pl-5 space-y-1 mt-2">
                <li>Chiffrement des mots de passe (BCrypt)</li>
                <li>Communications chiffrées (HTTPS/TLS)</li>
                <li>Cookies HttpOnly et Secure avec SameSite=Lax</li>
                <li>Limitation du débit (rate limiting) contre les abus</li>
                <li>Contrôle d'accès basé sur les rôles</li>
                <li>Verrouillage optimiste des données</li>
              </ul>
            </section>

            {/* 10. Modifications */}
            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                10. Modifications de cette politique
              </h2>
              <p>
                Nous pouvons mettre à jour cette politique de confidentialité. En cas de
                modification substantielle, nous vous en informerons par email ou par une
                notification dans l'application. La date de dernière mise à jour est indiquée
                en haut de cette page.
              </p>
            </section>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
