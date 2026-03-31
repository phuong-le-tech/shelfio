export type ActivityEventType =
  | 'ITEM_CREATED' | 'ITEM_UPDATED' | 'ITEM_DELETED'
  | 'LIST_CREATED' | 'LIST_UPDATED' | 'LIST_DELETED'
  | 'MEMBER_ADDED' | 'MEMBER_REMOVED' | 'MEMBER_ROLE_CHANGED';

export type EntityType = 'ITEM' | 'LIST' | 'MEMBER';

export interface ActivityEvent {
  id: string;
  workspaceId: string;
  actorId: string | null;
  actorName: string;
  actorAvatarUrl: string | null;
  action: ActivityEventType;
  entityType: EntityType;
  entityId: string | null;
  entityName: string | null;
  occurredAt: string; // ISO 8601
}

export const ACTION_LABELS: Record<ActivityEventType, string> = {
  ITEM_CREATED: 'created item',
  ITEM_UPDATED: 'updated item',
  ITEM_DELETED: 'deleted item',
  LIST_CREATED: 'created list',
  LIST_UPDATED: 'updated list',
  LIST_DELETED: 'deleted list',
  MEMBER_ADDED: 'joined the workspace',
  MEMBER_REMOVED: 'left the workspace',
  MEMBER_ROLE_CHANGED: 'role updated',
};

export interface ActivityFilters {
  actorId?: string;
  entityType?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
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
