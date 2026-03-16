import { useState, useEffect, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Package,
  Layers,
  AlertTriangle,
  AlertCircle,
  Plus,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
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

  const statusChartData = useMemo(() => {
    if (!stats?.countByStatus) return [];
    const statusConfig: Record<string, { label: string; fill: string }> = {
      AVAILABLE: { label: "Disponible", fill: "#22c55e" },
      TO_VERIFY: { label: "À vérifier", fill: "#f59e0b" },
      NEEDS_MAINTENANCE: { label: "Maintenance", fill: "#6366f1" },
      DAMAGED: { label: "Endommagé", fill: "#ef4444" },
    };
    return Object.entries(stats.countByStatus)
      .map(([key, count]) => ({
        name: statusConfig[key]?.label ?? key,
        count,
        fill: statusConfig[key]?.fill ?? "#94a3b8",
      }))
      .filter((d) => d.count > 0);
  }, [stats?.countByStatus]);

  const categoryData = useMemo(() => {
    if (!stats?.countByCategory) return [];
    const colors = ["#ef4444", "#22c55e", "#3b82f6", "#f59e0b", "#8b5cf6", "#ec4899"];
    return Object.entries(stats.countByCategory)
      .sort((a, b) => b[1] - a[1])
      .slice(0, 6)
      .map(([name, count], i) => ({
        name: name || "Autre",
        count,
        color: colors[i % colors.length],
      }));
  }, [stats?.countByCategory]);

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
      icon: Package,
      iconColor: "text-green-500",
      iconBg: "bg-green-50 dark:bg-green-950",
    },
    {
      label: "Quantité totale",
      value: stats?.totalQuantity || 0,
      icon: Layers,
      iconColor: "text-indigo-500",
      iconBg: "bg-indigo-100 dark:bg-indigo-950",
    },
    {
      label: "À vérifier",
      value: stats?.toVerifyCount || 0,
      icon: AlertTriangle,
      iconColor: "text-amber-500",
      iconBg: "bg-amber-50 dark:bg-amber-950",
    },
    {
      label: "Attention requise",
      value: stats?.needsAttentionCount || 0,
      icon: AlertCircle,
      iconColor: "text-red-500",
      iconBg: "bg-red-50 dark:bg-red-950",
    },
  ];

  return (
    <div className="max-w-7xl mx-auto space-y-10 pb-10">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <BlurFade delay={0.1}>
            <h1 className="font-display text-[28px] font-bold tracking-tight">
              Tableau de bord
            </h1>
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
            <div className="rounded-2xl bg-card p-5 h-full flex flex-col gap-2.5">
              <div className={`w-10 h-10 rounded-[10px] flex items-center justify-center ${card.iconBg}`}>
                <card.icon className={`h-5 w-5 ${card.iconColor}`} />
              </div>
              <div className="font-display text-[32px] font-extrabold tracking-tight leading-none">
                {card.value.toLocaleString("fr-FR")}
              </div>
              <div className="text-[13px] font-medium text-muted-foreground">
                {card.label}
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

      {/* Charts: Status + Categories side by side */}
      {stats && stats.totalItems > 0 && (statusChartData.length > 0 || categoryData.length > 0) && (
        <BlurFade delay={0.4}>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {statusChartData.length > 0 && (
              <section className="rounded-2xl bg-card p-5">
                <h2 className="font-display text-lg font-bold tracking-tight text-foreground mb-4">
                  Articles par statut
                </h2>
                <ResponsiveContainer width="100%" height={200}>
                  <BarChart data={statusChartData}>
                    <XAxis dataKey="name" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                    <YAxis hide />
                    <Tooltip
                      contentStyle={{ borderRadius: "0.75rem", border: "1px solid hsl(var(--border))", background: "hsl(var(--card))" }}
                      labelStyle={{ fontWeight: 600 }}
                    />
                    <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                      {statusChartData.map((entry, i) => (
                        <Cell key={i} fill={entry.fill} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </section>
            )}

            {categoryData.length > 0 && (
              <section className="rounded-2xl bg-card p-5">
                <h2 className="font-display text-lg font-bold tracking-tight text-foreground mb-4">
                  Articles par catégorie
                </h2>
                <div className="space-y-3">
                  {categoryData.map((cat) => (
                    <div key={cat.name} className="flex items-center gap-3">
                      <div className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: cat.color }} />
                      <span className="text-sm text-foreground flex-1">{cat.name}</span>
                      <span className="text-sm font-medium text-foreground">{cat.count}</span>
                    </div>
                  ))}
                </div>
              </section>
            )}
          </div>
        </BlurFade>
      )}

      {/* Tables Row: Lists Overview + Recently Updated side by side */}
      {((stats?.listsOverview && stats.listsOverview.length > 0) || (stats?.recentlyUpdated && stats.recentlyUpdated.length > 0)) && (
        <BlurFade delay={0.45}>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {/* Lists Overview */}
            {stats?.listsOverview && stats.listsOverview.length > 0 && (
              <section className="rounded-2xl bg-card p-5 flex flex-col gap-4">
                <div className="flex items-center justify-between">
                  <h2 className="font-display text-lg font-bold tracking-tight text-foreground">
                    Aperçu des listes
                  </h2>
                  <Link to="/lists" className="text-[13px] font-medium text-brand hover:opacity-80 transition-opacity">
                    Voir tout →
                  </Link>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-left">
                    <thead>
                      <tr className="text-xs font-semibold text-[hsl(var(--text-tertiary))]">
                        <th className="py-2 font-semibold">Nom</th>
                        <th className="py-2 font-semibold w-[100px]">Articles</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stats.listsOverview.map((list) => (
                        <tr key={list.listName} className="border-t border-border/50">
                          <td className="py-2.5">
                            <span className="text-[13px] font-medium text-foreground">{list.listName}</span>
                          </td>
                          <td className="py-2.5">
                            <span className="text-[13px] font-medium text-muted-foreground">{list.itemsCount}</span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}

            {/* Recently Updated */}
            {stats?.recentlyUpdated && stats.recentlyUpdated.length > 0 && (
              <section className="rounded-2xl bg-card p-5 flex flex-col gap-4">
                <h2 className="font-display text-lg font-bold tracking-tight text-foreground">
                  Récemment mis à jour
                </h2>
                <div className="overflow-x-auto">
                  <table className="w-full text-left">
                    <thead>
                      <tr className="text-xs font-semibold text-[hsl(var(--text-tertiary))]">
                        <th className="py-2 font-semibold">Article</th>
                        <th className="py-2 font-semibold w-[100px]">Statut</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stats.recentlyUpdated.map((item) => (
                        <tr
                          key={item.id}
                          role="button"
                          tabIndex={0}
                          aria-label={`Modifier l'article ${item.name}`}
                          className="border-t border-border/50 hover:bg-muted/30 transition-colors cursor-pointer"
                          onClick={() => navigate(`/lists/${item.listId}/items/${item.id}/edit`)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter' || e.key === ' ') {
                              e.preventDefault();
                              navigate(`/lists/${item.listId}/items/${item.id}/edit`);
                            }
                          }}
                        >
                          <td className="py-2.5">
                            <span className="text-[13px] font-medium text-foreground">{item.name}</span>
                          </td>
                          <td className="py-2.5">
                            <Badge variant={STATUS_BADGE_VARIANTS[item.status as ItemStatus] || "default"}>
                              {STATUS_LABELS[item.status as ItemStatus] || item.status}
                            </Badge>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}
          </div>
        </BlurFade>
      )}
    </div>
  );
}
