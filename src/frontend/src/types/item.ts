export type CustomFieldType = "TEXT" | "NUMBER" | "DATE" | "BOOLEAN" | "SELECT";

export interface CustomFieldDefinition {
  name: string;
  label: string;
  type: CustomFieldType;
  required: boolean;
  displayOrder: number;
  options?: string[];
}

export const FIELD_TYPE_OPTIONS: CustomFieldType[] = [
  "TEXT",
  "NUMBER",
  "DATE",
  "BOOLEAN",
  "SELECT",
];

export const FIELD_TYPE_LABELS: Record<CustomFieldType, string> = {
  TEXT: "Texte",
  NUMBER: "Nombre",
  DATE: "Date",
  BOOLEAN: "Oui/Non",
  SELECT: "Sélection",
};

export const formatCustomFieldValue = (
  type: CustomFieldType,
  value: unknown,
): string => {
  switch (type) {
    case "BOOLEAN":
      return value ? "Oui" : "Non";
    case "DATE":
      return new Date(
        typeof value === "string" ? value : String(value),
      ).toLocaleDateString("fr-FR");
    case "NUMBER":
      return String(value);
    case "SELECT":
      return String(value);
    case "TEXT":
    default:
      return String(value);
  }
};

export interface ItemList {
  id: string;
  name: string;
  description?: string;
  category?: string;
  itemCount?: number;
  customFieldDefinitions?: CustomFieldDefinition[];
  workspaceId?: string;
  workspaceName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ItemListWithItems extends ItemList {
  items: Item[];
}

export interface ItemListFormData {
  name: string;
  description?: string;
  category?: string;
  customFieldDefinitions?: CustomFieldDefinition[];
  workspaceId?: string;
}

export interface Item {
  id: string;
  name: string;
  itemListId: string;
  status: ItemStatus;
  stock: number;
  barcode?: string | null;
  imageUrl?: string | null;
  customFieldValues?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface ItemFormData {
  name: string;
  itemListId: string;
  status: ItemStatus;
  stock: number;
  barcode?: string;
  customFieldValues?: Record<string, unknown>;
}

export interface ListOverviewDto {
  listName: string;
  itemsCount: number;
  totalQuantity: number;
}

export interface RecentItemDto {
  id: string;
  name: string;
  listId: string;
  listName: string;
  quantity: number;
  status: string;
  lastUpdated: string;
}

export interface DashboardStats {
  totalItems: number;
  totalQuantity: number;
  toVerifyCount: number;
  needsAttentionCount: number;
  countByStatus: Record<string, number>;
  countByCategory: Record<string, number>;
  listsOverview: ListOverviewDto[];
  recentlyUpdated: RecentItemDto[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AdminItemList {
  id: string;
  name: string;
  description?: string;
  category?: string;
  itemCount: number;
  ownerId: string;
  ownerEmail: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminItem {
  id: string;
  name: string;
  status: ItemStatus;
  stock: number;
  barcode?: string | null;
  imageUrl?: string | null;
  listId: string;
  listName: string;
  ownerId: string;
  ownerEmail: string;
  customFieldValues?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface ItemSearchParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
  search?: string;
  itemListId?: string;
  status?: ItemStatus;
}

export interface ItemListSearchParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: "asc" | "desc";
  search?: string;
  workspaceId?: string;
}

export const STATUS_OPTIONS = [
  "AVAILABLE",
  "TO_VERIFY",
  "NEEDS_MAINTENANCE",
  "DAMAGED",
] as const;

export type ItemStatus = (typeof STATUS_OPTIONS)[number];

export const STATUS_LABELS: Record<ItemStatus, string> = {
  AVAILABLE: "Disponible",
  TO_VERIFY: "À vérifier",
  NEEDS_MAINTENANCE: "Maintenance requise",
  DAMAGED: "Endommagé",
};

export const formatStatus = (status: ItemStatus): string => {
  return STATUS_LABELS[status] || status;
};

export const STATUS_BADGE_VARIANTS: Record<ItemStatus, "success" | "warning" | "error" | "default"> = {
  AVAILABLE: "success",
  TO_VERIFY: "warning",
  NEEDS_MAINTENANCE: "default",
  DAMAGED: "error",
};

// AI Image Analysis
export type AnalysisStatus = "PENDING" | "COMPLETED" | "FAILED";

export interface ImageAnalysisResult {
  analysisId: string;
  status: AnalysisStatus;
  suggestedName?: string | null;
  suggestedStatus?: ItemStatus | null;
  suggestedStock?: number | null;
  suggestedCustomFieldValues?: Record<string, unknown> | null;
  errorMessage?: string | null;
}
