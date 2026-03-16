import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  List,
  Package,
  Menu,
  X,
  Shield,
  Layers,
  BarChart3,
  Settings,
  Crown,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import FocusTrap from 'focus-trap-react';
import { UserMenu } from './UserMenu';
import { useAuth } from '../contexts/AuthContext';
import { cn } from '@/lib/utils';

interface LayoutProps {
  children: React.ReactNode;
}

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
}

const mainNavItems: NavItem[] = [
  { to: '/dashboard', label: 'Tableau de bord', icon: LayoutDashboard },
  { to: '/lists', label: 'Mes Listes', icon: List },
  { to: '/settings', label: 'Paramètres', icon: Settings },
];

const adminNavItems: NavItem[] = [
  { to: '/admin/stats', label: 'Statistiques', icon: BarChart3 },
  { to: '/admin/users', label: 'Utilisateurs', icon: Shield },
  { to: '/admin/lists', label: 'Contenu', icon: Layers },
];

export default function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const { isAdmin, isPremium, user } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!sidebarOpen) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') setSidebarOpen(false);
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [sidebarOpen]);

  const renderNavItem = (item: NavItem) => {
    const isActive =
      location.pathname === item.to ||
      (item.to !== '/' && location.pathname.startsWith(item.to + '/'));
    return (
      <Link
        key={item.to}
        to={item.to}
        className={cn(
          'flex items-center gap-2.5 px-3 py-2.5 text-sm font-medium rounded-[10px] transition-all duration-200',
          isActive
            ? 'text-white bg-brand'
            : 'text-muted-foreground hover:text-foreground hover:bg-muted/50'
        )}
      >
        <item.icon className="h-[18px] w-[18px] flex-shrink-0" />
        {item.label}
      </Link>
    );
  };

  return (
    <div className="min-h-screen flex flex-col md:flex-row bg-background">
      {/* Mobile header */}
      <div className="md:hidden sticky top-0 z-40 flex items-center gap-3 px-4 py-3 bg-background/95 backdrop-blur-sm border-b">
        <button
          onClick={() => setSidebarOpen(true)}
          aria-label="Ouvrir le menu"
          className="p-2 -ml-2 text-muted-foreground hover:text-foreground transition-colors"
        >
          <Menu className="h-5 w-5" />
        </button>
        <div className="w-6 h-6 bg-brand rounded-md flex items-center justify-center">
          <Package className="h-3.5 w-3.5 text-white" />
        </div>
        <span className="font-display text-lg font-bold tracking-tight">
          Shelfio
        </span>
      </div>

      {/* Backdrop overlay (mobile) */}
      {sidebarOpen && (
        <div
          className="md:hidden fixed inset-0 z-40 bg-black/20 backdrop-blur-sm"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Sidebar */}
      <FocusTrap
        active={sidebarOpen}
        focusTrapOptions={{
          clickOutsideDeactivates: true,
          escapeDeactivates: true,
          allowOutsideClick: true,
          fallbackFocus: () =>
            document.querySelector(
              '[aria-label="Navigation principale"]'
            ) as HTMLElement,
        }}
      >
        <aside
          className={cn(
            'w-[240px] bg-sidebar border-r fixed h-full flex flex-col z-50 transition-transform duration-300 ease-in-out',
            sidebarOpen ? 'translate-x-0' : '-translate-x-full',
            'md:translate-x-0 md:static md:z-auto md:h-screen md:sticky md:top-0'
          )}
        >
          {/* Logo */}
          <div className="py-5 px-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div
                  className="w-8 h-8 bg-brand rounded-lg flex items-center justify-center"
                  aria-label="Logo Shelfio"
                >
                  <Package
                    className="h-4 w-4 text-white"
                    aria-hidden="true"
                  />
                </div>
                <span className="font-display text-[22px] font-bold tracking-tight">
                  Shelfio
                </span>
              </div>
              <button
                onClick={() => setSidebarOpen(false)}
                aria-label="Fermer le menu"
                className="md:hidden p-1 text-muted-foreground hover:text-foreground transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>

          {/* Main navigation */}
          <nav
            aria-label="Navigation principale"
            className="py-2 px-3 space-y-0.5"
          >
            {mainNavItems.map(renderNavItem)}
          </nav>

          {/* Admin section */}
          {isAdmin && (
            <>
              <div className="pt-4 px-6 pb-2">
                <div className="h-px bg-border" />
              </div>
              <div className="px-6 pb-1">
                <span className="text-[11px] font-semibold tracking-[0.5px] text-[hsl(var(--text-tertiary))]">
                  ADMINISTRATION
                </span>
              </div>
              <nav aria-label="Navigation admin" className="px-3 space-y-0.5">
                {adminNavItems.map(renderNavItem)}
              </nav>
            </>
          )}

          {/* Spacer */}
          <div className="flex-1" />

          {/* Bottom area */}
          <div className="px-3 pb-4 space-y-3">
            {!isPremium && user?.role !== 'ADMIN' && (
              <Link
                to="/upgrade"
                className="flex items-center justify-center gap-2 w-full py-2.5 px-3 text-[13px] font-semibold text-brand bg-brand-light rounded-[10px] hover:opacity-90 transition-opacity"
              >
                <Crown className="h-4 w-4" />
                Passer à Premium
              </Link>
            )}
            <div className="h-px bg-border" />
            <UserMenu />
          </div>
        </aside>
      </FocusTrap>

      <div className="flex-1 flex flex-col">
        <main className="flex-1 p-4 md:p-8 lg:px-10">{children}</main>
        <footer className="border-t px-4 py-4 md:px-8">
          <nav
            aria-label="Liens légaux"
            className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground"
          >
            <Link
              to="/mentions-legales"
              className="hover:text-foreground transition-colors"
            >
              Mentions légales
            </Link>
            <Link
              to="/privacy"
              className="hover:text-foreground transition-colors"
            >
              Politique de confidentialité
            </Link>
            <Link
              to="/terms"
              className="hover:text-foreground transition-colors"
            >
              Conditions d'utilisation
            </Link>
          </nav>
        </footer>
      </div>
    </div>
  );
}
