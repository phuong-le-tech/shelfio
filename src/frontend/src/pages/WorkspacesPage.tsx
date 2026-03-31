import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Activity, Plus, Settings, Trash2, Users } from 'lucide-react';
import { useWorkspace } from '../contexts/WorkspaceContext';
import { useAuth } from '../contexts/AuthContext';
import { workspaceApi } from '../services/workspaceApi';
import { WORKSPACE_ROLE_LABELS } from '../types/workspace';
import { useToast } from '../components/Toast';
import ConfirmModal from '../components/ConfirmModal';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { BlurFade } from '@/components/effects/blur-fade';
import { Breadcrumb } from '../components/Breadcrumb';

export default function WorkspacesPage() {
  const { workspaces, refreshWorkspaces, loading } = useWorkspace();
  const { isPremium } = useAuth();
  const { showToast } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [creating, setCreating] = useState(false);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

  // Handle ?action=create from WorkspaceSwitcher link
  useEffect(() => {
    if (searchParams.get('action') === 'create' && isPremium) {
      setShowCreate(true);
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, isPremium, setSearchParams]);

  if (loading) {
    return (
      <div>
        <Breadcrumb items={[{ label: 'Espaces de travail' }]} />
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="p-4 border rounded-lg bg-card animate-pulse h-32" />
          ))}
        </div>
      </div>
    );
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;
    setCreating(true);
    try {
      await workspaceApi.create({ name: newName.trim() });
      await refreshWorkspaces();
      setNewName('');
      setShowCreate(false);
      showToast('Espace de travail créé', 'success');
    } catch {
      showToast("Erreur lors de la création", 'error');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async () => {
    if (!pendingDeleteId) return;
    try {
      await workspaceApi.delete(pendingDeleteId);
      await refreshWorkspaces();
      showToast('Espace de travail supprimé', 'success');
    } catch {
      showToast('Erreur lors de la suppression', 'error');
    } finally {
      setPendingDeleteId(null);
    }
  };

  return (
    <>
      <Breadcrumb items={[{ label: 'Espaces de travail' }]} />

      <div className="flex items-center justify-between mb-6">
        <BlurFade delay={0.1}>
          <h1 className="text-2xl font-bold">Espaces de travail</h1>
        </BlurFade>
        {isPremium && (
          <BlurFade delay={0.2}>
            <Button onClick={() => setShowCreate(!showCreate)}>
              <Plus className="h-4 w-4 mr-2" />
              Nouvel espace
            </Button>
          </BlurFade>
        )}
      </div>

      {showCreate && (
        <BlurFade delay={0.1}>
          <form onSubmit={handleCreate} className="mb-6 p-4 border rounded-lg bg-card">
            <Label htmlFor="workspace-name">Nom de l'espace</Label>
            <div className="flex gap-2 mt-1">
              <Input
                id="workspace-name"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="Ex: Mon équipe"
                maxLength={100}
              />
              <Button type="submit" disabled={creating || !newName.trim()}>
                Créer
              </Button>
              <Button type="button" variant="outline" onClick={() => setShowCreate(false)}>
                Annuler
              </Button>
            </div>
          </form>
        </BlurFade>
      )}

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {workspaces.map((workspace, i) => (
          <BlurFade key={workspace.id} delay={0.1 + i * 0.05}>
            <div className="p-4 border rounded-lg bg-card hover:shadow-sm transition-shadow">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="font-semibold">{workspace.name}</h3>
                  <Badge variant="outline" className="mt-1 text-[11px]">
                    {WORKSPACE_ROLE_LABELS[workspace.role]}
                  </Badge>
                </div>
                {workspace.role === 'OWNER' && !workspace.isDefault && (
                  <button
                    onClick={() => setPendingDeleteId(workspace.id)}
                    className="p-1.5 text-muted-foreground hover:text-destructive transition-colors"
                    title="Supprimer"
                    aria-label="Supprimer l'espace de travail"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                )}
              </div>
              <div className="flex items-center gap-4 text-sm text-muted-foreground">
                <span className="flex items-center gap-1">
                  <Users className="h-3.5 w-3.5" />
                  {workspace.memberCount} membre{workspace.memberCount > 1 ? 's' : ''}
                </span>
                <span>{workspace.listCount} liste{workspace.listCount > 1 ? 's' : ''}</span>
              </div>
              <div className="flex items-center gap-3 mt-3">
                <Link
                  to={`/workspaces/${workspace.id}/activity`}
                  className="flex items-center gap-1 text-sm text-muted-foreground hover:underline"
                >
                  <Activity className="h-3.5 w-3.5" />
                  Activité
                </Link>
                {workspace.role === 'OWNER' && (
                  <Link
                    to={`/workspaces/${workspace.id}/settings`}
                    className="flex items-center gap-1 text-sm text-brand hover:underline"
                  >
                    <Settings className="h-3.5 w-3.5" />
                    Gérer
                  </Link>
                )}
              </div>
            </div>
          </BlurFade>
        ))}
      </div>

      <ConfirmModal
        isOpen={!!pendingDeleteId}
        onCancel={() => setPendingDeleteId(null)}
        onConfirm={handleDelete}
        title="Supprimer l'espace de travail"
        message="Toutes les listes et articles de cet espace seront supprimés définitivement."
        confirmLabel="Supprimer"
      />
    </>
  );
}
