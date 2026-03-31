import { useState, useEffect, useCallback } from 'react';
import activityApi from '../services/activityApi';
import type { ActivityEvent, ActivityFilters, PageResponse } from '../types/activity';

export function useActivityFeed(workspaceId: string, filters: ActivityFilters) {
  const [data, setData] = useState<PageResponse<ActivityEvent> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    try {
      const result = await activityApi.getActivity(workspaceId, filters);
      setData(result);
      setError(null);
    } catch {
      setError('Failed to load activity');
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaceId, JSON.stringify(filters)]);

  useEffect(() => {
    setLoading(true);
    fetchData();
  }, [fetchData]);

  // 30s polling — clears on unmount or filter change
  useEffect(() => {
    const id = setInterval(fetchData, 30_000);
    return () => clearInterval(id);
  }, [fetchData]);

  return { data, loading, error, refresh: fetchData };
}
