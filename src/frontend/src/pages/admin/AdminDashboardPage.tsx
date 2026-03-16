import { useQuery } from '@tanstack/react-query';
import { Users, UserCheck, Crown, ShieldCheck, TrendingUp, AlertTriangle } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { adminApi } from '../../services/authApi';
import { queryKeys } from '../../lib/queryKeys';
import { SkeletonStatCard, Skeleton } from '../../components/Skeleton';
import { BlurFade } from '@/components/effects/blur-fade';
import { StaggeredList, StaggeredItem } from '@/components/effects/staggered-list';
import { Button } from '@/components/ui/button';

export function AdminDashboardPage() {
  const { data: stats, isLoading, error, refetch } = useQuery({
    queryKey: queryKeys.admin.stats(),
    queryFn: () => adminApi.getStats(),
  });

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto space-y-10 pb-10">
        <div>
          <Skeleton className="w-48 h-10 mb-2" />
          <Skeleton className="w-64 h-4" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <SkeletonStatCard key={i} />
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-64 rounded-xl" />
          <Skeleton className="h-64 rounded-xl" />
        </div>
      </div>
    );
  }

  if (error && !stats) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center">
        <div className="w-16 h-16 bg-destructive/10 rounded-2xl flex items-center justify-center mb-4">
          <AlertTriangle className="h-8 w-8 text-destructive" />
        </div>
        <h2 className="font-display text-xl font-semibold mb-2">
          Impossible de charger les statistiques
        </h2>
        <p className="text-muted-foreground mb-6 max-w-sm">
          Une erreur est survenue lors du chargement du tableau de bord admin.
        </p>
        <Button onClick={() => refetch()}>Réessayer</Button>
      </div>
    );
  }

  if (!stats) return null;

  const statCards = [
    {
      label: 'Utilisateurs',
      value: stats.totalUsers,
      subtext: 'Comptes enregistrés',
      icon: Users,
      iconColor: 'text-slate-500',
      iconBg: 'bg-slate-50 dark:bg-slate-950',
    },
    {
      label: 'Actifs',
      value: stats.activeUsers,
      subtext: 'Comptes activés',
      icon: UserCheck,
      iconColor: 'text-emerald-500',
      iconBg: 'bg-emerald-50 dark:bg-emerald-950',
    },
    {
      label: 'Premium',
      value: stats.premiumUsers,
      subtext: `${stats.premiumConversionRate.toFixed(1)}% de conversion`,
      icon: Crown,
      iconColor: 'text-amber-500',
      iconBg: 'bg-amber-50 dark:bg-amber-950',
    },
    {
      label: 'Admins',
      value: stats.adminUsers,
      subtext: 'Administrateurs',
      icon: ShieldCheck,
      iconColor: 'text-blue-500',
      iconBg: 'bg-blue-50 dark:bg-blue-950',
    },
  ];

  const chartData = Object.entries(stats.registrationTrend)
    .filter(([month]) => /^\d{4}-\d{2}$/.test(month))
    .map(([month, count]) => {
      const [year, m] = month.split('-');
      const date = new Date(Number(year), Number(m) - 1);
      const label = date.toLocaleDateString('fr-FR', { month: 'short' });
      return { month: label, inscriptions: count };
    });

  return (
    <div className="max-w-7xl mx-auto space-y-10 pb-10">
      <div>
        <BlurFade delay={0.1}>
          <h1 className="font-display text-[28px] font-bold tracking-tight">
            Statistiques
          </h1>
        </BlurFade>
        <BlurFade delay={0.2}>
          <p className="text-muted-foreground mt-1">
            Vue d'ensemble de la plateforme
          </p>
        </BlurFade>
      </div>

      {/* Stat Cards */}
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
                {card.value}
              </div>
              <div className="text-[13px] font-medium text-muted-foreground">
                {card.label}
              </div>
            </div>
          </StaggeredItem>
        ))}
      </StaggeredList>

      {/* Conversion Rate */}
      <BlurFade delay={0.35}>
        <div className="rounded-2xl bg-card p-6">
          <p className="text-sm text-muted-foreground mb-1">Taux de conversion</p>
          <p className="text-4xl font-display font-bold tracking-tight">
            {stats.premiumConversionRate.toFixed(1)}%
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            des utilisateurs actifs sont Premium
          </p>
        </div>
      </BlurFade>

      {/* Registration Trend */}
      {chartData.length > 0 && (
        <BlurFade delay={0.4}>
          <section>
            <div className="mb-4">
              <h2 className="text-lg font-semibold tracking-tight text-foreground flex items-center gap-2">
                <TrendingUp className="h-5 w-5 text-muted-foreground" />
                Inscriptions récentes
              </h2>
              <p className="text-sm text-muted-foreground">
                Nouveaux comptes par mois (6 derniers mois)
              </p>
            </div>
            <div className="rounded-2xl bg-card p-6">
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={chartData} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="hsl(var(--border))" />
                  <XAxis
                    dataKey="month"
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 12, fill: 'hsl(var(--muted-foreground))' }}
                  />
                  <YAxis
                    allowDecimals={false}
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 12, fill: 'hsl(var(--muted-foreground))' }}
                  />
                  <Tooltip
                    cursor={{ fill: 'hsl(var(--muted) / 0.3)' }}
                    contentStyle={{
                      backgroundColor: 'hsl(var(--card))',
                      border: '1px solid hsl(var(--border))',
                      borderRadius: '0.75rem',
                      fontSize: '0.875rem',
                    }}
                    labelStyle={{ color: 'hsl(var(--foreground))' }}
                    formatter={(value) => [value, 'Inscriptions']}
                  />
                  <Bar
                    dataKey="inscriptions"
                    fill="hsl(var(--brand))"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={48}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </section>
        </BlurFade>
      )}

      {/* Top Users */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {stats.topUsersByLists.length > 0 && (
          <BlurFade delay={0.5}>
            <section>
              <div className="mb-4">
                <h2 className="text-lg font-semibold tracking-tight text-foreground">
                  Top utilisateurs par listes
                </h2>
                <p className="text-sm text-muted-foreground">
                  Les 5 utilisateurs avec le plus de listes
                </p>
              </div>
              <div className="rounded-2xl bg-card overflow-hidden">
                <table className="w-full text-sm text-left">
                  <thead className="bg-muted text-muted-foreground font-medium text-xs">
                    <tr>
                      <th className="px-5 py-2.5 font-semibold">Email</th>
                      <th className="px-5 py-2.5 font-semibold text-right">Listes</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    {stats.topUsersByLists.map((user, idx) => (
                      <tr key={idx} className="hover:bg-muted/30 transition-colors">
                        <td className="px-5 py-3 text-foreground truncate max-w-[200px]">{user.email}</td>
                        <td className="px-5 py-3 text-right font-medium text-foreground">{user.count}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </BlurFade>
        )}

        {stats.topUsersByItems.length > 0 && (
          <BlurFade delay={0.6}>
            <section>
              <div className="mb-4">
                <h2 className="text-lg font-semibold tracking-tight text-foreground">
                  Top utilisateurs par articles
                </h2>
                <p className="text-sm text-muted-foreground">
                  Les 5 utilisateurs avec le plus d'articles
                </p>
              </div>
              <div className="rounded-2xl bg-card overflow-hidden">
                <table className="w-full text-sm text-left">
                  <thead className="bg-muted text-muted-foreground font-medium text-xs">
                    <tr>
                      <th className="px-5 py-2.5 font-semibold">Email</th>
                      <th className="px-5 py-2.5 font-semibold text-right">Articles</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    {stats.topUsersByItems.map((user, idx) => (
                      <tr key={idx} className="hover:bg-muted/30 transition-colors">
                        <td className="px-5 py-3 text-foreground truncate max-w-[200px]">{user.email}</td>
                        <td className="px-5 py-3 text-right font-medium text-foreground">{user.count}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </BlurFade>
        )}
      </div>
    </div>
  );
}
