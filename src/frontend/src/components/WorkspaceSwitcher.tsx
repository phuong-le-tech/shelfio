import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { ChevronsUpDown, Plus, Settings, Check } from "lucide-react";
import { useWorkspace } from "../contexts/WorkspaceContext";
import { useAuth } from "../contexts/AuthContext";
import { WORKSPACE_ROLE_LABELS } from "../types/workspace";
import { cn } from "@/lib/utils";

export function WorkspaceSwitcher() {
  const { workspaces, currentWorkspace, setCurrentWorkspace } = useWorkspace();
  const { isPremium } = useAuth();
  const [open, setOpen] = useState(false);
  // Close on Escape key
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open]);

  if (!currentWorkspace) return null;

  return (
    <div className="relative px-3 pb-2">
      <div className="px-3 pb-1.5">
        <span className="text-[11px] font-semibold tracking-[0.5px] text-[hsl(var(--text-tertiary))]">
          Workspace
        </span>
      </div>
      <button
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        aria-haspopup="listbox"
        className="flex items-center justify-between w-full px-3 py-2 text-sm font-medium rounded-[10px] bg-muted/50 hover:bg-muted transition-colors"
      >
        <span className="truncate">{currentWorkspace.name}</span>
        <ChevronsUpDown className="h-4 w-4 text-muted-foreground flex-shrink-0" />
      </button>

      {open && (
        <>
          <div
            role="presentation"
            className="fixed inset-0 z-40"
            onClick={() => setOpen(false)}
            onKeyDown={(e) => {
              if (e.key === "Escape") setOpen(false);
            }}
          />
          <div className="absolute left-3 right-3 top-full mt-1 z-50 bg-popover border rounded-lg shadow-lg overflow-hidden">
            <div
              role="listbox"
              aria-label="Espaces de travail"
              className="p-1 max-h-64 overflow-y-auto"
            >
              {workspaces.map((workspace) => (
                <button
                  key={workspace.id}
                  role="option"
                  aria-selected={workspace.id === currentWorkspace.id}
                  onClick={() => {
                    setCurrentWorkspace(workspace.id);
                    setOpen(false);
                  }}
                  className={cn(
                    "flex items-center justify-between w-full px-3 py-2 text-sm rounded-md transition-colors",
                    workspace.id === currentWorkspace.id
                      ? "bg-accent text-accent-foreground"
                      : "hover:bg-muted/50",
                  )}
                >
                  <div className="flex flex-col items-start min-w-0">
                    <span className="truncate w-full text-left">
                      {workspace.name}
                    </span>
                    <span className="text-[11px] text-muted-foreground">
                      {WORKSPACE_ROLE_LABELS[workspace.role]}
                    </span>
                  </div>
                  {workspace.id === currentWorkspace.id && (
                    <Check className="h-4 w-4 flex-shrink-0 text-brand" />
                  )}
                </button>
              ))}
            </div>
            <div className="border-t p-1">
              {isPremium && (
                <Link
                  to="/workspaces?action=create"
                  onClick={() => setOpen(false)}
                  className="flex items-center gap-2 w-full px-3 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-md transition-colors"
                >
                  <Plus className="h-4 w-4" />
                  Nouvel espace
                </Link>
              )}
              <Link
                to="/workspaces"
                onClick={() => setOpen(false)}
                className="flex items-center gap-2 w-full px-3 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-md transition-colors"
              >
                <Settings className="h-4 w-4" />
                Gérer les espaces
              </Link>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
