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
  ITEM_CREATED: 'a créé l\'article',
  ITEM_UPDATED: 'a modifié l\'article',
  ITEM_DELETED: 'a supprimé l\'article',
  LIST_CREATED: 'a créé la liste',
  LIST_UPDATED: 'a modifié la liste',
  LIST_DELETED: 'a supprimé la liste',
  MEMBER_ADDED: 'a rejoint l\'espace de travail',
  MEMBER_REMOVED: 'a quitté l\'espace de travail',
  MEMBER_ROLE_CHANGED: 'a eu son rôle modifié',
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
