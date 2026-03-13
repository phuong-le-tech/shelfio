import { useNavigate } from "react-router-dom";
import { Settings, LogOut, ChevronUp, Crown } from "lucide-react";
import { useAuth } from "../contexts/AuthContext";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";

export function UserMenu() {
  const { user, logout, isAdmin, isPremium } = useAuth();
  const navigate = useNavigate();
  const handleLogout = async () => {
    await logout();
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
          <DropdownMenuItem onClick={() => navigate("/settings")}>
            <Settings className="w-4 h-4 mr-2" />
            Paramètres
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={handleLogout}>
            <LogOut className="w-4 h-4 mr-2" />
            Se déconnecter
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

    </>
  );
}
