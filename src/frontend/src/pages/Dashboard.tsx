import { Link, useNavigate } from "react-router-dom";
import {
  Package,
  Inbox,
  AlertTriangle,
  XCircle,
  MoreVertical,
  Plus,
} from "lucide-react";
import { useDashboardStats } from "../hooks/useDashboardStats";
import {
  SkeletonStatCard,
  SkeletonText,
  Skeleton,
} from "../components/Skeleton";
import { Button } from "@/components/ui/button";
import { BlurFade } from "@/components/effects/blur-fade";
import {
  StaggeredList,
  StaggeredItem,
} from "@/components/effects/staggered-list";
import { STATUS_LABELS, ItemStatus } from "../types/item";
import { Badge } from "@/components/ui/badge";

const statusToBadgeVariant: Record<string, "success" | "warning" | "error"> = {
  IN_STOCK: "success",
  LOW_STOCK: "warning",
  OUT_OF_STOCK: "error",
};

export default function Dashboard() {
  const { stats, loading, error, reload } = useDashboardStats();
  const navigate = useNavigate();

  if (loading) {
    return (
      <div>
        <div className="mb-8 flex justify-between items-start">
          <div>
            <SkeletonText className="w-48 h-10 mb-2" />
            <SkeletonText className="w-64 h-4" />
          </div>
          <Skeleton className="w-28 h-10 rounded-lg" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
          {[...Array(4)].map((_, i) => (
            <SkeletonStatCard key={i} />
          ))}
        </div>
        <Skeleton className="h-48 rounded-xl mb-10" />
        <Skeleton className="h-64 rounded-xl" />
      </div>
    );
  }

  if (error && !stats) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center animate-fade-in">
        <div className="w-16 h-16 bg-destructive/10 rounded-2xl flex items-center justify-center mb-4">
          <AlertTriangle className="h-8 w-8 text-destructive" />
        </div>
        <h2 className="font-display text-xl font-semibold mb-2">
          Impossible de charger les statistiques
        </h2>
        <p className="text-muted-foreground mb-6 max-w-sm">
          Une erreur est survenue lors du chargement du tableau de bord.
        </p>
        <Button onClick={reload}>Réessayer</Button>
      </div>
    );
  }

  const statCards = [
    {
      label: "Total articles",
      value: stats?.totalItems || 0,
      subtext: "Toutes listes confondues",
      icon: Package,
    },
    {
      label: "Quantité totale",
      value: stats?.totalQuantity || 0,
      subtext: "Unités en inventaire",
      icon: Inbox,
    },
    {
      label: "Stock faible",
      value: stats?.lowStockCount || 0,
      subtext: "À réapprovisionner",
      icon: AlertTriangle,
    },
    {
      label: "Rupture de stock",
      value: stats?.outOfStockCount || 0,
      subtext: "Articles indisponibles",
      icon: XCircle,
    },
  ];

  const listColors = [
    "bg-brand",
    "bg-emerald-600",
    "bg-teal-500",
    "bg-cyan-500",
    "bg-green-600",
  ];


  return (
    <div className="space-y-10 pb-10">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <BlurFade delay={0.1}>
            <h1 className="font-display text-3xl font-semibold tracking-tight">
              Tableau de bord
            </h1>
          </BlurFade>
          <BlurFade delay={0.2}>
            <p className="text-muted-foreground mt-1">
              Aperçu de votre inventaire
            </p>
          </BlurFade>
        </div>
        <BlurFade delay={0.3}>
          <Button className="font-medium" asChild>
            <Link to="/items/new">
              <Plus className="h-4 w-4 mr-2" />
              Ajouter un article
            </Link>
          </Button>
        </BlurFade>
      </div>

      {/* Top Stats Cards */}
      <StaggeredList
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"
        staggerDelay={0.08}
      >
        {statCards.map((card, idx) => (
          <StaggeredItem key={idx}>
            <div className="rounded-xl border bg-card p-5 shadow-sm transition-all duration-200 hover:shadow-md h-full flex flex-col justify-between">
              <div className="flex items-start justify-between mb-4">
                <span className="font-medium text-sm text-foreground">
                  {card.label}
                </span>
                <card.icon className="h-4 w-4 text-muted-foreground" />
              </div>
              <div>
                <div className="text-3xl font-display font-bold tracking-tight mb-1">
                  {card.value}
                </div>
                <div className="text-xs text-muted-foreground">
                  {card.subtext}
                </div>
              </div>
            </div>
          </StaggeredItem>
        ))}
      </StaggeredList>

      {/* Lists Overview */}
      {stats?.listsOverview && stats.listsOverview.length > 0 && (
        <BlurFade delay={0.4}>
          <section>
            <div className="mb-4">
              <h2 className="text-lg font-semibold tracking-tight text-foreground">
                Aperçu des listes
              </h2>
              <p className="text-sm text-muted-foreground">
                Votre inventaire organisé par listes
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {stats.listsOverview.map((list, idx) => (
                <div
                  key={idx}
                  className="rounded-xl border bg-card p-5 shadow-sm transition-all hover:shadow-md"
                >
                  <div className="flex items-center gap-3 mb-6">
                    <div
                      className={`w-3 h-3 rounded-full ${listColors[idx % listColors.length]}`}
                    ></div>
                    <span className="font-medium text-foreground">
                      {list.listName}
                    </span>
                  </div>
                  <div className="space-y-2">
                    <div className="flex justify-between items-center text-sm">
                      <span className="text-muted-foreground">Articles :</span>
                      <span className="font-medium">{list.itemsCount}</span>
                    </div>
                    <div className="flex justify-between items-center text-sm">
                      <span className="text-muted-foreground">Quantité :</span>
                      <span className="font-medium">{list.totalQuantity}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </BlurFade>
      )}

      {/* Recently Updated Table */}
      {stats?.recentlyUpdated && stats.recentlyUpdated.length > 0 && (
        <BlurFade delay={0.5}>
          <section>
            <div className="mb-4">
              <h2 className="text-lg font-semibold tracking-tight text-foreground">
                Récemment modifiés
              </h2>
              <p className="text-sm text-muted-foreground">
                Les 5 derniers articles modifiés
              </p>
            </div>
            <div className="rounded-xl border bg-card overflow-hidden shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                  <thead className="bg-muted/30 text-muted-foreground font-medium text-xs">
                    <tr>
                      <th className="px-5 py-3 rounded-tl-xl font-medium">
                        Nom de l'article
                      </th>
                      <th className="px-5 py-3 font-medium">Liste</th>
                      <th className="px-5 py-3 font-medium hidden sm:table-cell">
                        SKU
                      </th>
                      <th className="px-5 py-3 font-medium">Quantité</th>
                      <th className="px-5 py-3 font-medium">Statut</th>
                      <th className="px-5 py-3 font-medium hidden md:table-cell">
                        Dernière modification
                      </th>
                      <th className="px-5 py-3 rounded-tr-xl"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    {stats.recentlyUpdated.map((item, idx) => (
                      <tr
                        key={idx}
                        className="hover:bg-muted/30 transition-colors group cursor-pointer"
                        onClick={() => navigate(`/items/${item.id}`)}
                      >
                        <td className="px-5 py-4">
                          <div className="font-medium text-foreground">
                            {item.name}
                          </div>
                        </td>
                        <td className="px-5 py-4">
                          <div className="flex items-center gap-2">
                            <div className="w-2 h-2 rounded-full bg-brand"></div>
                            <span className="text-foreground">
                              {item.listName}
                            </span>
                          </div>
                        </td>
                        <td className="px-5 py-4 text-muted-foreground hidden sm:table-cell">
                          {item.sku || "-"}
                        </td>
                        <td className="px-5 py-4 font-medium text-foreground">
                          {item.quantity}
                        </td>
                        <td className="px-5 py-4">
                          <Badge variant={statusToBadgeVariant[item.status] || "default"}>
                            {STATUS_LABELS[item.status as ItemStatus] ||
                              item.status}
                          </Badge>
                        </td>
                        <td className="px-5 py-4 text-muted-foreground hidden md:table-cell">
                          {new Date(item.lastUpdated).toLocaleDateString(
                            "fr-FR",
                            { month: "short", day: "numeric", year: "numeric" },
                          )}
                        </td>
                        <td className="px-5 py-4 text-right">
                          <Button
                            variant="ghost"
                            size="icon"
                            aria-label="Options de l'article"
                            className="h-8 w-8 text-muted-foreground opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity"
                          >
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </BlurFade>
      )}
    </div>
  );
}
