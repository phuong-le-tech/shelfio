import { useState, useEffect } from 'react';

export type Theme = 'light' | 'dark' | 'system';

export const THEME_STORAGE_KEY = 'theme';
const CYCLE_ORDER: Theme[] = ['light', 'dark', 'system'];

export function getTheme(): Theme {
  const stored = localStorage.getItem(THEME_STORAGE_KEY);
  if (stored === 'light' || stored === 'dark' || stored === 'system') return stored;
  return 'system';
}

export function getResolvedTheme(theme: Theme): 'light' | 'dark' {
  if (theme !== 'system') return theme;
  return globalThis.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function applyTheme(theme: Theme) {
  const resolved = getResolvedTheme(theme);
  document.documentElement.classList.toggle('dark', resolved === 'dark');
}

function persistTheme(value: Theme) {
  localStorage.setItem(THEME_STORAGE_KEY, value);
  applyTheme(value);
  globalThis.dispatchEvent(new StorageEvent('storage', { key: THEME_STORAGE_KEY, newValue: value }));
}

export function useTheme() {
  const [theme, setTheme] = useState<Theme>(getTheme);

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  // Follow OS preference changes when in system mode
  useEffect(() => {
    if (theme !== 'system') return;
    const mq = globalThis.matchMedia('(prefers-color-scheme: dark)');
    const handler = () => applyTheme('system');
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, [theme]);

  // Sync across tabs
  useEffect(() => {
    function handleStorage(e: StorageEvent) {
      if (e.key === THEME_STORAGE_KEY) {
        const next = getTheme();
        setTheme(next);
        applyTheme(next);
      }
    }
    globalThis.addEventListener('storage', handleStorage);
    return () => globalThis.removeEventListener('storage', handleStorage);
  }, []);

  function cycleTheme() {
    const next = CYCLE_ORDER[(CYCLE_ORDER.indexOf(theme) + 1) % CYCLE_ORDER.length];
    persistTheme(next);
    setTheme(next);
  }

  return { theme, resolvedTheme: getResolvedTheme(theme), cycleTheme };
}
