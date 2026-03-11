import { useState } from 'react';
import { useParams, useLocation, Link } from 'react-router-dom';
import { Layers, Package } from 'lucide-react';
import { motion } from 'motion/react';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../services/authApi';
import { queryKeys } from '../../lib/queryKeys';
import { ItemStatus, STATUS_OPTIONS, STATUS_LABELS, formatStatus } from '../../types/item';
import { Button } from '@/components/ui/button';
import { Pagination } from '@/components/Pagination';
import { Badge } from '@/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { BlurFade } from '@/components/effects/blur-fade';
import { Breadcrumb } from '../../components/Breadcrumb';
import { SkeletonText } from '../../components/Skeleton';
import { cn } from '@/lib/utils';
import { sanitizeImageUrl } from '../../utils/imageUtils';

const ITEMS_PER_PAGE = 20;

const statusToBadgeVariant: Record<ItemStatus, 'success' | 'warning' | 'error' | 'default'> = {
  AVAILABLE: 'success',
  TO_VERIFY: 'warning',
  NEEDS_MAINTENANCE: 'default',
  DAMAGED: 'error',
};

export function AdminListDetailPage() {
  const { id } = useParams();
  const location = useLocation();
  const ownerEmailFromState = (location.state as { ownerEmail?: string })?.ownerEmail;

  const [statusFilter, setStatusFilter] = useState<ItemStatus | ''>('');
  const [itemPage, setItemPage] = useState(0);

  const { data: list, isLoading: listLoading, error: listError } = useQuery({
    queryKey: queryKeys.admin.listDetail(id!),
    queryFn: () => adminApi.getListDetail(id!),
    enabled: !!id,
  });

  const itemParams = {
    itemListId: id!,
    ...(statusFilter && { status: statusFilter }),
    page: itemPage,
    size: ITEMS_PER_PAGE,
  };

  const { data: itemsData, isLoading: itemsLoading } = useQuery({
    queryKey: queryKeys.admin.items(itemParams),
    queryFn: ({ signal }) => adminApi.getItems(itemParams, signal),
    enabled: !!id,
  });

  const items = itemsData?.content ?? [];
  const itemTotalPages = itemsData?.totalPages ?? 0;
  const itemTotalElements = itemsData?.totalElements ?? 0;

  const ownerEmail = ownerEmailFromState || items[0]?.ownerEmail;

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value as ItemStatus | '');
    setItemPage(0);
  };

  if (listLoading) {
    return (
      <div>
        <SkeletonText className="w-36 h-5 mb-8" />
        <div className="mb-8">
          <SkeletonText className="w-64 h-12 mb-2" />
          <SkeletonText className="w-96 h-4" />
        </div>
        <div className="flex gap-2 mb-6">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-9 w-24 bg-muted rounded-full animate-pulse" />
          ))}
        </div>
        <div className="rounded-xl border bg-card">
          <div className="border-b px-6 py-3 flex gap-4">
            <div className="h-4 w-32 bg-muted rounded animate-pulse" />
            <div className="h-4 w-20 bg-muted rounded animate-pulse" />
            <div className="h-4 w-16 bg-muted rounded animate-pulse" />
            <div className="h-4 w-20 bg-muted rounded animate-pulse" />
          </div>
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-6 py-5 border-b last:border-0">
              <div className="h-4 w-40 bg-muted rounded animate-pulse" />
              <div className="h-6 w-20 bg-muted rounded-full animate-pulse ml-4" />
              <div className="h-4 w-10 bg-muted rounded animate-pulse ml-auto" />
              <div className="h-4 w-24 bg-muted rounded animate-pulse ml-4" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (listError || !list) {
    return (
      <div className="text-center py-24 animate-fade-in">
        <div className="w-16 h-16 bg-muted rounded-2xl flex items-center justify-center mx-auto mb-4">
          <Layers className="h-8 w-8 text-muted-foreground" />
        </div>
        <h2 className="font-display text-xl font-semibold mb-2">Liste introuvable</h2>
        <p className="text-muted-foreground mb-6">Cette liste n'existe pas ou a été supprimée.</p>
        <Button variant="outline" asChild>
          <Link to="/admin/lists">Retour au contenu</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      <Breadcrumb items={[{ label: 'Contenu', href: '/admin/lists' }, { label: list.name }]} />

      {/* Header */}
      <div className="mb-8">
        <BlurFade>
          <div className="flex items-center gap-3 mb-2">
            <h1 className="font-display text-4xl lg:text-5xl font-semibold tracking-tight">
              {list.name}
            </h1>
            {list.category && (
              <Badge variant="secondary" className="mt-1">{list.category}</Badge>
            )}
          </div>
        </BlurFade>
        {list.description && (
          <BlurFade delay={0.1}>
            <p className="text-muted-foreground text-lg max-w-2xl">{list.description}</p>
          </BlurFade>
        )}
        {ownerEmail && (
          <BlurFade delay={0.15}>
            <p className="text-sm text-muted-foreground mt-2">
              Propriétaire : <span className="font-medium text-foreground">{ownerEmail}</span>
            </p>
          </BlurFade>
        )}
      </div>

      {/* Status filter pills */}
      <div className="flex items-center justify-between mb-6 gap-4">
        <div className="flex gap-2 flex-wrap" role="tablist" aria-label="Filtrer par statut">
          <button
            onClick={() => handleStatusFilterChange('')}
            role="tab"
            aria-selected={statusFilter === ''}
            className={cn(
              'px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-200 border',
              statusFilter === ''
                ? 'bg-foreground text-background border-foreground'
                : 'bg-background text-muted-foreground border-border hover:border-foreground/30',
            )}
          >
            Tous
          </button>
          {STATUS_OPTIONS.map((status) => (
            <button
              key={status}
              onClick={() => handleStatusFilterChange(status)}
              role="tab"
              aria-selected={statusFilter === status}
              className={cn(
                'px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-200 border',
                statusFilter === status
                  ? 'bg-foreground text-background border-foreground'
                  : 'bg-background text-muted-foreground border-border hover:border-foreground/30',
              )}
            >
              {STATUS_LABELS[status]}
            </button>
          ))}
        </div>
        <p className="text-sm text-muted-foreground shrink-0">
          {itemsLoading ? '...' : `${itemTotalElements} article${itemTotalElements !== 1 ? 's' : ''}`}
        </p>
      </div>

      {/* Items table */}
      {itemsLoading ? (
        <div className="rounded-xl border bg-card">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="flex items-center gap-4 px-6 py-5 border-b last:border-0">
              <div className="h-4 w-40 bg-muted rounded animate-pulse" />
              <div className="h-6 w-20 bg-muted rounded-full animate-pulse ml-4" />
              <div className="h-4 w-10 bg-muted rounded animate-pulse ml-auto" />
              <div className="h-4 w-24 bg-muted rounded animate-pulse ml-4" />
            </div>
          ))}
        </div>
      ) : (
        <>
          <div className="rounded-xl border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nom</TableHead>
                  <TableHead>Statut</TableHead>
                  <TableHead className="text-right">Stock</TableHead>
                  <TableHead>Créé le</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((item, index) => {
                  const imageUrl = sanitizeImageUrl(item.imageUrl);
                  return (
                  <motion.tr
                    key={item.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05, duration: 0.3 }}
                    className="border-b transition-colors hover:bg-muted/50"
                  >
                    <TableCell className="py-5">
                      <div className="flex items-center gap-3">
                        {imageUrl ? (
                          <div className="h-10 w-10 rounded-lg bg-muted overflow-hidden flex-shrink-0">
                            <img
                              src={imageUrl}
                              alt={item.name}
                              className="h-full w-full object-cover"
                              loading="lazy"
                              onError={(e) => { e.currentTarget.style.display = 'none'; }}
                            />
                          </div>
                        ) : (
                          <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center flex-shrink-0">
                            <Package className="h-4 w-4 text-muted-foreground/40" />
                          </div>
                        )}
                        <span className="font-medium">{item.name}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusToBadgeVariant[item.status]}>
                        {formatStatus(item.status)}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{item.stock}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {new Date(item.createdAt).toLocaleDateString('fr-FR')}
                    </TableCell>
                  </motion.tr>
                  );
                })}
              </TableBody>
            </Table>

            {items.length === 0 && (
              <div className="text-center py-12 text-muted-foreground">
                <Package className="h-10 w-10 mx-auto mb-3 text-muted-foreground/40" />
                <p>{statusFilter ? 'Aucun article avec ce statut' : 'Aucun article dans cette liste'}</p>
              </div>
            )}
          </div>

          <Pagination page={itemPage} totalPages={itemTotalPages} onPageChange={setItemPage} />
        </>
      )}
    </div>
  );
}
