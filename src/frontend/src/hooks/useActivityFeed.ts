import { useState, useEffect, useCallback, useRef } from 'react';
import activityApi from '../services/activityApi';
import type { ActivityEvent, ActivityFilters, PageResponse } from '../types/activity';

export function useActivityFeed(workspaceId: string, filters: ActivityFilters) {
  const [data, setData] = useState<PageResponse<ActivityEvent> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const { actorId, entityType, from, to, page, size } = filters;

  const fetchData = useCallback(async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    try {
      const result = await activityApi.getActivity(
        workspaceId,
        { actorId, entityType, from, to, page, size },
        controller.signal,
      );
      setData(result);
      setError(null);
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') return;
      setError('Impossible de charger l\'activité');
    } finally {
      setLoading(false);
    }
  }, [workspaceId, actorId, entityType, from, to, page, size]);

  useEffect(() => {
    setLoading(true);
    fetchData();
    return () => abortRef.current?.abort();
  }, [fetchData]);

  // 30s polling — clears on unmount or filter change
  useEffect(() => {
    const id = setInterval(fetchData, 30_000);
    return () => clearInterval(id);
  }, [fetchData]);

  return { data, loading, error, refresh: fetchData };
}
