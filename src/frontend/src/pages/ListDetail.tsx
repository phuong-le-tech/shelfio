import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Plus, Pencil, Trash2, Package, List, ChevronLeft, ChevronRight } from 'lucide-react';
import axios from 'axios';
import { listsApi, itemsApi } from '../services/api';
import { ItemList, Item, formatStatus, STATUS_OPTIONS, STATUS_LABELS, getItemImageUrl, formatCustomFieldValue } from '../types/item';
import { SkeletonCard, SkeletonText, Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import ConfirmModal from '../components/ConfirmModal';

const ITEMS_PER_PAGE = 12;

export default function ListDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();

  // List metadata
  const [list, setList] = useState<ItemList | null>(null);
  const [listLoading, setListLoading] = useState(true);

  // Items (server-side filtered + paginated)
  const [items, setItems] = useState<Item[]>([]);
  const [itemsLoading, setItemsLoading] = useState(true);
  const [itemPage, setItemPage] = useState(0);
  const [itemTotalPages, setItemTotalPages] = useState(0);
  const [itemTotalElements, setItemTotalElements] = useState(0);
  const [itemsReloadKey, setItemsReloadKey] = useState(0);

  const [deletingItemId, setDeletingItemId] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('');

  // Effect 1: load list metadata once on mount
  useEffect(() => {
    if (!id) return;
    const controller = new AbortController();

    const load = async () => {
      setListLoading(true);
      try {
        const listData = await listsApi.getById(id, controller.signal);
        if (!controller.signal.aborted) {
          setList(listData);
        }
      } catch (err) {
        if (!axios.isCancel(err) && !controller.signal.aborted) {
          console.error('Failed to load list:', err);
          showToast('Échec du chargement de la liste', 'error');
          navigate('/lists');
        }
      } finally {
        if (!controller.signal.aborted) {
          setListLoading(false);
        }
      }
    };

    load();
    return () => controller.abort();
  }, [id, navigate, showToast]);

  // Effect 2: load items — reruns on filter, page, or explicit reload
  useEffect(() => {
    if (!id) return;
    const controller = new AbortController();

    const load = async () => {
      setItemsLoading(true);
      try {
        const response = await itemsApi.getAll(
          {
            itemListId: id,
            status: statusFilter || undefined,
            page: itemPage,
            size: ITEMS_PER_PAGE,
            sortBy: 'createdAt',
            sortDir: 'desc',
          },
          controller.signal
        );
        if (!controller.signal.aborted) {
          setItems(response.content);
          setItemTotalPages(response.totalPages);
          setItemTotalElements(response.totalElements);
        }
      } catch (err) {
        if (!axios.isCancel(err) && !controller.signal.aborted) {
          console.error('Failed to load items:', err);
          showToast('Échec du chargement des articles', 'error');
        }
      } finally {
        if (!controller.signal.aborted) {
          setItemsLoading(false);
        }
      }
    };

    load();
    return () => controller.abort();
  }, [id, statusFilter, itemPage, itemsReloadKey, showToast]);

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value);
    setItemPage(0);
  };

  const handleDeleteConfirm = async () => {
    if (!pendingDeleteId) return;
    const itemId = pendingDeleteId;
    setPendingDeleteId(null);
    setDeletingItemId(itemId);
    try {
      await itemsApi.delete(itemId);
      showToast('Article supprimé avec succès', 'success');
      setItemsReloadKey(k => k + 1);
    } catch (error) {
      console.error('Failed to delete item:', error);
      showToast("Échec de la suppression de l'article", 'error');
    } finally {
      setDeletingItemId(null);
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'TO_PREPARE':
        return 'bg-amber-500/10 text-amber-400 border border-amber-500/20';
      case 'TO_VERIFY':
        return 'bg-blue-400/10 text-blue-400 border border-blue-400/20';
      case 'PENDING':
        return 'bg-purple-400/10 text-purple-400 border border-purple-400/20';
      case 'READY':
        return 'bg-lime-500/10 text-lime-400 border border-lime-500/20';
      case 'ARCHIVED':
        return 'bg-stone-500/10 text-stone-400 border border-stone-500/20';
      default:
        return 'bg-stone-500/10 text-stone-400 border border-stone-500/20';
    }
  };

  if (listLoading) {
    return (
      <div>
        <SkeletonText className="w-36 h-5 mb-6" />
        <div className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 rounded-2xl border border-white/[0.06] shadow-premium p-6 mb-6">
          <div className="flex items-start justify-between">
            <div>
              <SkeletonText className="w-48 h-8 mb-2" />
              <SkeletonText className="w-64 h-4" />
            </div>
            <div className="flex gap-2">
              <Skeleton className="h-10 w-24 rounded-xl" />
              <Skeleton className="h-10 w-32 rounded-xl" />
            </div>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(3)].map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      </div>
    );
  }

  if (!list) {
    return (
      <div className="text-center py-24 animate-fade-in">
        <div className="w-16 h-16 bg-stone-500/10 rounded-2xl flex items-center justify-center mx-auto mb-4">
          <List className="h-8 w-8 text-stone-500" />
        </div>
        <h2 className="font-display text-xl text-stone-100 mb-2">Liste introuvable</h2>
        <p className="text-stone-500 mb-6">Cette liste n'existe pas ou a été supprimée.</p>
        <Link
          to="/lists"
          className="inline-flex items-center text-amber-400 hover:text-amber-300 transition-colors"
        >
          <ArrowLeft className="h-4 w-4 mr-1.5" />
          Retour aux listes
        </Link>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <button
        onClick={() => navigate('/lists')}
        className="inline-flex items-center text-stone-400 hover:text-stone-200 mb-6 transition-all duration-200 hover:-translate-x-0.5 group"
      >
        <ArrowLeft className="h-5 w-5 mr-1.5 transition-transform group-hover:-translate-x-0.5" />
        Retour aux listes
      </button>

      <div className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 backdrop-blur-xl rounded-2xl border border-white/[0.06] shadow-premium p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 bg-amber-500/10 rounded-xl flex items-center justify-center flex-shrink-0">
              <List className="h-7 w-7 text-amber-400" />
            </div>
            <div>
              <div className="flex items-center gap-3 mb-1">
                <h1 className="font-display text-2xl text-stone-100 tracking-tight">{list.name}</h1>
                {list.category && (
                  <span className="px-2.5 py-1 bg-stone-500/10 text-stone-400 rounded-md text-xs font-medium">
                    {list.category}
                  </span>
                )}
              </div>
              {list.description && (
                <p className="text-stone-500">{list.description}</p>
              )}
            </div>
          </div>
          <div className="flex gap-2 flex-shrink-0">
            <Link
              to={`/lists/${id}/edit`}
              className="inline-flex items-center px-4 py-2.5 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] hover:text-stone-100"
            >
              <Pencil className="h-4 w-4 mr-1.5" />
              Modifier
            </Link>
            <Link
              to={`/lists/${id}/items/new`}
              className="inline-flex items-center px-4 py-2.5 bg-gradient-to-r from-amber-500 to-amber-600 text-surface-base font-semibold rounded-xl shadow-glow-amber transition-all duration-200 hover:from-amber-400 hover:to-amber-500 hover:-translate-y-0.5 active:translate-y-0"
            >
              <Plus className="h-4 w-4 mr-1.5" />
              Ajouter article
            </Link>
          </div>
        </div>
      </div>

      <div className="flex justify-between items-center mb-4">
        <p className="text-stone-400">
          {itemsLoading ? '…' : `${itemTotalElements} article${itemTotalElements !== 1 ? 's' : ''}`}
        </p>
        <select
          value={statusFilter}
          onChange={(e) => handleStatusFilterChange(e.target.value)}
          className="px-4 py-2 bg-surface-base/50 border border-white/[0.08] rounded-xl text-stone-100 cursor-pointer transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-amber-500/30 focus:border-amber-500/40 hover:border-white/[0.12]"
        >
          <option value="">Tous les statuts</option>
          {STATUS_OPTIONS.map((status) => (
            <option key={status} value={status}>{STATUS_LABELS[status]}</option>
          ))}
        </select>
      </div>

      {itemsLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(3)].map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {items.map((item, index) => (
              <div
                key={item.id}
                className="bg-gradient-to-br from-surface-elevated/80 to-surface-card/80 backdrop-blur-xl rounded-2xl border border-white/[0.06] shadow-premium overflow-hidden transition-all duration-300 hover:shadow-premium-hover hover:border-white/[0.1] hover:-translate-y-1 group animate-fade-in-up"
                style={{ animationDelay: `${index * 60}ms`, animationFillMode: 'backwards' }}
              >
                <div className="h-48 bg-surface-base/50 flex items-center justify-center overflow-hidden relative">
                  {item.hasImage ? (
                    <>
                      <img
                        src={getItemImageUrl(item.id)}
                        alt={item.name}
                        className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-surface-base/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                    </>
                  ) : (
                    <div className="text-stone-600 flex flex-col items-center">
                      <Package className="h-12 w-12 mb-2 opacity-40" />
                      <span className="text-sm">Pas d'image</span>
                    </div>
                  )}
                </div>
                <div className="p-4">
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-semibold text-lg text-stone-100 tracking-tight">{item.name}</h3>
                    <span className={`px-2.5 py-1 rounded-md text-[11px] font-medium uppercase tracking-wider ${getStatusBadgeClass(item.status)}`}>
                      {formatStatus(item.status)}
                    </span>
                  </div>
                  <p className="text-stone-400 text-sm mb-2">
                    <span className="font-medium">Stock:</span> {item.stock}
                  </p>
                  {list.customFieldDefinitions && list.customFieldDefinitions.length > 0 && (
                    <div className="space-y-1 mb-3">
                      {[...list.customFieldDefinitions]
                        .sort((a, b) => a.displayOrder - b.displayOrder)
                        .map((def) => {
                          const value = item.customFieldValues?.[def.name];
                          if (value === undefined || value === null || value === '') return null;
                          return (
                            <p key={def.name} className="text-stone-400 text-sm">
                              <span className="font-medium">{def.label}:</span>{' '}
                              {formatCustomFieldValue(def.type, value)}
                            </p>
                          );
                        })}
                    </div>
                  )}
                  <div className="flex gap-2">
                    <Link
                      to={`/lists/${id}/items/${item.id}/edit`}
                      className="flex-1 inline-flex items-center justify-center px-3 py-2.5 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] hover:text-stone-100"
                    >
                      <Pencil className="h-4 w-4 mr-1.5" />
                      Modifier
                    </Link>
                    <button
                      onClick={() => setPendingDeleteId(item.id)}
                      disabled={deletingItemId === item.id}
                      aria-label={`Supprimer ${item.name}`}
                      className="inline-flex items-center justify-center px-3 py-2.5 border border-red-400/30 text-red-400 rounded-xl transition-all duration-200 hover:bg-red-400/10 hover:border-red-400/40 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Trash2 className={`h-4 w-4 ${deletingItemId === item.id ? 'animate-pulse' : ''}`} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {items.length === 0 && (
            <div className="text-center py-16 text-stone-500 animate-fade-in">
              <Package className="h-16 w-16 mx-auto mb-4 opacity-30" />
              <p className="text-lg">Aucun article dans cette liste.</p>
              <Link to={`/lists/${id}/items/new`} className="text-amber-400 hover:text-amber-300 transition-colors mt-2 inline-block">
                Ajouter votre premier article
              </Link>
            </div>
          )}

          {itemTotalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mt-8">
              <button
                onClick={() => setItemPage(p => Math.max(0, p - 1))}
                disabled={itemPage === 0}
                className="inline-flex items-center px-4 py-2 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Précédent
              </button>
              <span className="text-stone-400">
                Page {itemPage + 1} sur {itemTotalPages}
              </span>
              <button
                onClick={() => setItemPage(p => Math.min(itemTotalPages - 1, p + 1))}
                disabled={itemPage >= itemTotalPages - 1}
                className="inline-flex items-center px-4 py-2 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Suivant
                <ChevronRight className="h-4 w-4 ml-1" />
              </button>
            </div>
          )}
        </>
      )}

      <ConfirmModal
        isOpen={pendingDeleteId !== null}
        title="Supprimer l'article"
        message="Êtes-vous sûr de vouloir supprimer cet article ? Cette action est irréversible."
        confirmLabel="Supprimer"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setPendingDeleteId(null)}
      />
    </div>
  );
}
