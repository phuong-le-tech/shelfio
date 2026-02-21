import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { dashboardApi } from '../services/api';
import { DashboardStats } from '../types/item';
import { useToast } from '../components/Toast';

interface UseDashboardStatsResult {
  stats: DashboardStats | null;
  loading: boolean;
  error: string | null;
  reload: () => void;
}

export function useDashboardStats(): UseDashboardStatsResult {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { showToast } = useToast();

  const load = useCallback(async (signal: AbortSignal) => {
    setLoading(true);
    setError(null);
    try {
      const data = await dashboardApi.getStats(signal);
      if (!signal.aborted) {
        setStats(data);
      }
    } catch (err) {
      if (!axios.isCancel(err) && !signal.aborted) {
        setError('Échec du chargement des statistiques');
        showToast('Échec du chargement des statistiques', 'error');
      }
    } finally {
      if (!signal.aborted) {
        setLoading(false);
      }
    }
  }, [showToast]);

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal);
    return () => controller.abort();
  }, [load]);

  const reload = useCallback(() => {
    const controller = new AbortController();
    load(controller.signal);
  }, [load]);

  return { stats, loading, error, reload };
}
