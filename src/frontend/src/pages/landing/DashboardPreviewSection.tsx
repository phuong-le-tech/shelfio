import {
  Package,
  Inbox,
  AlertTriangle,
  XCircle,
} from "lucide-react";
import { BlurFade } from "@/components/effects/blur-fade";
import {
  StaggeredList,
  StaggeredItem,
} from "@/components/effects/staggered-list";
import {
  MOCK_STATS,
  MOCK_LISTS_OVERVIEW,
  MOCK_RECENT_ITEMS,
} from "./mockData";

const STATUS_LABELS: Record<string, string> = {
  IN_STOCK: "En stock",
  LOW_STOCK: "Stock faible",
  OUT_OF_STOCK: "Rupture de stock",
};

const STATUS_COLORS: Record<string, string> = {
  IN_STOCK: "bg-emerald-500/15 text-emerald-400",
  LOW_STOCK: "bg-amber-500/15 text-amber-400",
  OUT_OF_STOCK: "bg-red-500/15 text-red-400",
};

const LIST_COLORS = [
  "bg-blue-500",
  "bg-emerald-500",
  "bg-amber-500",
];

const statCards = [
  {
    label: "Total articles",
    value: MOCK_STATS.totalItems.toLocaleString("fr-FR"),
    subtext: "Toutes listes confondues",
    icon: Package,
  },
  {
    label: "Quantite totale",
    value: MOCK_STATS.totalQuantity.toLocaleString("fr-FR"),
    subtext: "Unites en inventaire",
    icon: Inbox,
  },
  {
    label: "Stock faible",
    value: MOCK_STATS.lowStockCount,
    subtext: "A reapprovisionner",
    icon: AlertTriangle,
  },
  {
    label: "Rupture de stock",
    value: MOCK_STATS.outOfStockCount,
    subtext: "Articles indisponibles",
    icon: XCircle,
  },
];

export function DashboardPreviewSection() {
  return (
    <section aria-labelledby="dashboard-heading" className="py-20 md:py-32">
      <div className="max-w-6xl mx-auto px-6">
        <BlurFade delay={0.1} inView>
          <div className="text-center mb-12 md:mb-16">
            <h2 id="dashboard-heading" className="font-display text-3xl md:text-4xl font-semibold tracking-tight mb-4">
              Tableau de bord intelligent
            </h2>
            <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
              Visualisez l'etat de votre inventaire en un coup d'oeil avec des
              statistiques detaillees et des alertes de stock.
            </p>
          </div>
        </BlurFade>

        <BlurFade delay={0.2} inView>
          <div className="rounded-2xl border bg-card overflow-hidden shadow-float">
            <div className="flex items-center gap-2 px-4 py-3 border-b bg-muted/30" aria-hidden="true">
              <div className="flex gap-1.5">
                <span className="w-3 h-3 rounded-full bg-red-500/50" />
                <span className="w-3 h-3 rounded-full bg-yellow-500/50" />
                <span className="w-3 h-3 rounded-full bg-green-500/50" />
              </div>
              <span className="text-xs text-muted-foreground ml-2">
                Tableau de bord
              </span>
            </div>

            <div className="p-5 md:p-8 space-y-8">
              <StaggeredList
                className="grid grid-cols-2 lg:grid-cols-4 gap-3 md:gap-4"
                staggerDelay={0.08}
              >
                {statCards.map((card, idx) => (
                  <StaggeredItem key={idx}>
                    <div className="rounded-xl border bg-background/50 p-4 md:p-5">
                      <div className="flex items-start justify-between mb-3 md:mb-4">
                        <span className="font-medium text-xs md:text-sm text-foreground">
                          {card.label}
                        </span>
                        <card.icon className="h-4 w-4 text-muted-foreground hidden sm:block" aria-hidden="true" />
                      </div>
                      <div className="text-2xl md:text-3xl font-display font-bold tracking-tight mb-1">
                        {card.value}
                      </div>
                      <div className="text-xs text-muted-foreground hidden sm:block">
                        {card.subtext}
                      </div>
                    </div>
                  </StaggeredItem>
                ))}
              </StaggeredList>

              <div>
                <h3 className="text-sm font-semibold text-foreground mb-3">
                  Apercu des listes
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3 md:gap-4">
                  {MOCK_LISTS_OVERVIEW.map((list, idx) => (
                    <div
                      key={idx}
                      className="rounded-xl border bg-background/50 p-4 md:p-5"
                    >
                      <div className="flex items-center gap-3 mb-4">
                        <div
                          className={`w-3 h-3 rounded-full ${LIST_COLORS[idx]}`}
                          aria-hidden="true"
                        />
                        <span className="font-medium text-sm text-foreground">
                          {list.listName}
                        </span>
                      </div>
                      <div className="space-y-2">
                        <div className="flex justify-between items-center text-sm">
                          <span className="text-muted-foreground">
                            Articles :
                          </span>
                          <span className="font-medium">{list.itemsCount}</span>
                        </div>
                        <div className="flex justify-between items-center text-sm">
                          <span className="text-muted-foreground">
                            Quantite :
                          </span>
                          <span className="font-medium">
                            {list.totalQuantity.toLocaleString("fr-FR")}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="hidden md:block">
                <h3 className="text-sm font-semibold text-foreground mb-3">
                  Recemment modifies
                </h3>
                <div className="rounded-xl border bg-background/50 overflow-hidden">
                  <table className="w-full text-sm text-left">
                    <thead className="bg-muted/30 text-muted-foreground font-medium text-xs">
                      <tr>
                        <th scope="col" className="px-5 py-3 font-medium">
                          Nom de l'article
                        </th>
                        <th scope="col" className="px-5 py-3 font-medium">Liste</th>
                        <th scope="col" className="px-5 py-3 font-medium">Quantite</th>
                        <th scope="col" className="px-5 py-3 font-medium">Statut</th>
                        <th scope="col" className="px-5 py-3 font-medium">
                          Derniere modification
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border/50">
                      {MOCK_RECENT_ITEMS.map((item, idx) => (
                        <tr key={idx} className="text-foreground">
                          <td className="px-5 py-4 font-medium">{item.name}</td>
                          <td className="px-5 py-4">
                            <div className="flex items-center gap-2">
                              <div className="w-2 h-2 rounded-full bg-blue-500" aria-hidden="true" />
                              {item.listName}
                            </div>
                          </td>
                          <td className="px-5 py-4 font-medium">
                            {item.quantity}
                          </td>
                          <td className="px-5 py-4">
                            <span
                              className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[item.status] || "bg-muted text-muted-foreground"}`}
                            >
                              {STATUS_LABELS[item.status] || item.status}
                            </span>
                          </td>
                          <td className="px-5 py-4 text-muted-foreground">
                            {new Date(item.lastUpdated).toLocaleDateString(
                              "fr-FR",
                              {
                                month: "short",
                                day: "numeric",
                                year: "numeric",
                              },
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div className="h-16 bg-gradient-to-t from-card to-transparent -mt-16 relative z-10 pointer-events-none" />
          </div>
        </BlurFade>
      </div>
    </section>
  );
}
