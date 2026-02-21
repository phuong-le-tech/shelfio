import { Link } from 'react-router-dom';
import { Package, Clock, CheckCircle, Search, Archive, Hourglass, AlertCircle, RefreshCw } from 'lucide-react';
import { useDashboardStats } from '../hooks/useDashboardStats';
import { SkeletonStatCard, SkeletonText, Skeleton } from '../components/Skeleton';

export default function Dashboard() {
  const { stats, loading, error, reload } = useDashboardStats();

  if (loading) {
    return (
      <div>
        <div className="mb-8">
          <SkeletonText className="w-32 h-9 mb-2" />
          <SkeletonText className="w-48" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-6 mb-8">
          {[...Array(6)].map((_, i) => (
            <SkeletonStatCard key={i} />
          ))}
        </div>
        <div className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 rounded-2xl border border-white/[0.06] shadow-premium p-6">
          <SkeletonText className="w-40 h-5 mb-4" />
          <div className="space-y-3">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="flex justify-between">
                <SkeletonText className="w-24" />
                <Skeleton className="h-6 w-8 rounded-md" />
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error && !stats) {
    return (
      <div className="flex flex-col items-center justify-center py-24 text-center animate-fade-in">
        <div className="w-16 h-16 bg-red-400/10 rounded-2xl flex items-center justify-center mb-4">
          <AlertCircle className="h-8 w-8 text-red-400" />
        </div>
        <h2 className="font-display text-xl text-stone-100 mb-2">Impossible de charger les statistiques</h2>
        <p className="text-stone-500 mb-6 max-w-sm">
          Une erreur est survenue lors du chargement du tableau de bord. Vérifiez votre connexion et réessayez.
        </p>
        <button
          onClick={reload}
          className="inline-flex items-center px-5 py-2.5 bg-gradient-to-r from-amber-500 to-amber-600 text-surface-base font-semibold rounded-xl shadow-glow-amber transition-all duration-200 hover:from-amber-400 hover:to-amber-500 hover:-translate-y-0.5 active:translate-y-0"
        >
          <RefreshCw className="h-4 w-4 mr-2" />
          Réessayer
        </button>
      </div>
    );
  }

  const statusCards = [
    { label: 'Total articles', value: stats?.totalItems || 0, icon: Package, color: 'bg-gradient-to-br from-amber-500 to-amber-600', glow: 'shadow-glow-amber' },
    { label: 'À préparer', value: stats?.countByStatus?.['TO_PREPARE'] || 0, icon: Clock, color: 'bg-gradient-to-br from-amber-400 to-amber-500', glow: 'shadow-glow-amber' },
    { label: 'À vérifier', value: stats?.countByStatus?.['TO_VERIFY'] || 0, icon: Search, color: 'bg-gradient-to-br from-blue-400 to-blue-500', glow: 'shadow-glow-blue' },
    { label: 'En attente', value: stats?.countByStatus?.['PENDING'] || 0, icon: Hourglass, color: 'bg-gradient-to-br from-purple-400 to-purple-500', glow: '' },
    { label: 'Prêt', value: stats?.countByStatus?.['READY'] || 0, icon: CheckCircle, color: 'bg-gradient-to-br from-lime-500 to-lime-600', glow: 'shadow-glow-lime' },
    { label: 'Archivé', value: stats?.countByStatus?.['ARCHIVED'] || 0, icon: Archive, color: 'bg-gradient-to-br from-stone-400 to-stone-500', glow: '' },
  ];

  return (
    <div>
      <div className="mb-8">
        <h1 className="font-display text-3xl text-stone-100 tracking-tight">Tableau de bord</h1>
        <p className="text-stone-400 mt-1">Aperçu de votre inventaire</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-6 mb-8">
        {statusCards.map((card, index) => (
          <div
            key={card.label}
            className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 backdrop-blur-xl rounded-2xl border border-white/[0.06] shadow-premium p-6 transition-all duration-300 hover:shadow-premium-hover hover:border-white/[0.1] hover:-translate-y-1 group animate-fade-in-up"
            style={{ animationDelay: `${index * 75}ms`, animationFillMode: 'backwards' }}
          >
            <div className="flex items-center">
              <div className={`${card.color} ${card.glow} p-3 rounded-xl transition-transform duration-200 group-hover:scale-110`}>
                <card.icon className="h-6 w-6 text-surface-base" />
              </div>
              <div className="ml-4">
                <p className="text-xs font-medium text-stone-500 uppercase tracking-wider">{card.label}</p>
                <p className="text-2xl font-semibold text-stone-100 tracking-tight">{card.value}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {stats && Object.keys(stats.countByCategory).length > 0 && (
        <div className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 backdrop-blur-xl rounded-2xl border border-white/[0.06] shadow-premium p-6 animate-fade-in">
          <h2 className="text-lg font-semibold text-stone-100 mb-4 tracking-tight">Articles par catégorie</h2>
          <div className="space-y-2">
            {Object.entries(stats.countByCategory).map(([category, count]) => (
              <div key={category} className="flex items-center justify-between py-2.5 px-3 rounded-xl hover:bg-white/[0.03] transition-colors duration-200">
                <span className="text-stone-300">{category}</span>
                <span className="bg-amber-500/10 text-amber-400 border border-amber-500/20 px-3 py-1 rounded-md text-xs font-medium uppercase tracking-wider">
                  {count}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="mt-8">
        <Link
          to="/lists"
          className="inline-flex items-center px-6 py-3 bg-gradient-to-r from-amber-500 to-amber-600 text-surface-base font-semibold rounded-xl shadow-glow-amber transition-all duration-200 hover:from-amber-400 hover:to-amber-500 hover:-translate-y-0.5 active:translate-y-0"
        >
          Voir mes listes
        </Link>
      </div>
    </div>
  );
}
