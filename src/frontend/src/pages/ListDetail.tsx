import { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import {
  ArrowLeft,
  Plus,
  Pencil,
  Trash2,
  Package,
  List,
  ChevronLeft,
  ChevronRight,
  MoreHorizontal,
} from "lucide-react";
import axios from "axios";
import { listsApi, itemsApi } from "../services/api";
import {
  ItemList,
  Item,
  ItemStatus,
  formatStatus,
  STATUS_OPTIONS,
  STATUS_LABELS,
  getItemImageUrl,
  formatCustomFieldValue,
} from "../types/item";
import { SkeletonCard, SkeletonText } from "../components/Skeleton";
import { useToast } from "../components/Toast";
import ConfirmModal from "../components/ConfirmModal";
import { Button } from "@/components/ui/button";
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

const ITEMS_PER_PAGE = 12;

const statusToBadgeVariant: Record<
  ItemStatus,
  "success" | "warning" | "error" | "default"
> = {
  IN_STOCK: "success",
  LOW_STOCK: "warning",
  OUT_OF_STOCK: "error",
};

export default function ListDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [list, setList] = useState<ItemList | null>(null);
  const [listLoading, setListLoading] = useState(true);
  const [items, setItems] = useState<Item[]>([]);
  const [itemsLoading, setItemsLoading] = useState(true);
  const [itemPage, setItemPage] = useState(0);
  const [itemTotalPages, setItemTotalPages] = useState(0);
  const [itemTotalElements, setItemTotalElements] = useState(0);
  const [itemsReloadKey, setItemsReloadKey] = useState(0);
  const [deletingItemId, setDeletingItemId] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<ItemStatus | "">("");

  useEffect(() => {
    if (!id) return;
    const controller = new AbortController();
    const load = async () => {
      setListLoading(true);
      try {
        const listData = await listsApi.getById(id, controller.signal);
        if (!controller.signal.aborted) setList(listData);
      } catch (err) {
        if (!axios.isCancel(err) && !controller.signal.aborted) {
          showToast("Echec du chargement de la liste", "error");
          navigate("/lists");
        }
      } finally {
        if (!controller.signal.aborted) setListLoading(false);
      }
    };
    load();
    return () => controller.abort();
  }, [id, navigate, showToast]);

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
            sortBy: "createdAt",
            sortDir: "desc",
          },
          controller.signal,
        );
        if (!controller.signal.aborted) {
          setItems(response.content);
          setItemTotalPages(response.totalPages);
          setItemTotalElements(response.totalElements);
        }
      } catch (err) {
        if (!axios.isCancel(err) && !controller.signal.aborted) {
          showToast("Echec du chargement des articles", "error");
        }
      } finally {
        if (!controller.signal.aborted) setItemsLoading(false);
      }
    };
    load();
    return () => controller.abort();
  }, [id, statusFilter, itemPage, itemsReloadKey, showToast]);

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value as ItemStatus | "");
    setItemPage(0);
  };

  const handleDeleteConfirm = async () => {
    if (!pendingDeleteId) return;
    const itemId = pendingDeleteId;
    setPendingDeleteId(null);
    setDeletingItemId(itemId);
    try {
      await itemsApi.delete(itemId);
      showToast("Article supprimé avec succès", "success");
      setItemsReloadKey((k) => k + 1);
    } catch {
      showToast("Échec de la suppression de l'article", "error");
    } finally {
      setDeletingItemId(null);
    }
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
        <Link to="/lists">
          <Button variant="outline">
            <ArrowLeft className="h-4 w-4 mr-1.5" />
            Retour aux listes
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <button
        onClick={() => navigate("/lists")}
        className="inline-flex items-center text-muted-foreground hover:text-foreground mb-8 transition-all duration-200 hover:-translate-x-0.5 group text-sm"
      >
        <ArrowLeft className="h-4 w-4 mr-1.5 transition-transform group-hover:-translate-x-0.5" />
        Retour aux listes
      </button>

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
            <Link to={`/lists/${id}/edit`}>
              <Button variant="outline">
                <Pencil className="h-4 w-4 mr-1.5" />
                Modifier
              </Button>
            </Link>
            <Link to={`/lists/${id}/items/new`}>
              <Button>
                <Plus className="h-4 w-4 mr-1.5" />
                Ajouter article
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Status filter pills */}
      <div className="flex items-center justify-between mb-6 gap-4">
        <div className="flex gap-2 flex-wrap">
          <button
            onClick={() => handleStatusFilterChange("")}
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
            {items.map((item) => (
              <StaggeredItem key={item.id}>
                <div className="group rounded-2xl border bg-card shadow-card overflow-hidden transition-all duration-300 hover:shadow-elevated">
                  <div className="aspect-[4/3] bg-muted flex items-center justify-center overflow-hidden relative">
                    {item.hasImage ? (
                      <img
                        src={getItemImageUrl(item.id)}
                        alt={item.name}
                        className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                      />
                    ) : (
                      <div className="text-muted-foreground/40 flex flex-col items-center">
                        <Package className="h-10 w-10 mb-1" />
                      </div>
                    )}
                    <div className="absolute top-3 right-3">
                      <Badge variant={statusToBadgeVariant[item.status]}>
                        {formatStatus(item.status)}
                      </Badge>
                    </div>
                    <div className="absolute top-3 left-3 opacity-0 group-hover:opacity-100 transition-opacity">
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
                            disabled={deletingItemId === item.id}
                            className="text-destructive focus:text-destructive"
                          >
                            <Trash2 className="h-4 w-4 mr-2" />
                            Supprimer
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>

                  <div className="p-4">
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
                  </div>
                </div>
              </StaggeredItem>
            ))}
          </StaggeredList>

          {items.length === 0 && (
            <div className="text-center py-20 animate-fade-in">
              <Package className="h-12 w-12 mx-auto mb-3 text-muted-foreground/40" />
              <p className="text-lg text-muted-foreground mb-4">
                Aucun article dans cette liste.
              </p>
              <Link to={`/lists/${id}/items/new`}>
                <Button>Ajouter votre premier article</Button>
              </Link>
            </div>
          )}

          {itemTotalPages > 1 && (
            <div className="flex items-center justify-center gap-4 mt-8">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setItemPage((p) => Math.max(0, p - 1))}
                disabled={itemPage === 0}
              >
                <ChevronLeft className="h-4 w-4 mr-1" />
                Précédent
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {itemPage + 1} sur {itemTotalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() =>
                  setItemPage((p) => Math.min(itemTotalPages - 1, p + 1))
                }
                disabled={itemPage >= itemTotalPages - 1}
              >
                Suivant
                <ChevronRight className="h-4 w-4 ml-1" />
              </Button>
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
