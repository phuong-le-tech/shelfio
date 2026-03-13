import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Package,
  Inbox,
  AlertTriangle,
  XCircle,
  Plus,
  Pencil,
  Eye,
} from "lucide-react";
import { dashboardApi } from "../services/api";
import { queryKeys } from "../lib/queryKeys";
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
import { STATUS_LABELS, STATUS_BADGE_VARIANTS, ItemStatus } from "../types/item";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import ListCombobox from "../components/ListCombobox";

export default function Dashboard() {
  const { data: stats, isLoading: loading, error, refetch: reload } = useQuery({
    queryKey: queryKeys.dashboard.stats(),
    queryFn: () => dashboardApi.getStats(),
  });
  const navigate = useNavigate();
  const [showAddItem, setShowAddItem] = useState(false);
  const [selectedListId, setSelectedListId] = useState("");

  // Reset state on unmount to prevent stale state on navigation
  useEffect(() => {
    return () => {
      setShowAddItem(false);
      setSelectedListId("");
    };
  }, []);

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
        <Button onClick={() => reload()}>Réessayer</Button>
      </div>
    );
  }

  const statCards = [
    {
      label: "Total articles",
      value: stats?.totalItems || 0,
      subtext: "Toutes listes confondues",
      icon: Package,
      iconColor: "text-muted-foreground",
    },
    {
      label: "Quantité totale",
      value: stats?.totalQuantity || 0,
      subtext: "Unités en inventaire",
      icon: Inbox,
      iconColor: "text-muted-foreground",
    },
    {
      label: "À vérifier",
      value: stats?.toVerifyCount || 0,
      subtext: "Articles à contrôler",
      icon: AlertTriangle,
      iconColor: "text-amber-500",
    },
    {
      label: "Attention requise",
      value: stats?.needsAttentionCount || 0,
      subtext: "Maintenance ou endommagé",
      icon: XCircle,
      iconColor: "text-red-500",
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
    <div className="max-w-7xl mx-auto space-y-10 pb-10">
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
          {showAddItem ? (
            <div className="flex items-center gap-2 w-64">
              <div className="flex-1">
                <ListCombobox
                  value={selectedListId}
                  onChange={(id) => {
                    setSelectedListId(id);
                    if (id) {
                      navigate(`/lists/${id}/items/new`);
                      setShowAddItem(false);
                      setSelectedListId("");
                    }
                  }}
                />
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setShowAddItem(false);
                  setSelectedListId("");
                }}
              >
                Annuler
              </Button>
            </div>
          ) : (
            <Button className="font-medium" onClick={() => setShowAddItem(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Ajouter un article
            </Button>
          )}
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
                <card.icon className={`h-4 w-4 ${card.iconColor}`} />
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

      {/* Onboarding empty state */}
      {stats?.totalItems === 0 && (
        <BlurFade delay={0.4}>
          <div className="rounded-xl border bg-card p-8 text-center shadow-sm">
            <div className="w-14 h-14 bg-brand/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <Package className="h-7 w-7 text-brand" />
            </div>
            <h2 className="font-display text-xl font-semibold tracking-tight mb-2">
              Bienvenue sur votre tableau de bord
            </h2>
            <p className="text-muted-foreground mb-6 max-w-md mx-auto">
              Commencez par créer votre première liste pour organiser vos articles et suivre votre inventaire.
            </p>
            <Button asChild>
              <Link to="/lists/new">
                <Plus className="h-4 w-4 mr-2" />
                Créer ma première liste
              </Link>
            </Button>
          </div>
        </BlurFade>
      )}

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
                        onClick={() => navigate(`/lists/${item.listId}/items/${item.id}/edit`)}
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
                        <td className="px-5 py-4 font-medium text-foreground">
                          {item.quantity}
                        </td>
                        <td className="px-5 py-4">
                          <Badge variant={STATUS_BADGE_VARIANTS[item.status as ItemStatus] || "default"}>
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
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="icon"
                                aria-label="Options de l'article"
                                className="h-8 w-8 text-muted-foreground opacity-100 md:opacity-0 md:group-hover:opacity-100 focus-visible:opacity-100 transition-opacity"
                                onClick={(e) => e.stopPropagation()}
                              >
                                <Eye className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" onClick={(e) => e.stopPropagation()}>
                              <DropdownMenuItem onClick={() => navigate(`/lists/${item.listId}`)}>
                                <Eye className="h-4 w-4 mr-2" />
                                Voir la liste
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => navigate(`/lists/${item.listId}/items/${item.id}/edit`)}>
                                <Pencil className="h-4 w-4 mr-2" />
                                Modifier l'article
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
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
