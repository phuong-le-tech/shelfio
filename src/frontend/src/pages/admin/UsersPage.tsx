import { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Plus, Trash2, Shield, User as UserIcon, AlertCircle } from 'lucide-react';
import { motion } from 'motion/react';
import { createUserSchema, CreateUserFormData } from '../../schemas/auth.schemas';
import { User } from '../../types/auth';
import { adminApi } from '../../services/authApi';
import { useToast } from '../../components/Toast';
import ConfirmModal from '../../components/ConfirmModal';
import { getApiErrorStatus } from '../../utils/errorUtils';
import { Button } from '@/components/ui/button';
import { Pagination } from '@/components/Pagination';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { BlurFade } from '@/components/effects/blur-fade';

const PASTEL_COLORS = [
  'bg-brand',
  'bg-status-verify',
  'bg-status-pending',
  'bg-status-ready',
  'bg-status-prepare',
];

function getAvatarColor(email: string): string {
  let hash = 0;
  for (let i = 0; i < email.length; i++) {
    hash = email.charCodeAt(i) + ((hash << 5) - hash);
  }
  return PASTEL_COLORS[Math.abs(hash) % PASTEL_COLORS.length];
}

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
      showToast('Echec du chargement des utilisateurs', 'error');
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
      showToast('Utilisateur supprime', 'success');
      if (users.length === 1 && page > 0) {
        setPage(p => p - 1);
      } else {
        loadUsers();
      }
    } catch {
      showToast("Echec de la suppression de l'utilisateur", 'error');
    }
  };

  const handleToggleRole = async (user: User) => {
    const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    try {
      const updated = await adminApi.updateUserRole(user.id, newRole);
      setUsers(users.map(u => u.id === user.id ? updated : u));
      const label = newRole === 'ADMIN' ? 'Administrateur' : 'Utilisateur';
      showToast(`Role modifie en ${label}`, 'success');
    } catch {
      showToast('Echec de la mise a jour du role', 'error');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-foreground"></div>
      </div>
    );
  }

  return (
    <TooltipProvider>
      <div className="animate-fade-in">
        <div className="flex items-center justify-between mb-8">
          <div>
            <BlurFade>
              <h1 className="font-display text-4xl font-semibold tracking-tight mb-2">Utilisateurs</h1>
            </BlurFade>
            <BlurFade delay={0.1}>
              <p className="text-muted-foreground">
                Gerer les comptes utilisateurs et les permissions
                {totalElements > 0 && ` -- ${totalElements} au total`}
              </p>
            </BlurFade>
          </div>
          <Button onClick={() => setShowCreateModal(true)}>
            <Plus className="w-4 h-4 mr-2" />
            Ajouter un utilisateur
          </Button>
        </div>

        <div className="rounded-xl border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Utilisateur</TableHead>
                <TableHead>Role</TableHead>
                <TableHead>Cree le</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.map((user, index) => (
                <motion.tr
                  key={user.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05, duration: 0.3 }}
                  className="border-b transition-colors hover:bg-muted/50"
                >
                  <TableCell className="py-5">
                    <div className="flex items-center gap-3">
                      <Avatar className="h-10 w-10">
                        {user.pictureUrl && <AvatarImage src={user.pictureUrl} alt="" />}
                        <AvatarFallback className={`${getAvatarColor(user.email)} text-foreground text-sm font-medium`}>
                          {user.email[0].toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      <span className="font-medium">{user.email}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant={user.role === 'ADMIN' ? 'default' : 'secondary'}>
                      {user.role === 'ADMIN' ? (
                        <><Shield className="w-3 h-3 mr-1" />Administrateur</>
                      ) : (
                        <><UserIcon className="w-3 h-3 mr-1" />Utilisateur</>
                      )}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {new Date(user.createdAt).toLocaleDateString('fr-FR')}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center justify-end gap-1">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => handleToggleRole(user)}
                          >
                            <Shield className="w-4 h-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                          {user.role === 'ADMIN' ? 'Retrograder en utilisateur' : 'Promouvoir en administrateur'}
                        </TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-muted-foreground hover:text-destructive"
                            onClick={() => setPendingDeleteId(user.id)}
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>Supprimer l'utilisateur</TooltipContent>
                      </Tooltip>
                    </div>
                  </TableCell>
                </motion.tr>
              ))}
            </TableBody>
          </Table>

          {users.length === 0 && (
            <div className="text-center py-12 text-muted-foreground">
              Aucun utilisateur trouve
            </div>
          )}
        </div>

        <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />

        <ConfirmModal
          isOpen={pendingDeleteId !== null}
          title="Supprimer l'utilisateur"
          message="Etes-vous sur de vouloir supprimer cet utilisateur ? Cette action est irreversible."
          confirmLabel="Supprimer"
          onConfirm={handleDeleteConfirm}
          onCancel={() => setPendingDeleteId(null)}
        />

        <CreateUserModal
          open={showCreateModal}
          onClose={() => setShowCreateModal(false)}
          onCreated={(user) => {
            setUsers(prev => [...prev, user]);
            setShowCreateModal(false);
            showToast('Utilisateur cree avec succes', 'success');
          }}
        />
      </div>
    </TooltipProvider>
  );
}

function CreateUserModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: (user: User) => void }) {
  const [loading, setLoading] = useState(false);
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<CreateUserFormData>({
    resolver: zodResolver(createUserSchema),
    defaultValues: { role: 'USER' },
  });

  useEffect(() => {
    if (open) {
      reset({ email: '', password: '', role: 'USER' });
      setServerError('');
    }
  }, [open, reset]);

  const onSubmit = async (data: CreateUserFormData) => {
    setLoading(true);
    setServerError('');
    try {
      const user = await adminApi.createUser(data);
      onCreated(user);
    } catch (err: unknown) {
      const status = getApiErrorStatus(err);
      setServerError(
        status === 409
          ? 'Un utilisateur avec cet email existe deja'
          : "Echec de la creation de l'utilisateur"
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Creer un utilisateur</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {serverError && (
            <div className="bg-destructive/10 border border-destructive/20 rounded-lg px-4 py-3 text-destructive text-sm">
              {serverError}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="create-user-email">Email</Label>
            <Input
              id="create-user-email"
              type="email"
              {...register('email')}
              className={errors.email ? 'border-destructive' : ''}
              placeholder="utilisateur@exemple.com"
            />
            {errors.email && (
              <p className="text-sm text-destructive flex items-center gap-1.5">
                <AlertCircle className="h-4 w-4 flex-shrink-0" />
                {errors.email.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="create-user-password">Mot de passe</Label>
            <Input
              id="create-user-password"
              type="password"
              {...register('password')}
              className={errors.password ? 'border-destructive' : ''}
              placeholder="Minimum 6 caracteres"
            />
            {errors.password && (
              <p className="text-sm text-destructive flex items-center gap-1.5">
                <AlertCircle className="h-4 w-4 flex-shrink-0" />
                {errors.password.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="create-user-role">Role</Label>
            <select
              id="create-user-role"
              {...register('role')}
              className="flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <option value="USER">Utilisateur</option>
              <option value="ADMIN">Administrateur</option>
            </select>
          </div>

          <DialogFooter className="gap-2 sm:gap-0 pt-4">
            <Button type="button" variant="outline" onClick={onClose}>
              Annuler
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? 'Creation...' : 'Creer'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
