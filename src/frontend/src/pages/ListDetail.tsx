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
  Download,
  ScanLine,
  Check,
} from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { listsApi, itemsApi } from "../services/api";
import BarcodeScannerModal from "../components/BarcodeScannerModal";
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
import { getApiErrorMessage } from "../utils/errorUtils";
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
import { useWorkspace } from "../contexts/WorkspaceContext";

const ITEMS_PER_PAGE = 12;

export default function ListDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const { currentWorkspace } = useWorkspace();
  const isViewer = currentWorkspace?.role === 'VIEWER';

  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<ItemStatus | "">("");
  const [itemPage, setItemPage] = useState(0);
  const [isExporting, setIsExporting] = useState(false);
  const [scannerOpen, setScannerOpen] = useState(false);
  const [selectMode, setSelectMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [bulkDeleteConfirmOpen, setBulkDeleteConfirmOpen] = useState(false);

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

  const bulkDeleteMutation = useMutation({
    mutationFn: (ids: string[]) => itemsApi.bulkDelete(ids),
    onSuccess: () => {
      const count = selectedIds.size;
      showToast(`${count} article${count !== 1 ? "s" : ""} supprimé${count !== 1 ? "s" : ""}`, "success");
      setSelectedIds(new Set());
      setSelectMode(false);
      queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.lists.detail(id!) });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all });
    },
    onError: () => {
      showToast("Échec de la suppression", "error");
    },
  });

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value as ItemStatus | "");
    setItemPage(0);
    setSelectMode(false);
    setSelectedIds(new Set());
  };

  const handleDeleteConfirm = () => {
    if (!pendingDeleteId) return;
    const itemId = pendingDeleteId;
    setPendingDeleteId(null);
    deleteMutation.mutate(itemId);
  };

  const toggleSelect = (itemId: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(itemId)) {
        next.delete(itemId);
      } else {
        next.add(itemId);
      }
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === items.length && items.length > 0) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(items.map((item) => item.id)));
    }
  };

  const exitSelectMode = () => {
    setSelectMode(false);
    setSelectedIds(new Set());
  };

  const buildBulkDeleteMessage = () => {
    const count = selectedIds.size;
    const selectedItems = items.filter((item) => selectedIds.has(item.id));
    const names = selectedItems.map((item) => item.name);
    const preview = names.length <= 5
      ? names.map((n) => `• ${n}`).join("\n")
      : [...names.slice(0, 5).map((n) => `• ${n}`), `• et ${names.length - 5} autre${names.length - 5 !== 1 ? "s" : ""}`].join("\n");
    return `Êtes-vous sûr de vouloir supprimer ${count} article${count !== 1 ? "s" : ""} ? Cette action est irréversible.\n\n${preview}`;
  };

  const handleBulkDeleteConfirm = () => {
    setBulkDeleteConfirmOpen(false);
    bulkDeleteMutation.mutate(Array.from(selectedIds));
  };

  const handleBarcodeScan = async (barcode: string) => {
    try {
      const item = await itemsApi.getByBarcode(barcode);
      if (item && item.itemListId === id) {
        navigate(`/lists/${id}/items/${item.id}/edit`);
      } else if (item) {
        // Item exists but in a different list — navigate to its edit page
        navigate(`/lists/${item.itemListId}/items/${item.id}/edit`);
      } else {
        // No item found — create new with barcode prefilled
        navigate(`/lists/${id}/items/new?barcode=${encodeURIComponent(barcode)}`);
      }
    } catch {
      navigate(`/lists/${id}/items/new?barcode=${encodeURIComponent(barcode)}`);
    }
  };

  const handleExportCsv = async () => {
    if (!id) return;
    setIsExporting(true);
    try {
      const { blob, filename } = await listsApi.exportCsv(id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      a.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      showToast("Export CSV téléchargé", "success");
    } catch (err: unknown) {
      showToast(
        getApiErrorMessage(err, "Erreur lors de l'export CSV"),
        "error"
      );
    } finally {
      setIsExporting(false);
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
        <Button variant="outline" asChild>
          <Link to="/lists">
            <ArrowLeft className="h-4 w-4 mr-1.5" />
            Retour aux listes
          </Link>
        </Button>
      </div>
    );
  }

  const allOnPageSelected = items.length > 0 && selectedIds.size === items.length;

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
          <div className="flex gap-2 flex-shrink-0 flex-wrap">
            {!isViewer && (
              <Button variant="outline" asChild>
                <Link to={`/lists/${id}/edit`}>
                  <Pencil className="h-4 w-4 mr-1.5" />
                  Modifier
                </Link>
              </Button>
            )}
            <Button
              variant="outline"
              onClick={handleExportCsv}
              disabled={isExporting}
            >
              <Download className="h-4 w-4 mr-1.5" />
              {isExporting ? "Export..." : "Exporter CSV"}
            </Button>
            <Button
              variant="outline"
              onClick={() => setScannerOpen(true)}
            >
              <ScanLine className="h-4 w-4 mr-1.5" />
              Scanner
            </Button>
            {!isViewer && (
              <Button asChild>
                <Link to={`/lists/${id}/items/new`}>
                  <Plus className="h-4 w-4 mr-1.5" />
                  Ajouter article
                </Link>
              </Button>
            )}
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
        <div className="flex items-center gap-3 shrink-0">
          <p className="text-sm text-muted-foreground">
            {itemsLoading
              ? "..."
              : `${itemTotalElements} article${itemTotalElements !== 1 ? "s" : ""}`}
          </p>
          {!isViewer && items.length > 0 && !selectMode && (
            <button
              onClick={() => setSelectMode(true)}
              className="text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              Sélectionner
            </button>
          )}
          {selectMode && (
            <div className="flex items-center gap-2">
              <button
                onClick={toggleSelectAll}
                aria-pressed={allOnPageSelected}
                className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                <div className={cn(
                  "h-4 w-4 rounded border flex items-center justify-center transition-colors",
                  allOnPageSelected ? "bg-brand border-brand" : "border-border bg-background"
                )}>
                  {allOnPageSelected && <Check className="h-3 w-3 text-white" />}
                </div>
                Tout sélectionner
              </button>
              <button
                onClick={exitSelectMode}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                Annuler
              </button>
            </div>
          )}
        </div>
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
              const isSelected = selectedIds.has(item.id);
              return (
              <StaggeredItem key={item.id}>
                <div
                  className={cn(
                    "group rounded-2xl border bg-card shadow-card overflow-hidden transition-all duration-300 hover:shadow-elevated",
                    selectMode && "cursor-pointer",
                    isSelected && "ring-2 ring-brand"
                  )}
                  onClick={selectMode ? () => toggleSelect(item.id) : undefined}
                  role={selectMode ? "checkbox" : undefined}
                  aria-checked={selectMode ? isSelected : undefined}
                  tabIndex={selectMode ? 0 : undefined}
                  onKeyDown={selectMode ? (e) => { if (e.key === ' ' || e.key === 'Enter') { e.preventDefault(); toggleSelect(item.id); } } : undefined}
                >
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
                    {selectMode ? (
                      <div className="absolute top-3 left-3">
                        <div className={cn(
                          "h-6 w-6 rounded-full border-2 flex items-center justify-center shadow-sm transition-colors",
                          isSelected ? "bg-brand border-brand" : "bg-background/90 border-border"
                        )}>
                          {isSelected && <Check className="h-3.5 w-3.5 text-white" />}
                        </div>
                      </div>
                    ) : !isViewer && (
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
                    )}
                  </div>

                  {selectMode ? (
                    <div className="p-4">
                      <h3 className="font-semibold tracking-tight mb-1">{item.name}</h3>
                      <p className="text-muted-foreground text-sm">
                        <span className="font-medium">Stock:</span> {item.stock}
                      </p>
                    </div>
                  ) : (
                  <Link
                    to={`/lists/${id}/items/${item.id}/edit`}
                    className="block p-4 cursor-pointer"
                  >
                    <h3 className="font-semibold tracking-tight mb-1">
                      {item.name}
                    </h3>
                    <p className="text-muted-foreground text-sm mb-1">
                      <span className="font-medium">Stock:</span> {item.stock}
                    </p>
                    {item.barcode && (
                      <p className="text-muted-foreground text-xs mb-1 font-mono truncate">
                        <span className="font-medium font-sans">Code-barres:</span>{" "}
                        {item.barcode}
                      </p>
                    )}
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
                  )}
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

      {/* Bulk action bar */}
      {selectedIds.size > 0 && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 bg-card border shadow-float rounded-2xl px-4 py-3 animate-fade-in-up">
          <span className="text-sm font-medium">
            {selectedIds.size} article{selectedIds.size !== 1 ? "s" : ""} sélectionné{selectedIds.size !== 1 ? "s" : ""}
          </span>
          <div className="h-4 w-px bg-border" />
          <Button
            variant="destructive"
            size="sm"
            onClick={() => setBulkDeleteConfirmOpen(true)}
            disabled={bulkDeleteMutation.isPending}
          >
            <Trash2 className="h-3.5 w-3.5 mr-1.5" />
            Supprimer
          </Button>
          <Button variant="ghost" size="sm" onClick={exitSelectMode}>
            Annuler
          </Button>
        </div>
      )}

      <ConfirmModal
        isOpen={pendingDeleteId !== null}
        title="Supprimer l'article"
        message="Êtes-vous sûr de vouloir supprimer cet article ? Cette action est irréversible."
        confirmLabel="Supprimer"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setPendingDeleteId(null)}
      />

      <ConfirmModal
        isOpen={bulkDeleteConfirmOpen}
        title={`Supprimer ${selectedIds.size} article${selectedIds.size !== 1 ? "s" : ""}`}
        message={buildBulkDeleteMessage()}
        confirmLabel="Supprimer"
        onConfirm={handleBulkDeleteConfirm}
        onCancel={() => setBulkDeleteConfirmOpen(false)}
      />

      <BarcodeScannerModal
        isOpen={scannerOpen}
        onClose={() => setScannerOpen(false)}
        onScan={handleBarcodeScan}
      />
    </div>
  );
}
