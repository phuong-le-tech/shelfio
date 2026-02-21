import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Pencil, Trash2, List, ChevronLeft, ChevronRight, FolderOpen } from 'lucide-react';
import { listsApi } from '../services/api';
import { ItemList, ItemListSearchParams } from '../types/item';
import { SkeletonCard, SkeletonText, Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import ConfirmModal from '../components/ConfirmModal';

export default function ListsPage() {
  const [lists, setLists] = useState<ItemList[]>([]);
  const [loading, setLoading] = useState(true);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const { showToast } = useToast();

  const loadLists = useCallback(async () => {
    setLoading(true);
    try {
      const params: ItemListSearchParams = {
        page,
        size: 9,
        sortBy: 'createdAt',
        sortDir: 'desc',
      };

      const response = await listsApi.getAll(params);
      setLists(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (error) {
      console.error('Failed to load lists:', error);
      showToast('Échec du chargement des listes', 'error');
    } finally {
      setLoading(false);
    }
  }, [page, showToast]);

  useEffect(() => {
    loadLists();
  }, [loadLists]);

  const handleDeleteConfirm = async () => {
    if (!pendingDeleteId) return;
    const id = pendingDeleteId;
    setPendingDeleteId(null);
    setDeletingId(id);
    try {
      await listsApi.delete(id);
      showToast('Liste supprimée avec succès', 'success');
      loadLists();
    } catch (error) {
      console.error('Failed to delete list:', error);
      showToast('Échec de la suppression de la liste', 'error');
    } finally {
      setDeletingId(null);
    }
  };

  if (loading && lists.length === 0) {
    return (
      <div>
        <div className="flex justify-between items-center mb-6">
          <div className="space-y-2">
            <SkeletonText className="w-28 h-9" />
            <SkeletonText className="w-20" />
          </div>
          <Skeleton className="h-12 w-40 rounded-xl" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="font-display text-3xl text-stone-100 tracking-tight">Mes Listes</h1>
          <p className="text-stone-500 mt-1">{totalElements} listes au total</p>
        </div>
        <Link
          to="/lists/new"
          className="inline-flex items-center px-6 py-3 bg-gradient-to-r from-amber-500 to-amber-600 text-surface-base font-semibold rounded-xl shadow-glow-amber transition-all duration-200 hover:from-amber-400 hover:to-amber-500 hover:-translate-y-0.5 active:translate-y-0"
        >
          <Plus className="h-5 w-5 mr-2" />
          Nouvelle liste
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {lists.map((list, index) => (
          <div
            key={list.id}
            className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 backdrop-blur-xl rounded-2xl border border-white/[0.06] shadow-premium overflow-hidden transition-all duration-300 hover:shadow-premium-hover hover:border-white/[0.1] hover:-translate-y-1 group animate-fade-in-up"
            style={{ animationDelay: `${index * 60}ms`, animationFillMode: 'backwards' }}
          >
            <Link to={`/lists/${list.id}`} className="block p-6">
              <div className="flex items-start justify-between mb-3">
                <div className="w-12 h-12 bg-amber-500/10 rounded-xl flex items-center justify-center group-hover:scale-110 transition-transform duration-300">
                  <List className="h-6 w-6 text-amber-400" />
                </div>
                {list.category && (
                  <span className="px-2.5 py-1 bg-stone-500/10 text-stone-400 rounded-md text-xs font-medium">
                    {list.category}
                  </span>
                )}
              </div>
              <h3 className="font-semibold text-lg text-stone-100 tracking-tight mb-1 group-hover:text-amber-400 transition-colors">
                {list.name}
              </h3>
              {list.description && (
                <p className="text-stone-500 text-sm line-clamp-2 mb-3">{list.description}</p>
              )}
              <p className="text-stone-400 text-sm">
                <span className="font-medium">{list.itemCount || 0}</span> articles
              </p>
            </Link>
            <div className="px-6 pb-6">
              <div className="flex gap-2">
                <Link
                  to={`/lists/${list.id}/edit`}
                  className="flex-1 inline-flex items-center justify-center px-3 py-2.5 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] hover:text-stone-100"
                >
                  <Pencil className="h-4 w-4 mr-1.5" />
                  Modifier
                </Link>
                <button
                  onClick={(e) => {
                    e.preventDefault();
                    setPendingDeleteId(list.id);
                  }}
                  disabled={deletingId === list.id}
                  aria-label={`Supprimer ${list.name}`}
                  className="inline-flex items-center justify-center px-3 py-2.5 border border-red-400/30 text-red-400 rounded-xl transition-all duration-200 hover:bg-red-400/10 hover:border-red-400/40 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Trash2 className={`h-4 w-4 ${deletingId === list.id ? 'animate-pulse' : ''}`} />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {lists.length === 0 && !loading && (
        <div className="text-center py-16 text-stone-500 animate-fade-in">
          <FolderOpen className="h-16 w-16 mx-auto mb-4 opacity-30" />
          <p className="text-lg">Aucune liste trouvée.</p>
          <Link to="/lists/new" className="text-amber-400 hover:text-amber-300 transition-colors mt-2 inline-block">
            Créer votre première liste
          </Link>
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-4 mt-8">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="inline-flex items-center px-4 py-2 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ChevronLeft className="h-4 w-4 mr-1" />
            Précédent
          </button>
          <span className="text-stone-400">
            Page {page + 1} sur {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="inline-flex items-center px-4 py-2 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Suivant
            <ChevronRight className="h-4 w-4 ml-1" />
          </button>
        </div>
      )}

      <ConfirmModal
        isOpen={pendingDeleteId !== null}
        title="Supprimer la liste"
        message="Êtes-vous sûr de vouloir supprimer cette liste et tous ses articles ? Cette action est irréversible."
        confirmLabel="Supprimer"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setPendingDeleteId(null)}
      />
    </div>
  );
}
