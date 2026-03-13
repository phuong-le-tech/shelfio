import { useState, useEffect } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import {
  ArrowLeft,
  Plus,
  Pencil,
  Trash2,
  Package,
  List,
  MoreHorizontal,
} from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { listsApi, itemsApi } from "../services/api";
import {
  ItemStatus,
  formatStatus,
  STATUS_OPTIONS,
  STATUS_LABELS,
  STATUS_BADGE_VARIANTS,
  formatCustomFieldValue,
} from "../types/item";
import { SkeletonCard, SkeletonText } from "../components/Skeleton";
import { useToast } from "../components/Toast";
import ConfirmModal from "../components/ConfirmModal";
import { Button } from "@/components/ui/button";
import { Pagination } from "@/components/Pagination";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { BlurFade } from "@/components/effects/blur-fade";
import {
  StaggeredList,
  StaggeredItem,
} from "@/components/effects/staggered-list";
import { cn } from "@/lib/utils";
import { sanitizeImageUrl } from "../utils/imageUtils";
import { queryKeys } from "../lib/queryKeys";
import { Breadcrumb } from "../components/Breadcrumb";

const ITEMS_PER_PAGE = 12;

export default function ListDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const queryClient = useQueryClient();

  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<ItemStatus | "">("");
  const [itemPage, setItemPage] = useState(0);

  const { data: list, isLoading: listLoading, error: listError } = useQuery({
    queryKey: queryKeys.lists.detail(id!),
    queryFn: () => listsApi.getById(id!),
    enabled: !!id,
  });

  // Navigate away on list fetch error
  useEffect(() => {
    if (listError && !list) {
      showToast("Échec du chargement de la liste", "error");
      navigate("/lists");
    }
  }, [listError, list, showToast, navigate]);

  const itemParams = {
    itemListId: id!,
    status: statusFilter || undefined,
    page: itemPage,
    size: ITEMS_PER_PAGE,
    sortBy: "createdAt",
    sortDir: "desc" as const,
  };
  const { data: itemsData, isLoading: itemsLoading } = useQuery({
    queryKey: queryKeys.items.list(itemParams),
    queryFn: () => itemsApi.getAll(itemParams),
    enabled: !!id,
  });

  const items = itemsData?.content ?? [];
  const itemTotalPages = itemsData?.totalPages ?? 0;
  const itemTotalElements = itemsData?.totalElements ?? 0;

  const deleteMutation = useMutation({
    mutationFn: (itemId: string) => itemsApi.delete(itemId),
    onSuccess: () => {
      showToast("Article supprimé avec succès", "success");
      queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.lists.detail(id!) });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all });
    },
    onError: () => {
      showToast("Échec de la suppression de l'article", "error");
    },
  });

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value as ItemStatus | "");
    setItemPage(0);
  };

  const handleDeleteConfirm = () => {
    if (!pendingDeleteId) return;
    const itemId = pendingDeleteId;
    setPendingDeleteId(null);
    deleteMutation.mutate(itemId);
  };

  if (listLoading) {
    return (
      <div>
        <SkeletonText className="w-36 h-5 mb-8" />
        <div className="mb-8">
          <SkeletonText className="w-64 h-12 mb-2" />
          <SkeletonText className="w-96 h-4" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {[...Array(4)].map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      </div>
    );
  }

  if (!list) {
    return (
      <div className="text-center py-24 animate-fade-in">
        <div className="w-16 h-16 bg-muted rounded-2xl flex items-center justify-center mx-auto mb-4">
          <List className="h-8 w-8 text-muted-foreground" />
        </div>
        <h2 className="font-display text-xl font-semibold mb-2">
          Liste introuvable
        </h2>
        <p className="text-muted-foreground mb-6">
          Cette liste n'existe pas ou a été supprimée.
        </p>
        <Button variant="outline" asChild>
          <Link to="/lists">
            <ArrowLeft className="h-4 w-4 mr-1.5" />
            Retour aux listes
          </Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      <Breadcrumb items={[{ label: 'Mes Listes', href: '/lists' }, { label: list.name }]} />

      {/* Full-width header banner */}
      <div className="mb-8">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
          <div>
            <BlurFade>
              <div className="flex items-center gap-3 mb-2">
                <h1 className="font-display text-4xl lg:text-5xl font-semibold tracking-tight">
                  {list.name}
                </h1>
                {list.category && (
                  <Badge variant="secondary" className="mt-1">
                    {list.category}
                  </Badge>
                )}
              </div>
            </BlurFade>
            {list.description && (
              <BlurFade delay={0.1}>
                <p className="text-muted-foreground text-lg max-w-2xl">
                  {list.description}
                </p>
              </BlurFade>
            )}
          </div>
          <div className="flex gap-2 flex-shrink-0">
            <Button variant="outline" asChild>
              <Link to={`/lists/${id}/edit`}>
                <Pencil className="h-4 w-4 mr-1.5" />
                Modifier
              </Link>
            </Button>
            <Button asChild>
              <Link to={`/lists/${id}/items/new`}>
                <Plus className="h-4 w-4 mr-1.5" />
                Ajouter article
              </Link>
            </Button>
          </div>
        </div>
      </div>

      {/* Status filter pills */}
      <div className="flex items-center justify-between mb-6 gap-4">
        <div className="flex gap-2 flex-wrap" role="tablist" aria-label="Filtrer par statut">
          <button
            onClick={() => handleStatusFilterChange("")}
            role="tab"
            aria-selected={statusFilter === ""}
            className={cn(
              "px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-200 border",
              statusFilter === ""
                ? "bg-foreground text-background border-foreground"
                : "bg-background text-muted-foreground border-border hover:border-foreground/30",
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
                "px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-200 border",
                statusFilter === status
                  ? "bg-foreground text-background border-foreground"
                  : "bg-background text-muted-foreground border-border hover:border-foreground/30",
              )}
            >
              {STATUS_LABELS[status]}
            </button>
          ))}
        </div>
        <p className="text-sm text-muted-foreground shrink-0">
          {itemsLoading
            ? "..."
            : `${itemTotalElements} article${itemTotalElements !== 1 ? "s" : ""}`}
        </p>
      </div>

      {itemsLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {[...Array(4)].map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : (
        <>
          <StaggeredList className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
            {items.map((item) => {
              const safeImageUrl = sanitizeImageUrl(item.imageUrl);
              return (
              <StaggeredItem key={item.id}>
                <div className="group rounded-2xl border bg-card shadow-card overflow-hidden transition-all duration-300 hover:shadow-elevated">
                  <div className="aspect-[4/3] bg-muted flex items-center justify-center overflow-hidden relative">
                    {safeImageUrl ? (
                      <img
                        src={safeImageUrl}
                        alt={item.name}
                        loading="lazy"
                        className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                        onError={(e) => { e.currentTarget.style.display = 'none'; }}
                      />
                    ) : (
                      <div className="text-muted-foreground/40 flex flex-col items-center">
                        <Package className="h-10 w-10 mb-1" />
                      </div>
                    )}
                    <div className="absolute top-3 right-3">
                      <Badge variant={STATUS_BADGE_VARIANTS[item.status]}>
                        {formatStatus(item.status)}
                      </Badge>
                    </div>
                    <div className="absolute top-3 left-3 opacity-100 md:opacity-0 md:group-hover:opacity-100 md:focus-within:opacity-100 transition-opacity">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="secondary"
                            size="icon"
                            aria-label="Options de l'article"
                            className="h-8 w-8 shadow-sm"
                          >
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="start">
                          <DropdownMenuItem
                            onClick={() =>
                              navigate(`/lists/${id}/items/${item.id}/edit`)
                            }
                          >
                            <Pencil className="h-4 w-4 mr-2" />
                            Modifier
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => setPendingDeleteId(item.id)}
                            disabled={deleteMutation.isPending && deleteMutation.variables === item.id}
                            className="text-destructive focus:text-destructive"
                          >
                            <Trash2 className="h-4 w-4 mr-2" />
                            Supprimer
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>

                  <Link
                    to={`/lists/${id}/items/${item.id}/edit`}
                    className="block p-4 cursor-pointer"
                  >
                    <h3 className="font-semibold tracking-tight mb-1">
                      {item.name}
                    </h3>
                    <p className="text-muted-foreground text-sm mb-2">
                      <span className="font-medium">Stock:</span> {item.stock}
                    </p>
                    {list.customFieldDefinitions &&
                      list.customFieldDefinitions.length > 0 && (
                        <div className="space-y-0.5">
                          {[...list.customFieldDefinitions]
                            .sort((a, b) => a.displayOrder - b.displayOrder)
                            .map((def) => {
                              const value = item.customFieldValues?.[def.name];
                              if (
                                value === undefined ||
                                value === null ||
                                value === ""
                              )
                                return null;
                              return (
                                <p
                                  key={def.name}
                                  className="text-muted-foreground text-xs"
                                >
                                  <span className="font-medium">
                                    {def.label}:
                                  </span>{" "}
                                  {formatCustomFieldValue(def.type, value)}
                                </p>
                              );
                            })}
                        </div>
                      )}
                  </Link>
                </div>
              </StaggeredItem>
              );
            })}
          </StaggeredList>

          {items.length === 0 && (
            <div className="text-center py-20 animate-fade-in">
              <Package className="h-12 w-12 mx-auto mb-3 text-muted-foreground/40" />
              <p className="text-lg text-muted-foreground mb-4">
                Aucun article dans cette liste.
              </p>
              <Button asChild>
                <Link to={`/lists/${id}/items/new`}>Ajouter votre premier article</Link>
              </Button>
            </div>
          )}

          <Pagination page={itemPage} totalPages={itemTotalPages} onPageChange={setItemPage} />
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
