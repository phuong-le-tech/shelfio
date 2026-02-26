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
}

export interface Item {
  id: string;
  name: string;
  itemListId: string;
  status: ItemStatus;
  stock: number;
  hasImage: boolean;
  contentType?: string;
  customFieldValues?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export const getItemImageUrl = (itemId: string): string => {
  return `/api/v1/items/${itemId}/image`;
};

export interface ItemFormData {
  name: string;
  itemListId: string;
  status: ItemStatus;
  stock: number;
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
  listName: string;
  sku: string | null;
  quantity: number;
  status: string;
  lastUpdated: string;
}

export interface DashboardStats {
  totalItems: number;
  totalQuantity: number;
  lowStockCount: number;
  outOfStockCount: number;
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
}

export const STATUS_OPTIONS = [
  "IN_STOCK",
  "LOW_STOCK",
  "OUT_OF_STOCK",
] as const;

export type ItemStatus = (typeof STATUS_OPTIONS)[number];

export const STATUS_LABELS: Record<ItemStatus, string> = {
  IN_STOCK: "En stock",
  LOW_STOCK: "Stock faible",
  OUT_OF_STOCK: "Rupture de stock",
};

export const formatStatus = (status: ItemStatus): string => {
  return STATUS_LABELS[status] || status;
};
