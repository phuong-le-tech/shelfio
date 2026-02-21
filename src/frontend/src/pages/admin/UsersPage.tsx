import { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Plus, Trash2, Shield, User as UserIcon, ChevronLeft, ChevronRight, AlertCircle } from 'lucide-react';
import { createUserSchema, CreateUserFormData } from '../../schemas/auth.schemas';
import { User } from '../../types/auth';
import { adminApi } from '../../services/authApi';
import { useToast } from '../../components/Toast';
import ConfirmModal from '../../components/ConfirmModal';

export function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const { showToast } = useToast();

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const response = await adminApi.getUsers({ page, size: 20 });
      setUsers(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch {
      showToast('Échec du chargement des utilisateurs', 'error');
    } finally {
      setLoading(false);
    }
  }, [page, showToast]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const handleDeleteConfirm = async () => {
    if (!pendingDeleteId) return;
    const id = pendingDeleteId;
    setPendingDeleteId(null);

    try {
      await adminApi.deleteUser(id);
      setUsers(users.filter(u => u.id !== id));
      showToast('Utilisateur supprimé', 'success');
      // If the page is now empty, go back one page
      if (users.length === 1 && page > 0) {
        setPage(p => p - 1);
      } else {
        loadUsers();
      }
    } catch {
      showToast("Échec de la suppression de l'utilisateur", 'error');
    }
  };

  const handleToggleRole = async (user: User) => {
    const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    try {
      const updated = await adminApi.updateUserRole(user.id, newRole);
      setUsers(users.map(u => u.id === user.id ? updated : u));
      const label = newRole === 'ADMIN' ? 'Administrateur' : 'Utilisateur';
      showToast(`Rôle modifié en ${label}`, 'success');
    } catch {
      showToast('Échec de la mise à jour du rôle', 'error');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-500"></div>
      </div>
    );
  }

  return (
    <div className="animate-fade-in">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-display text-3xl text-stone-100 mb-2">Utilisateurs</h1>
          <p className="text-stone-400">
            Gérer les comptes utilisateurs et les permissions
            {totalElements > 0 && ` — ${totalElements} au total`}
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-white font-medium rounded-lg transition-all shadow-glow-amber"
        >
          <Plus className="w-5 h-5" />
          Ajouter un utilisateur
        </button>
      </div>

      <div className="bg-surface-card rounded-2xl border border-white/5 overflow-hidden shadow-premium">
        <table className="w-full">
          <thead>
            <tr className="border-b border-white/5">
              <th scope="col" className="px-6 py-4 text-left text-xs font-medium text-stone-400 uppercase tracking-wider">Utilisateur</th>
              <th scope="col" className="px-6 py-4 text-left text-xs font-medium text-stone-400 uppercase tracking-wider">Rôle</th>
              <th scope="col" className="px-6 py-4 text-left text-xs font-medium text-stone-400 uppercase tracking-wider">Créé le</th>
              <th scope="col" className="px-6 py-4 text-right text-xs font-medium text-stone-400 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {users.map((user) => (
              <tr key={user.id} className="hover:bg-white/[0.02] transition-colors">
                <td className="px-6 py-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center text-white font-medium">
                      {user.pictureUrl ? (
                        <img src={user.pictureUrl} alt="" className="w-full h-full rounded-full object-cover" />
                      ) : (
                        user.email[0].toUpperCase()
                      )}
                    </div>
                    <span className="text-stone-200">{user.email}</span>
                  </div>
                </td>
                <td className="px-6 py-4">
                  <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                    user.role === 'ADMIN'
                      ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                      : 'bg-stone-500/10 text-stone-400 border border-stone-500/20'
                  }`}>
                    {user.role === 'ADMIN' ? <Shield className="w-3 h-3" /> : <UserIcon className="w-3 h-3" />}
                    {user.role === 'ADMIN' ? 'Administrateur' : 'Utilisateur'}
                  </span>
                </td>
                <td className="px-6 py-4 text-stone-400 text-sm">
                  {new Date(user.createdAt).toLocaleDateString('fr-FR')}
                </td>
                <td className="px-6 py-4">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => handleToggleRole(user)}
                      className="p-2 text-stone-400 hover:text-amber-400 hover:bg-amber-500/10 rounded-lg transition-colors"
                      title={user.role === 'ADMIN' ? 'Rétrograder en utilisateur' : 'Promouvoir en administrateur'}
                    >
                      <Shield className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => setPendingDeleteId(user.id)}
                      className="p-2 text-stone-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
                      title="Supprimer l'utilisateur"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {users.length === 0 && (
          <div className="text-center py-12 text-stone-500">
            Aucun utilisateur trouvé
          </div>
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-4 mt-8">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="inline-flex items-center px-4 py-2 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ChevronLeft className="h-4 w-4 mr-1" />
            Précédent
          </button>
          <span className="text-stone-400">
            Page {page + 1} sur {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="inline-flex items-center px-4 py-2 bg-white/[0.04] border border-white/[0.08] text-stone-300 font-medium rounded-xl transition-all duration-200 hover:bg-white/[0.08] hover:border-white/[0.12] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Suivant
            <ChevronRight className="h-4 w-4 ml-1" />
          </button>
        </div>
      )}

      <ConfirmModal
        isOpen={pendingDeleteId !== null}
        title="Supprimer l'utilisateur"
        message="Êtes-vous sûr de vouloir supprimer cet utilisateur ? Cette action est irréversible."
        confirmLabel="Supprimer"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setPendingDeleteId(null)}
      />

      {showCreateModal && (
        <CreateUserModal
          onClose={() => setShowCreateModal(false)}
          onCreated={(user) => {
            setUsers(prev => [...prev, user]);
            setShowCreateModal(false);
            showToast('Utilisateur créé avec succès', 'success');
          }}
        />
      )}
    </div>
  );
}

function CreateUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: (user: User) => void }) {
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CreateUserFormData>({
    resolver: zodResolver(createUserSchema),
    defaultValues: { role: 'USER' },
  });

  // Close on Escape
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const onSubmit = async (data: CreateUserFormData) => {
    setLoading(true);
    setServerError('');
    try {
      const user = await adminApi.createUser(data);
      onCreated(user);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      setServerError(
        status === 409
          ? 'Un utilisateur avec cet email existe déjà'
          : "Échec de la création de l'utilisateur"
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm animate-fade-in"
      role="dialog"
      aria-modal="true"
      aria-labelledby="create-user-title"
    >
      <div className="bg-surface-card rounded-2xl p-6 w-full max-w-md border border-white/5 shadow-premium-hover">
        <h2 id="create-user-title" className="text-xl font-display text-stone-100 mb-6">Créer un utilisateur</h2>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {serverError && (
            <div className="bg-red-500/10 border border-red-500/20 rounded-lg px-4 py-3 text-red-400 text-sm">
              {serverError}
            </div>
          )}

          <div>
            <label htmlFor="create-user-email" className="block text-sm font-medium text-stone-300 mb-2">Email</label>
            <input
              id="create-user-email"
              type="email"
              {...register('email')}
              className={`w-full px-4 py-2.5 bg-surface-elevated border rounded-lg text-stone-100 placeholder-stone-500 focus:outline-none focus:ring-2 focus:ring-accent/50 ${
                errors.email ? 'border-red-400/40' : 'border-white/10'
              }`}
              placeholder="utilisateur@exemple.com"
            />
            {errors.email && (
              <p className="mt-1.5 text-sm text-red-400 flex items-center gap-1.5">
                <AlertCircle className="h-4 w-4 flex-shrink-0" />
                {errors.email.message}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="create-user-password" className="block text-sm font-medium text-stone-300 mb-2">Mot de passe</label>
            <input
              id="create-user-password"
              type="password"
              {...register('password')}
              className={`w-full px-4 py-2.5 bg-surface-elevated border rounded-lg text-stone-100 placeholder-stone-500 focus:outline-none focus:ring-2 focus:ring-accent/50 ${
                errors.password ? 'border-red-400/40' : 'border-white/10'
              }`}
              placeholder="Minimum 6 caractères"
            />
            {errors.password && (
              <p className="mt-1.5 text-sm text-red-400 flex items-center gap-1.5">
                <AlertCircle className="h-4 w-4 flex-shrink-0" />
                {errors.password.message}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="create-user-role" className="block text-sm font-medium text-stone-300 mb-2">Rôle</label>
            <select
              id="create-user-role"
              {...register('role')}
              className="w-full px-4 py-2.5 bg-surface-elevated border border-white/10 rounded-lg text-stone-100 focus:outline-none focus:ring-2 focus:ring-accent/50"
            >
              <option value="USER">Utilisateur</option>
              <option value="ADMIN">Administrateur</option>
            </select>
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2.5 bg-surface-elevated hover:bg-surface-overlay border border-white/10 text-stone-300 font-medium rounded-lg transition-colors"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-white font-medium rounded-lg transition-all disabled:opacity-50"
            >
              {loading ? 'Création…' : 'Créer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
