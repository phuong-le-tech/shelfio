import { useState } from 'react';
import { useParams, Navigate } from 'react-router-dom';
import { RefreshCw } from 'lucide-react';
import { useActivityFeed } from '../hooks/useActivityFeed';
import { Skeleton } from '../components/ui/skeleton';
import { Button } from '../components/ui/button';
import { Breadcrumb } from '../components/Breadcrumb';
import type { ActivityFilters, ActivityEvent } from '../types/activity';
import { ACTION_LABELS } from '../types/activity';

function groupByDay(events: ActivityEvent[]): Map<string, ActivityEvent[]> {
  const map = new Map<string, ActivityEvent[]>();
  for (const e of events) {
    const d = new Date(e.occurredAt);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);
    let label: string;
    if (d.toDateString() === today.toDateString()) label = "Aujourd'hui";
    else if (d.toDateString() === yesterday.toDateString()) label = 'Hier';
    else label = d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
    const group = map.get(label) ?? [];
    group.push(e);
    map.set(label, group);
  }
  return map;
}

export default function ActivityPage() {
  const { id: workspaceId } = useParams<{ id: string }>();
  if (!workspaceId) return <Navigate to="/workspaces" replace />;
  return <ActivityPageInner workspaceId={workspaceId} />;
}

function ActivityPageInner({ workspaceId }: { workspaceId: string }) {
  const [filters, setFilters] = useState<ActivityFilters>({ page: 0, size: 20 });
  const { data, loading, error, refresh } = useActivityFeed(workspaceId, filters);

  const events = data?.content ?? [];
  const grouped = groupByDay(events);

  return (
    <div className="max-w-3xl mx-auto py-8 px-4">
      <Breadcrumb
        items={[
          { label: 'Espaces de travail', href: '/workspaces' },
          { label: 'Activité' },
        ]}
      />

      <h1 className="text-2xl font-bold mb-6">Activité</h1>

      {/* Filter bar */}
      <div className="flex flex-wrap gap-3 mb-6 items-center">
        <select
          className="border rounded px-3 py-1.5 text-sm bg-background"
          value={filters.entityType ?? ''}
          onChange={(e) =>
            setFilters((f) => ({ ...f, entityType: e.target.value || undefined, page: 0 }))
          }
        >
          <option value="">Tous les types</option>
          <option value="ITEM">Items</option>
          <option value="LIST">Listes</option>
          <option value="MEMBER">Membres</option>
        </select>

        <Button variant="outline" size="sm" onClick={refresh}>
          <RefreshCw className="h-3.5 w-3.5 mr-1" />
          Actualiser
        </Button>
      </div>

      {loading && events.length === 0 && (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex gap-3 items-start">
              <Skeleton className="h-8 w-8 rounded-full" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/4" />
              </div>
            </div>
          ))}
        </div>
      )}

      {error && <p className="text-destructive text-sm">{error}</p>}

      {!loading && events.length === 0 && !error && (
        <p className="text-muted-foreground text-sm text-center py-16">
          Aucune activité pour l'instant.
        </p>
      )}

      {events.length > 0 && (
        <div className="space-y-6">
          {Array.from(grouped.entries()).map(([day, dayEvents]) => (
            <div key={day}>
              <div className="flex items-center gap-2 mb-3">
                <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
                  {day}
                </span>
                <div className="flex-1 h-px bg-border" />
              </div>
              <div className="space-y-3">
                {dayEvents.map((event) => (
                  <EventRow key={event.id} event={event} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-8">
          <Button
            variant="outline"
            size="sm"
            disabled={data.first}
            onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) - 1 }))}
          >
            Précédent
          </Button>
          <span className="text-sm text-muted-foreground self-center">
            {(filters.page ?? 0) + 1} / {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={data.last}
            onClick={() => setFilters((f) => ({ ...f, page: (f.page ?? 0) + 1 }))}
          >
            Suivant
          </Button>
        </div>
      )}
    </div>
  );
}

function EventRow({ event }: { event: ActivityEvent }) {
  const label = ACTION_LABELS[event.action];
  const ago = new Date(event.occurredAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="flex gap-3 items-start text-sm">
      <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center text-xs font-semibold shrink-0 uppercase">
        {(event.actorName ?? '??').slice(0, 2)}
      </div>
      <div className="flex-1 min-w-0">
        <p>
          <span className="font-medium">{event.actorName ?? 'Utilisateur supprimé'}</span>{' '}
          <span className="text-muted-foreground">{label}</span>
          {event.entityName && (
            <>
              {' '}
              <span className="font-medium">{event.entityName}</span>
            </>
          )}
        </p>
        <p className="text-xs text-muted-foreground mt-0.5">{ago}</p>
      </div>
    </div>
  );
}
