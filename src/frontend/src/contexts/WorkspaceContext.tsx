import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useRef,
  type ReactNode,
} from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import type { Workspace, WorkspaceInvitation } from '../types/workspace';
import { workspaceApi } from '../services/workspaceApi';
import { useAuth } from './AuthContext';
import { queryKeys } from '../lib/queryKeys';

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
  const queryClient = useQueryClient();
  const [currentWorkspace, setCurrentWorkspaceState] = useState<Workspace | null>(null);

  const {
    data: workspaces = [],
    isLoading,
    error: workspacesError,
  } = useQuery({
    queryKey: queryKeys.workspaces.list(),
    queryFn: ({ signal }) => workspaceApi.getAll(signal),
    enabled: !!user,
  });

  const { data: pendingInvitations = [] } = useQuery({
    queryKey: queryKeys.workspaces.pendingInvitations(),
    queryFn: ({ signal }) => workspaceApi.getPendingInvitations(signal),
    enabled: !!user,
  });

  // Track previous user to detect logout (not initial null)
  const prevUserRef = useRef(user);

  // Single effect: derive currentWorkspace from user + workspaces.
  // Handles both data updates and logout cleanup.
  useEffect(() => {
    const wasLoggedIn = prevUserRef.current !== null;
    prevUserRef.current = user;

    // Logout: clear state only if user was previously logged in
    if (!user) {
      setCurrentWorkspaceState(null);
      if (wasLoggedIn) {
        localStorage.removeItem(STORAGE_KEY);
      }
      return;
    }

    if (workspaces.length === 0) {
      setCurrentWorkspaceState(null);
      return;
    }

    setCurrentWorkspaceState((prev) => {
      // Keep current selection if still valid, update object reference
      if (prev) {
        const updated = workspaces.find((w) => w.id === prev.id);
        if (updated) return updated;
      }
      // Restore from localStorage or pick default
      const savedId = localStorage.getItem(STORAGE_KEY);
      const saved = savedId ? workspaces.find((w) => w.id === savedId) : null;
      return saved || workspaces.find((w) => w.isDefault) || workspaces[0] || null;
    });
  }, [workspaces, user]);

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

  const refreshWorkspaces = useCallback(async () => {
    await queryClient.refetchQueries({ queryKey: queryKeys.workspaces.list() });
  }, [queryClient]);

  const refreshInvitations = useCallback(async () => {
    await queryClient.refetchQueries({ queryKey: queryKeys.workspaces.pendingInvitations() });
  }, [queryClient]);

  const value: WorkspaceContextType = {
    workspaces,
    currentWorkspace,
    setCurrentWorkspace,
    refreshWorkspaces,
    pendingInvitations,
    refreshInvitations,
    loading: isLoading,
    error: workspacesError
      ? 'Impossible de charger les espaces de travail. Veuillez réessayer.'
      : null,
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
