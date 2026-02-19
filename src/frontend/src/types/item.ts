export type CustomFieldType = 'TEXT' | 'NUMBER' | 'DATE' | 'BOOLEAN';

export interface CustomFieldDefinition {
  name: string;
  label: string;
  type: CustomFieldType;
  required: boolean;
  displayOrder: number;
}

export const FIELD_TYPE_OPTIONS: CustomFieldType[] = ['TEXT', 'NUMBER', 'DATE', 'BOOLEAN'];

export const FIELD_TYPE_LABELS: Record<CustomFieldType, string> = {
  TEXT: 'Texte',
  NUMBER: 'Nombre',
  DATE: 'Date',
  BOOLEAN: 'Oui/Non',
};

export const formatCustomFieldValue = (type: CustomFieldType, value: unknown): string => {
  switch (type) {
    case 'BOOLEAN':
      return value ? 'Oui' : 'Non';
    case 'DATE':
      return new Date(value as string).toLocaleDateString('fr-FR');
    case 'NUMBER':
      return String(value);
    case 'TEXT':
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
  status: string;
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
  status: string;
  stock: number;
  customFieldValues?: Record<string, unknown>;
}

export interface DashboardStats {
  totalItems: number;
  countByStatus: Record<string, number>;
  countByCategory: Record<string, number>;
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
  sortDir?: 'asc' | 'desc';
  search?: string;
  itemListId?: string;
  status?: string;
}

export interface ItemListSearchParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export const STATUS_OPTIONS = ['TO_PREPARE', 'TO_VERIFY', 'PENDING', 'READY', 'ARCHIVED'] as const;

export const STATUS_LABELS: Record<string, string> = {
  TO_PREPARE: 'À préparer',
  TO_VERIFY: 'À vérifier',
  PENDING: 'En attente',
  READY: 'Prêt',
  ARCHIVED: 'Archivé',
};

export const formatStatus = (status: string): string => {
  return STATUS_LABELS[status] || status;
};
