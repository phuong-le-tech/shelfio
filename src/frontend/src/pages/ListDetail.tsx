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
  GripVertical,
} from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { listsApi, itemsApi } from "../services/api";
import BarcodeScannerModal from "../components/BarcodeScannerModal";
import {
  type ItemStatus,
  formatStatus,
  STATUS_OPTIONS,
  STATUS_LABELS,
  STATUS_BADGE_VARIANTS,
  formatCustomFieldValue,
  type Item,
  type ItemListWithItems,
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
import {
  DndContext,
  closestCenter,
  type DragEndEvent,
  PointerSensor,
  KeyboardSensor,
  useSensor,
  useSensors,
} from "@dnd-kit/core";
import {
  SortableContext,
  rectSortingStrategy,
  useSortable,
  arrayMove,
  sortableKeyboardCoordinates,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";

const ITEMS_PER_PAGE = 12;

const SORT_OPTIONS = [
  { value: "createdAt", label: "Date ↓", dir: "desc" },
  { value: "updatedAt", label: "Mis à jour", dir: "desc" },
  { value: "name", label: "Nom", dir: "asc" },
  { value: "status", label: "Statut", dir: "asc" },
  { value: "stock", label: "Stock ↓", dir: "desc" },
  { value: "position", label: "Personnalisé", dir: "asc" },
] as const;
type SortByValue = (typeof SORT_OPTIONS)[number]["value"];

type SortableItemCardProps = Readonly<{
  item: Item;
  list: ItemListWithItems;
  listId: string;
  selectMode: boolean;
  isSelected: boolean;
  isViewer: boolean;
  isDndMode: boolean;
  navigate: (path: string) => void;
  onToggleSelect: (id: string) => void;
  onDeleteRequest: (id: string) => void;
  deleteIsPending: boolean;
}>;

type ItemCardOverlayProps = Readonly<{
  isDndMode: boolean;
  selectMode: boolean;
  isSelected: boolean;
  isViewer: boolean;
  item: Item;
  listId: string;
  navigate: (path: string) => void;
  onDeleteRequest: (id: string) => void;
  deleteIsPending: boolean;
  dragAttributes: ReturnType<typeof useSortable>["attributes"];
  dragListeners: ReturnType<typeof useSortable>["listeners"];
}>;

function ItemCardOverlay({
  isDndMode,
  selectMode,
  isSelected,
  isViewer,
  item,
  listId,
  navigate,
  onDeleteRequest,
  deleteIsPending,
  dragAttributes,
  dragListeners,
}: ItemCardOverlayProps) {
  if (isDndMode) {
    return (
      <div className="absolute top-3 left-3" {...dragAttributes} {...dragListeners}>
        <div className="h-8 w-8 bg-background/90 rounded-md flex items-center justify-center cursor-grab active:cursor-grabbing shadow-sm">
          <GripVertical className="h-4 w-4 text-muted-foreground" />
        </div>
      </div>
    );
  }
  if (selectMode) {
    const indicatorClass = isSelected ? "bg-brand border-brand" : "bg-background/90 border-border";
    return (
      <div className="absolute top-3 left-3">
        <div
          className={cn(
            "h-6 w-6 rounded-full border-2 flex items-center justify-center shadow-sm transition-colors",
            indicatorClass
          )}
        >
          {isSelected && <Check className="h-3.5 w-3.5 text-white" />}
        </div>
      </div>
    );
  }
  if (isViewer) return null;
  return (
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
            onClick={() => navigate(`/lists/${listId}/items/${item.id}/edit`)}
          >
            <Pencil className="h-4 w-4 mr-2" />
            Modifier
          </DropdownMenuItem>
          <DropdownMenuItem
            onClick={() => onDeleteRequest(item.id)}
            disabled={deleteIsPending}
            className="text-destructive focus:text-destructive"
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Supprimer
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

type ItemCardBodyProps = Readonly<{
  selectMode: boolean;
  isDndMode: boolean;
  item: Item;
  list: ItemListWithItems;
  listId: string;
}>;

function ItemCardBody({ selectMode, isDndMode, item, list, listId }: ItemCardBodyProps) {
  if (selectMode) {
    return (
      <div className="p-4">
        <h3 className="font-semibold tracking-tight mb-1">{item.name}</h3>
        <p className="text-muted-foreground text-sm">
          <span className="font-medium">Stock:</span> {item.stock}
        </p>
      </div>
    );
  }
  if (isDndMode) {
    return (
      <div className="p-4">
        <h3 className="font-semibold tracking-tight mb-1">{item.name}</h3>
        <p className="text-muted-foreground text-sm mb-1">
          <span className="font-medium">Stock:</span> {item.stock}
        </p>
        {item.barcode && (
          <p className="text-muted-foreground text-xs mb-1 font-mono truncate">
            <span className="font-medium font-sans">Code-barres:</span> {item.barcode}
          </p>
        )}
      </div>
    );
  }
  return (
    <Link
      to={`/lists/${listId}/items/${item.id}/edit`}
      className="block p-4 cursor-pointer"
    >
      <h3 className="font-semibold tracking-tight mb-1">{item.name}</h3>
      <p className="text-muted-foreground text-sm mb-1">
        <span className="font-medium">Stock:</span> {item.stock}
      </p>
      {item.barcode && (
        <p className="text-muted-foreground text-xs mb-1 font-mono truncate">
          <span className="font-medium font-sans">Code-barres:</span> {item.barcode}
        </p>
      )}
      {list.customFieldDefinitions && list.customFieldDefinitions.length > 0 && (
        <div className="space-y-0.5">
          {[...list.customFieldDefinitions]
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map((def) => {
              const value = item.customFieldValues?.[def.name];
              if (value === undefined || value === null || value === "") return null;
              return (
                <p key={def.name} className="text-muted-foreground text-xs">
                  <span className="font-medium">{def.label}:</span>{" "}
                  {formatCustomFieldValue(def.type, value)}
                </p>
              );
            })}
        </div>
      )}
    </Link>
  );
}

function SortableItemCard({
  item,
  list,
  listId,
  selectMode,
  isSelected,
  isViewer,
  isDndMode,
  navigate,
  onToggleSelect,
  onDeleteRequest,
  deleteIsPending,
}: SortableItemCardProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: item.id, disabled: !isDndMode });

  const style = isDndMode
    ? { transform: CSS.Transform.toString(transform), transition, opacity: isDragging ? 0.5 : 1, zIndex: isDragging ? 10 : undefined }
    : undefined;

  const safeImageUrl = sanitizeImageUrl(item.imageUrl);

  const cardClass = cn(
    "group rounded-2xl border bg-card shadow-card overflow-hidden transition-all duration-300 hover:shadow-elevated",
    isSelected && "ring-2 ring-brand",
    isDndMode && "cursor-default"
  );

  const cardInner = (
    <>
      <div className="aspect-[4/3] bg-muted flex items-center justify-center overflow-hidden relative">
        {safeImageUrl ? (
          <img
            src={safeImageUrl}
            alt={item.name}
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            onError={(e) => { e.currentTarget.style.display = "none"; }}
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
        <ItemCardOverlay
          isDndMode={isDndMode}
          selectMode={selectMode}
          isSelected={isSelected}
          isViewer={isViewer}
          item={item}
          listId={listId}
          navigate={navigate}
          onDeleteRequest={onDeleteRequest}
          deleteIsPending={deleteIsPending}
          dragAttributes={attributes}
          dragListeners={listeners}
        />
      </div>
      <ItemCardBody
        selectMode={selectMode}
        isDndMode={isDndMode}
        item={item}
        list={list}
        listId={listId}
      />
    </>
  );

  return (
    <div ref={setNodeRef} style={style}>
      {selectMode ? (
        <label className={cn(cardClass, "cursor-pointer block")}>
          <input
            type="checkbox"
            checked={isSelected}
            onChange={() => onToggleSelect(item.id)}
            className="sr-only"
          />
          {cardInner}
        </label>
      ) : (
        <div className={cardClass}>{cardInner}</div>
      )}
    </div>
  );
}

type ItemsGridProps = Readonly<{
  isDndMode: boolean;
  localItems: Item[];
  items: Item[];
  list: ItemListWithItems;
  listId: string;
  selectMode: boolean;
  selectedIds: Set<string>;
  isViewer: boolean;
  navigate: (path: string) => void;
  sensors: ReturnType<typeof useSensors>;
  onDragEnd: (event: DragEndEvent) => void;
  onToggleSelect: (id: string) => void;
  onDeleteRequest: (id: string) => void;
  deleteMutation: { isPending: boolean; variables: string | undefined };
}>;

function ItemsGrid({
  isDndMode,
  localItems,
  items,
  list,
  listId,
  selectMode,
  selectedIds,
  isViewer,
  navigate,
  sensors,
  onDragEnd,
  onToggleSelect,
  onDeleteRequest,
  deleteMutation,
}: ItemsGridProps) {
  const gridClass = "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5";

  if (isDndMode) {
    return (
      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
        <SortableContext items={localItems.map((i) => i.id)} strategy={rectSortingStrategy}>
          <div className={gridClass}>
            {localItems.map((item) => (
              <SortableItemCard
                key={item.id}
                item={item}
                list={list}
                listId={listId}
                selectMode={false}
                isSelected={false}
                isViewer={isViewer}
                isDndMode={true}
                navigate={navigate}
                onToggleSelect={onToggleSelect}
                onDeleteRequest={onDeleteRequest}
                deleteIsPending={deleteMutation.isPending && deleteMutation.variables === item.id}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>
    );
  }

  return (
    <StaggeredList className={gridClass}>
      {items.map((item) => (
        <StaggeredItem key={item.id}>
          <SortableItemCard
            item={item}
            list={list}
            listId={listId}
            selectMode={selectMode}
            isSelected={selectedIds.has(item.id)}
            isViewer={isViewer}
            isDndMode={false}
            navigate={navigate}
            onToggleSelect={onToggleSelect}
            onDeleteRequest={onDeleteRequest}
            deleteIsPending={deleteMutation.isPending && deleteMutation.variables === item.id}
          />
        </StaggeredItem>
      ))}
    </StaggeredList>
  );
}

export default function ListDetail() {
  const { id } = useParams() as { id: string };
  const navigate = useNavigate();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const { currentWorkspace } = useWorkspace();
  const isViewer = currentWorkspace?.role === "VIEWER";

  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<ItemStatus | "">("");
  const [itemPage, setItemPage] = useState(0);
  const [isExporting, setIsExporting] = useState(false);
  const [scannerOpen, setScannerOpen] = useState(false);
  const [selectMode, setSelectMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [bulkDeleteConfirmOpen, setBulkDeleteConfirmOpen] = useState(false);
  const [sortBy, setSortBy] = useState<SortByValue>("createdAt");
  const [localItems, setLocalItems] = useState<Item[]>([]);

  const isDndMode = sortBy === "position" && !selectMode;

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const { data: list, isLoading: listLoading, error: listError } = useQuery({
    queryKey: queryKeys.lists.detail(id),
    queryFn: () => listsApi.getById(id),
    enabled: !!id,
  });

  // Navigate away on list fetch error
  useEffect(() => {
    if (listError && !list) {
      showToast("Échec du chargement de la liste", "error");
      navigate("/lists");
    }
  }, [listError, list, showToast, navigate]);

  const sortOption = SORT_OPTIONS.find((o) => o.value === sortBy) ?? SORT_OPTIONS[0];
  const itemParams = {
    itemListId: id,
    status: statusFilter || undefined,
    page: isDndMode ? 0 : itemPage,
    size: isDndMode ? 500 : ITEMS_PER_PAGE,
    sortBy,
    sortDir: sortOption.dir,
  };

  const { data: itemsData, isLoading: itemsLoading } = useQuery({
    queryKey: queryKeys.items.list(itemParams),
    queryFn: () => itemsApi.getAll(itemParams),
    enabled: !!id,
  });

  const items = itemsData?.content ?? [];
  const itemTotalPages = itemsData?.totalPages ?? 0;
  const itemTotalElements = itemsData?.totalElements ?? 0;

  // Keep localItems in sync with fetched items
  useEffect(() => {
    setLocalItems(items);
  }, [items]);

  const deleteMutation = useMutation({
    mutationFn: (itemId: string) => itemsApi.delete(itemId),
    onSuccess: () => {
      showToast("Article supprimé avec succès", "success");
      queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.lists.detail(id) });
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
      showToast(
        `${count} article${count === 1 ? "" : "s"} supprimé${count === 1 ? "" : "s"}`,
        "success"
      );
      setSelectedIds(new Set());
      setSelectMode(false);
      queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.lists.detail(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all });
    },
    onError: () => {
      showToast("Échec de la suppression", "error");
    },
  });

  const reorderMutation = useMutation({
    mutationFn: ({ listId, orderedIds }: { listId: string; orderedIds: string[] }) =>
      itemsApi.reorder(listId, orderedIds),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
    },
    onError: () => {
      showToast("Échec de la réorganisation", "error");
      // Restore from server state on error
      setLocalItems(items);
    },
  });

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (isViewer || !over || active.id === over.id) return;

    const oldIndex = localItems.findIndex((i) => i.id === active.id.toString());
    const newIndex = localItems.findIndex((i) => i.id === over.id.toString());
    if (oldIndex === -1 || newIndex === -1) return;

    const reordered = arrayMove(localItems, oldIndex, newIndex);
    setLocalItems(reordered);
    reorderMutation.mutate({
      listId: id,
      orderedIds: reordered.map((i) => i.id),
    });
  };

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value as ItemStatus | "");
    setItemPage(0);
    setSelectMode(false);
    setSelectedIds(new Set());
  };

  const handleSortChange = (value: SortByValue) => {
    setSortBy(value);
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
    const preview =
      names.length <= 5
        ? names.map((n) => `• ${n}`).join("\n")
        : [
            ...names.slice(0, 5).map((n) => `• ${n}`),
            `• et ${names.length - 5} autre${names.length - 5 === 1 ? "" : "s"}`,
          ].join("\n");
    return `Êtes-vous sûr de vouloir supprimer ${count} article${count === 1 ? "" : "s"} ? Cette action est irréversible.\n\n${preview}`;
  };

  const handleBulkDeleteConfirm = () => {
    setBulkDeleteConfirmOpen(false);
    bulkDeleteMutation.mutate(Array.from(selectedIds));
  };

  const handleBarcodeScan = async (barcode: string) => {
    try {
      const item = await itemsApi.getByBarcode(barcode);
      if (item) {
        navigate(`/lists/${item.itemListId}/items/${item.id}/edit`);
      } else {
        navigate(`/lists/${id}/items/new?barcode=${encodeURIComponent(barcode)}`);
      }
    } catch {
      navigate(`/lists/${id}/items/new?barcode=${encodeURIComponent(barcode)}`);
    }
  };

  const handleExportCsv = async () => {
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

  const allOnPageSelected =
    items.length > 0 && selectedIds.size === items.length;

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      <Breadcrumb
        items={[{ label: "Mes Listes", href: "/lists" }, { label: list.name }]}
      />

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
            <Button variant="outline" onClick={() => setScannerOpen(true)}>
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

      {/* Status filter pills + sort + select controls */}
      <div className="flex items-center justify-between mb-6 gap-4">
        <div
          className="flex gap-2 flex-wrap"
          role="tablist"
          aria-label="Filtrer par statut"
        >
          <button
            onClick={() => handleStatusFilterChange("")}
            role="tab"
            aria-selected={statusFilter === ""}
            className={cn(
              "px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-200 border",
              statusFilter === ""
                ? "bg-foreground text-background border-foreground"
                : "bg-background text-muted-foreground border-border hover:border-foreground/30"
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
                  : "bg-background text-muted-foreground border-border hover:border-foreground/30"
              )}
            >
              {STATUS_LABELS[status]}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-3 shrink-0">
          {!selectMode && (
            <select
              value={sortBy}
              onChange={(e) => handleSortChange(e.target.value as SortByValue)}
              aria-label="Trier par"
              className="text-sm text-muted-foreground bg-background border border-border rounded-lg px-2 py-1 cursor-pointer hover:border-foreground/30 transition-colors focus:outline-none focus:ring-1 focus:ring-ring"
            >
              {SORT_OPTIONS.filter(
                (opt) => opt.value !== "position" || !isViewer
              ).map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          )}
          <p className="text-sm text-muted-foreground">
            {itemsLoading
              ? "..."
              : `${itemTotalElements} article${itemTotalElements !== 1 ? "s" : ""}`}
          </p>
          {!isViewer && items.length > 0 && !selectMode && !isDndMode && (
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
                <div
                  className={cn(
                    "h-4 w-4 rounded border flex items-center justify-center transition-colors",
                    allOnPageSelected
                      ? "bg-brand border-brand"
                      : "border-border bg-background"
                  )}
                >
                  {allOnPageSelected && (
                    <Check className="h-3 w-3 text-white" />
                  )}
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
          <ItemsGrid
            isDndMode={isDndMode}
            localItems={localItems}
            items={items}
            list={list}
            listId={id}
            selectMode={selectMode}
            selectedIds={selectedIds}
            isViewer={isViewer}
            navigate={navigate}
            sensors={sensors}
            onDragEnd={handleDragEnd}
            onToggleSelect={toggleSelect}
            onDeleteRequest={setPendingDeleteId}
            deleteMutation={deleteMutation}
          />

          {items.length === 0 && (
            <div className="text-center py-20 animate-fade-in">
              <Package className="h-12 w-12 mx-auto mb-3 text-muted-foreground/40" />
              <p className="text-lg text-muted-foreground mb-4">
                Aucun article dans cette liste.
              </p>
              <Button asChild>
                <Link to={`/lists/${id}/items/new`}>
                  Ajouter votre premier article
                </Link>
              </Button>
            </div>
          )}

          {!isDndMode && (
            <Pagination
              page={itemPage}
              totalPages={itemTotalPages}
              onPageChange={setItemPage}
            />
          )}
        </>
      )}

      {/* Bulk action bar */}
      {selectedIds.size > 0 && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 bg-card border shadow-float rounded-2xl px-4 py-3 animate-fade-in-up">
          <span className="text-sm font-medium">
            {selectedIds.size} article{selectedIds.size === 1 ? "" : "s"}{" "}
            sélectionné{selectedIds.size === 1 ? "" : "s"}
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
        title={`Supprimer ${selectedIds.size} article${selectedIds.size === 1 ? "" : "s"}`}
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
