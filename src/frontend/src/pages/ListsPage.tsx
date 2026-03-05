import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Pencil, Trash2, ChevronLeft, ChevronRight, FolderOpen } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { listsApi } from '../services/api';
import { ItemList, ItemListSearchParams } from '../types/item';
import { SkeletonCard, SkeletonText, Skeleton } from '../components/Skeleton';
import { useToast } from '../components/Toast';
import ConfirmModal from '../components/ConfirmModal';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { BlurFade } from '@/components/effects/blur-fade';
import { SpotlightCard } from '@/components/effects/spotlight-card';
import { StaggeredList, StaggeredItem } from '@/components/effects/staggered-list';

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
    } catch {
      showToast('Echec du chargement des listes', 'error');
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
      showToast('Liste supprimee avec succes', 'success');
      loadLists();
    } catch {
      showToast('Echec de la suppression de la liste', 'error');
    } finally {
      setDeletingId(null);
    }
  };

  if (loading && lists.length === 0) {
    return (
      <div>
        <div className="flex justify-between items-center mb-8">
          <div className="space-y-2">
            <SkeletonText className="w-32 h-10" />
            <SkeletonText className="w-24 h-4" />
          </div>
          <Skeleton className="h-10 w-36 rounded-lg" />
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
      <div className="flex justify-between items-start mb-8">
        <div>
          <BlurFade delay={0.1}>
            <h1 className="font-display text-4xl font-semibold tracking-tight">Mes Listes</h1>
          </BlurFade>
          <BlurFade delay={0.2}>
            <p className="text-muted-foreground mt-1">{totalElements} listes au total</p>
          </BlurFade>
        </div>
        <BlurFade delay={0.2}>
          <Button asChild>
            <Link to="/lists/new">
              <Plus className="h-4 w-4 mr-2" />
              Nouvelle liste
            </Link>
          </Button>
        </BlurFade>
      </div>

      <StaggeredList className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <AnimatePresence>
          {lists.map((list, index) => (
            <StaggeredItem key={list.id} className={index % 4 === 3 ? 'lg:col-span-2' : ''}>
              <SpotlightCard className="group rounded-2xl border bg-card shadow-card transition-all duration-300 hover:shadow-elevated overflow-hidden">
                <Link to={`/lists/${list.id}`} className="block p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="w-12 h-12 rounded-xl bg-peach-light flex items-center justify-center">
                      <span className="font-display text-xl font-bold text-foreground">
                        {list.name[0]?.toUpperCase()}
                      </span>
                    </div>
                    {list.category && (
                      <Badge variant="secondary">{list.category}</Badge>
                    )}
                  </div>
                  <h3 className="font-display text-lg font-semibold tracking-tight mb-1 group-hover:text-peach-dark transition-colors">
                    {list.name}
                  </h3>
                  {list.description && (
                    <p className="text-muted-foreground text-sm line-clamp-2 mb-3">{list.description}</p>
                  )}
                  <p className="text-muted-foreground text-sm">
                    <span className="font-semibold text-foreground">{list.itemCount || 0}</span> articles
                  </p>
                </Link>

                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  className="px-6 pb-6"
                >
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" className="w-full flex-1" asChild>
                      <Link to={`/lists/${list.id}/edit`}>
                        <Pencil className="h-3.5 w-3.5 mr-1.5" />
                        Modifier
                      </Link>
                    </Button>
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-9 w-9 text-muted-foreground hover:text-destructive hover:border-destructive"
                      onClick={(e) => {
                        e.preventDefault();
                        setPendingDeleteId(list.id);
                      }}
                      disabled={deletingId === list.id}
                      aria-label={`Supprimer ${list.name}`}
                    >
                      <Trash2 className={`h-3.5 w-3.5 ${deletingId === list.id ? 'animate-pulse' : ''}`} />
                    </Button>
                  </div>
                </motion.div>
              </SpotlightCard>
            </StaggeredItem>
          ))}
        </AnimatePresence>
      </StaggeredList>

      {lists.length === 0 && !loading && (
        <div className="text-center py-24 animate-fade-in relative">
          <div className="text-[12rem] font-display font-bold text-muted/30 leading-none select-none">0</div>
          <div className="-mt-16 relative z-10">
            <FolderOpen className="h-12 w-12 mx-auto mb-3 text-muted-foreground/50" />
            <p className="text-lg text-muted-foreground mb-4">Aucune liste trouvee.</p>
            <Button asChild>
              <Link to="/lists/new">Creer votre premiere liste</Link>
            </Button>
          </div>
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-4 mt-8">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            <ChevronLeft className="h-4 w-4 mr-1" />
            Precedent
          </Button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} sur {totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
          >
            Suivant
            <ChevronRight className="h-4 w-4 ml-1" />
          </Button>
        </div>
      )}

      <ConfirmModal
        isOpen={pendingDeleteId !== null}
        title="Supprimer la liste"
        message="Etes-vous sur de vouloir supprimer cette liste et tous ses articles ? Cette action est irreversible."
        confirmLabel="Supprimer"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setPendingDeleteId(null)}
      />
    </div>
  );
}
