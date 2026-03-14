import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, List, Package, Menu, X, Shield, Layers, BarChart3, Settings } from 'lucide-react';
import FocusTrap from 'focus-trap-react';
import { UserMenu } from './UserMenu';
import { useAuth } from '../contexts/AuthContext';
import { cn } from '@/lib/utils';

interface LayoutProps {
  children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const { isAdmin } = useAuth();
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

  const navItems = [
    { to: '/dashboard', label: 'Tableau de bord', icon: LayoutDashboard },
    { to: '/lists', label: 'Mes Listes', icon: List },
    { to: '/settings', label: 'Paramètres', icon: Settings },
    ...(isAdmin ? [
      { to: '/admin/stats', label: 'Statistiques', icon: BarChart3 },
      { to: '/admin/users', label: 'Utilisateurs', icon: Shield },
      { to: '/admin/lists', label: 'Contenu', icon: Layers },
    ] : []),
  ];

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
        <Package className="h-5 w-5 text-foreground" />
        <span className="font-display text-lg font-semibold tracking-tight">Inventory</span>
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
          fallbackFocus: () => document.querySelector('[aria-label="Navigation principale"]') as HTMLElement,
        }}
      >
        <aside
          className={cn(
            'w-64 bg-background border-r fixed h-full flex flex-col z-50 transition-transform duration-300 ease-in-out',
            sidebarOpen ? 'translate-x-0' : '-translate-x-full',
            'md:translate-x-0 md:static md:z-auto md:h-screen md:sticky md:top-0'
          )}
        >
          <div className="p-6 border-b">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-brand/10 rounded-xl flex items-center justify-center" aria-label="Logo Inventory">
                  <Package className="h-5 w-5 text-brand" aria-hidden="true" />
                </div>
                <span className="font-display text-xl font-semibold tracking-tight">Inventory</span>
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

          <nav aria-label="Navigation principale" className="p-4 space-y-1 flex-1">
            {navItems.map((item) => {
              const isActive = location.pathname === item.to ||
                (item.to !== '/' && location.pathname.startsWith(item.to + '/'));
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={cn(
                    'group flex items-center px-4 py-3 text-sm font-medium rounded-xl transition-all duration-200',
                    isActive
                      ? 'text-foreground bg-secondary'
                      : 'text-muted-foreground hover:text-foreground hover:bg-secondary/60'
                  )}
                >
                  <item.icon className={cn(
                    'h-5 w-5 mr-3 transition-transform duration-200',
                    !isActive && 'group-hover:scale-110'
                  )} />
                  {item.label}
                  {isActive && (
                    <div className="ml-auto w-1.5 h-1.5 rounded-full bg-foreground" />
                  )}
                </Link>
              );
            })}
          </nav>

          <div className="p-4 border-t">
            <UserMenu />
          </div>
        </aside>
      </FocusTrap>

      <div className="flex-1 flex flex-col">
        <main className="flex-1 p-4 md:p-8 lg:p-12">
          {children}
        </main>
        <footer className="border-t px-4 py-4 md:px-8">
          <nav aria-label="Liens légaux" className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
            <Link to="/mentions-legales" className="hover:text-foreground transition-colors">Mentions légales</Link>
            <Link to="/privacy" className="hover:text-foreground transition-colors">Politique de confidentialité</Link>
            <Link to="/terms" className="hover:text-foreground transition-colors">Conditions d'utilisation</Link>
          </nav>
        </footer>
      </div>
    </div>
  );
}
