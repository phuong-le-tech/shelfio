import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Settings, LogOut, Trash2, ChevronUp, Crown } from "lucide-react";
import { useAuth } from "../contexts/AuthContext";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import ConfirmModal from "./ConfirmModal";

export function UserMenu() {
  const { user, logout, deleteAccount, isAdmin, isPremium } = useAuth();
  const navigate = useNavigate();
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const handleDeleteAccount = async () => {
    await deleteAccount();
    navigate("/login");
  };

  if (!user) return null;

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-left transition-colors hover:bg-secondary">
            <Avatar className="h-9 w-9">
              {user.pictureUrl && <AvatarImage src={user.pictureUrl} alt="" />}
              <AvatarFallback className="bg-brand text-foreground text-sm font-medium">
                {user.email[0].toUpperCase()}
              </AvatarFallback>
            </Avatar>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium truncate">{user.email}</p>
              <p className="text-xs text-muted-foreground capitalize flex items-center gap-1">
                {user.role === 'PREMIUM_USER' ? (
                  <><Crown className="w-3 h-3" /> Premium</>
                ) : (
                  user.role.toLowerCase()
                )}
              </p>
            </div>
            <ChevronUp className="w-4 h-4 text-muted-foreground" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent side="top" align="start" className="w-56">
          {!isPremium && (
            <>
              <DropdownMenuItem onClick={() => navigate("/upgrade")}>
                <Crown className="w-4 h-4 mr-2" />
                Passer en Premium
              </DropdownMenuItem>
              <DropdownMenuSeparator />
            </>
          )}
          {isAdmin && (
            <>
              <DropdownMenuItem onClick={() => navigate("/admin/users")}>
                <Settings className="w-4 h-4 mr-2" />
                Gestion des utilisateurs
              </DropdownMenuItem>
              <DropdownMenuSeparator />
            </>
          )}
          <DropdownMenuItem
            onClick={() => setShowDeleteModal(true)}
            className="text-destructive focus:text-destructive"
          >
            <Trash2 className="w-4 h-4 mr-2" />
            Supprimer mon compte
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={handleLogout}>
            <LogOut className="w-4 h-4 mr-2" />
            Se deconnecter
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <ConfirmModal
        isOpen={showDeleteModal}
        title="Supprimer votre compte"
        message="Cette action est irreversible. Toutes vos listes et vos objets seront definitivement supprimes."
        confirmLabel="Supprimer definitivement"
        onConfirm={handleDeleteAccount}
        onCancel={() => setShowDeleteModal(false)}
      />
    </>
  );
}
