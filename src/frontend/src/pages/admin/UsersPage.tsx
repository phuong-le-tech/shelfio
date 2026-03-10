import { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, Trash2, Shield, Search, X } from "lucide-react";
import { motion } from "motion/react";
import { User, Role } from "../../types/auth";
import { adminApi } from "../../services/authApi";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "../../lib/queryKeys";
import { useToast } from "../../components/Toast";
import ConfirmModal from "../../components/ConfirmModal";
import { Button } from "@/components/ui/button";
import { Pagination } from "@/components/Pagination";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { BlurFade } from "@/components/effects/blur-fade";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useDebounce } from "../../hooks/useDebounce";
import { getAvatarColor } from "../../utils/avatarUtils";
import { RoleBadge } from "../../components/RoleBadge";
import { CreateUserModal } from "./CreateUserModal";

export function UsersPage() {
  const navigate = useNavigate();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("ALL");
  const [enabledFilter, setEnabledFilter] = useState<string>("ALL");
  const { showToast } = useToast();
  const queryClient = useQueryClient();

  const debouncedSearch = useDebounce(searchInput, 300);

  // Reset page when filters change
  const handleSearchChange = useCallback((value: string) => {
    setSearchInput(value);
    setPage(0);
  }, []);

  const handleRoleFilterChange = useCallback((value: string) => {
    setRoleFilter(value);
    setPage(0);
  }, []);

  const handleEnabledFilterChange = useCallback((value: string) => {
    setEnabledFilter(value);
    setPage(0);
  }, []);

  const hasActiveFilters =
    debouncedSearch || roleFilter !== "ALL" || enabledFilter !== "ALL";

  const clearFilters = () => {
    setSearchInput("");
    setRoleFilter("ALL");
    setEnabledFilter("ALL");
    setPage(0);
  };

  const params = {
    page,
    size: 20,
    ...(debouncedSearch && { search: debouncedSearch }),
    ...(roleFilter !== "ALL" && { role: roleFilter as Role }),
    ...(enabledFilter !== "ALL" && { enabled: enabledFilter === "ENABLED" }),
  };

  const { data, isLoading: loading } = useQuery({
    queryKey: queryKeys.admin.users(params),
    queryFn: () => adminApi.getUsers(params),
  });

  const users = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const handleDeleteConfirm = async () => {
    if (!pendingDeleteId) return;
    const id = pendingDeleteId;
    setPendingDeleteId(null);
    try {
      await adminApi.deleteUser(id);
      showToast("Utilisateur supprimé", "success");
      if (users.length === 1 && page > 0) {
        setPage((p) => p - 1);
      }
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    } catch {
      showToast("Échec de la suppression de l'utilisateur", "error");
    }
  };

  const handleToggleRole = async (user: User) => {
    const newRole = user.role === "ADMIN" ? "USER" : "ADMIN";
    try {
      await adminApi.updateUserRole(user.id, newRole);
      const label = newRole === "ADMIN" ? "Administrateur" : "Utilisateur";
      showToast(`Rôle modifié en ${label}`, "success");
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
    } catch {
      showToast("Échec de la mise à jour du rôle", "error");
    }
  };

  if (loading) {
    return (
      <div className="animate-fade-in">
        <div className="flex items-center justify-between mb-8">
          <div>
            <div className="h-10 w-48 bg-muted rounded-lg animate-pulse mb-2" />
            <div className="h-5 w-72 bg-muted rounded-lg animate-pulse" />
          </div>
          <div className="h-10 w-44 bg-muted rounded-lg animate-pulse" />
        </div>
        <div className="flex gap-3 mb-6">
          <div className="h-10 flex-1 max-w-sm bg-muted rounded-lg animate-pulse" />
          <div className="h-10 w-40 bg-muted rounded-lg animate-pulse" />
          <div className="h-10 w-36 bg-muted rounded-lg animate-pulse" />
        </div>
        <div className="rounded-xl border bg-card">
          <div className="border-b px-6 py-3 flex gap-4">
            <div className="h-4 w-32 bg-muted rounded animate-pulse" />
            <div className="h-4 w-16 bg-muted rounded animate-pulse" />
            <div className="h-4 w-20 bg-muted rounded animate-pulse" />
            <div className="h-4 w-16 bg-muted rounded animate-pulse" />
            <div className="h-4 w-16 bg-muted rounded animate-pulse ml-auto" />
          </div>
          {[...Array(5)].map((_, i) => (
            <div
              key={i}
              className="flex items-center gap-4 px-6 py-5 border-b last:border-0"
            >
              <div className="h-10 w-10 rounded-full bg-muted animate-pulse" />
              <div className="h-4 w-48 bg-muted rounded animate-pulse" />
              <div className="h-6 w-28 bg-muted rounded-full animate-pulse ml-8" />
              <div className="h-4 w-24 bg-muted rounded animate-pulse ml-auto" />
              <div className="flex gap-1 ml-4">
                <div className="h-8 w-8 bg-muted rounded animate-pulse" />
                <div className="h-8 w-8 bg-muted rounded animate-pulse" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <TooltipProvider>
      <div className="max-w-7xl mx-auto animate-fade-in">
        <div className="flex items-center justify-between mb-8">
          <div>
            <BlurFade>
              <h1 className="font-display text-4xl font-semibold tracking-tight mb-2">
                Utilisateurs
              </h1>
            </BlurFade>
            <BlurFade delay={0.1}>
              <p className="text-muted-foreground">
                Gérer les comptes utilisateurs et les permissions
                {totalElements > 0 && ` -- ${totalElements} au total`}
              </p>
            </BlurFade>
          </div>
          <Button onClick={() => setShowCreateModal(true)}>
            <Plus className="w-4 h-4 mr-2" />
            Ajouter un utilisateur
          </Button>
        </div>

        {/* Search and filters */}
        <div className="flex flex-col sm:flex-row gap-3 mb-6">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Rechercher par email..."
              value={searchInput}
              onChange={(e) => handleSearchChange(e.target.value)}
              className="pl-9"
            />
          </div>
          <Select value={roleFilter} onValueChange={handleRoleFilterChange}>
            <SelectTrigger className="w-[160px]">
              <SelectValue placeholder="Rôle" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tous les rôles</SelectItem>
              <SelectItem value="USER">Utilisateur</SelectItem>
              <SelectItem value="PREMIUM_USER">Premium</SelectItem>
              <SelectItem value="ADMIN">Administrateur</SelectItem>
            </SelectContent>
          </Select>
          <Select
            value={enabledFilter}
            onValueChange={handleEnabledFilterChange}
          >
            <SelectTrigger className="w-[140px]">
              <SelectValue placeholder="Statut" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Tous les statuts</SelectItem>
              <SelectItem value="ENABLED">Actif</SelectItem>
              <SelectItem value="DISABLED">Désactivé</SelectItem>
            </SelectContent>
          </Select>
          {hasActiveFilters && (
            <Button
              variant="ghost"
              size="sm"
              onClick={clearFilters}
              className="h-10 px-3"
            >
              <X className="w-4 h-4 mr-1" />
              Effacer
            </Button>
          )}
        </div>

        <div className="rounded-xl border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Utilisateur</TableHead>
                <TableHead>Rôle</TableHead>
                <TableHead>Statut</TableHead>
                <TableHead>Créé le</TableHead>
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
                  className="border-b transition-colors hover:bg-muted/50 cursor-pointer"
                  onClick={() => navigate(`/admin/users/${user.id}`)}
                >
                  <TableCell className="py-5">
                    <div className="flex items-center gap-3">
                      <Avatar className="h-10 w-10">
                        {user.pictureUrl && (
                          <AvatarImage src={user.pictureUrl} alt="" />
                        )}
                        <AvatarFallback
                          className={`${getAvatarColor(user.email)} text-foreground text-sm font-medium`}
                        >
                          {user.email[0].toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      <span className="font-medium">{user.email}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <RoleBadge role={user.role} />
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={user.enabled ? "secondary" : "destructive"}
                      className={
                        user.enabled
                          ? "bg-emerald-500/10 text-emerald-600 hover:bg-emerald-500/20"
                          : ""
                      }
                    >
                      {user.enabled ? "Actif" : "Désactivé"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {new Date(user.createdAt).toLocaleDateString("fr-FR")}
                  </TableCell>
                  <TableCell>
                    <div
                      className="flex items-center justify-end gap-1"
                      onClick={(e) => e.stopPropagation()}
                    >
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
                          {user.role === "ADMIN"
                            ? "Rétrograder en utilisateur"
                            : "Promouvoir en administrateur"}
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
              {hasActiveFilters
                ? "Aucun résultat pour ces filtres"
                : "Aucun utilisateur trouvé"}
            </div>
          )}
        </div>

        <Pagination
          page={page}
          totalPages={totalPages}
          onPageChange={setPage}
        />

        <ConfirmModal
          isOpen={pendingDeleteId !== null}
          title="Supprimer l'utilisateur"
          message="Êtes-vous sûr de vouloir supprimer cet utilisateur ? Cette action est irréversible."
          confirmLabel="Supprimer"
          onConfirm={handleDeleteConfirm}
          onCancel={() => setPendingDeleteId(null)}
        />

        <CreateUserModal
          open={showCreateModal}
          onClose={() => setShowCreateModal(false)}
          onCreated={() => {
            setShowCreateModal(false);
            showToast("Utilisateur créé avec succès", "success");
            queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
          }}
        />
      </div>
    </TooltipProvider>
  );
}
