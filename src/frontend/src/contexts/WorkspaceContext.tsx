import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import type { Workspace, WorkspaceInvitation } from '../types/workspace';
import { workspaceApi } from '../services/workspaceApi';
import { useAuth } from './AuthContext';

interface WorkspaceContextType {
  workspaces: Workspace[];
  currentWorkspace: Workspace | null;
  setCurrentWorkspace: (id: string) => void;
  refreshWorkspaces: () => Promise<void>;
  pendingInvitations: WorkspaceInvitation[];
  refreshInvitations: () => Promise<void>;
  loading: boolean;
  error: string | null;
}

const WorkspaceContext = createContext<WorkspaceContextType | undefined>(undefined);

const STORAGE_KEY = 'shelfio_current_workspace';

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [currentWorkspace, setCurrentWorkspaceState] = useState<Workspace | null>(null);
  const [pendingInvitations, setPendingInvitations] = useState<WorkspaceInvitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchWorkspaces = useCallback(async () => {
    if (!user) {
      setWorkspaces([]);
      setCurrentWorkspaceState(null);
      localStorage.removeItem(STORAGE_KEY);
      setLoading(false);
      setError(null);
      return;
    }

    try {
      setError(null);
      const data = await workspaceApi.getAll();
      setWorkspaces(data);

      // Restore last selected workspace from localStorage
      const savedId = localStorage.getItem(STORAGE_KEY);
      const saved = savedId ? data.find((w) => w.id === savedId) : null;
      const defaultWorkspace = saved || data.find((w) => w.isDefault) || data[0] || null;
      setCurrentWorkspaceState(defaultWorkspace);
    } catch (err) {
      console.error('Failed to fetch workspaces:', err);
      setError('Impossible de charger les espaces de travail. Veuillez réessayer.');
      setWorkspaces([]);
      setCurrentWorkspaceState(null);
    } finally {
      setLoading(false);
    }
  }, [user]);

  const fetchInvitations = useCallback(async () => {
    if (!user) {
      setPendingInvitations([]);
      return;
    }
    try {
      const data = await workspaceApi.getPendingInvitations();
      setPendingInvitations(data);
    } catch (err) {
      console.error('Failed to fetch invitations:', err);
      setPendingInvitations([]);
    }
  }, [user]);

  useEffect(() => {
    // TODO: Re-enable when workspace backend is ready
    // fetchWorkspaces();
    // fetchInvitations();
    setLoading(false);
  }, [fetchWorkspaces, fetchInvitations]);

  const setCurrentWorkspace = useCallback(
    (id: string) => {
      const workspace = workspaces.find((w) => w.id === id);
      if (workspace) {
        setCurrentWorkspaceState(workspace);
        localStorage.setItem(STORAGE_KEY, id);
      }
    },
    [workspaces],
  );

  const value: WorkspaceContextType = {
    workspaces,
    currentWorkspace,
    setCurrentWorkspace,
    refreshWorkspaces: fetchWorkspaces,
    pendingInvitations,
    refreshInvitations: fetchInvitations,
    loading,
    error,
  };

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext);
  if (context === undefined) {
    throw new Error('useWorkspace must be used within a WorkspaceProvider');
  }
  return context;
}
