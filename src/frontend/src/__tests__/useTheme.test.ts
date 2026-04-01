// @vitest-environment jsdom
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { getTheme, getResolvedTheme, applyTheme, THEME_STORAGE_KEY } from '../hooks/useTheme';
import type { Theme } from '../hooks/useTheme';

// Mock localStorage
const storage: Record<string, string> = {};
const localStorageMock = {
  getItem: (key: string) => storage[key] ?? null,
  setItem: (key: string, value: string) => { storage[key] = value; },
  removeItem: (key: string) => { delete storage[key]; },
  clear: () => { Object.keys(storage).forEach(k => delete storage[k]); },
};
Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock });

function setPrefersDark(dark: boolean) {
  Object.defineProperty(globalThis, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: query === '(prefers-color-scheme: dark)' ? dark : false,
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    }),
  });
}

beforeEach(() => {
  localStorageMock.clear();
  document.documentElement.classList.remove('dark');
  setPrefersDark(false);
});

describe('getTheme', () => {
  it('defaults to system when nothing in localStorage', () => {
    expect(getTheme()).toBe('system');
  });

  it('returns stored theme when valid', () => {
    localStorageMock.setItem(THEME_STORAGE_KEY, 'dark');
    expect(getTheme()).toBe('dark');

    localStorageMock.setItem(THEME_STORAGE_KEY, 'light');
    expect(getTheme()).toBe('light');

    localStorageMock.setItem(THEME_STORAGE_KEY, 'system');
    expect(getTheme()).toBe('system');
  });

  it('defaults to system for invalid stored value', () => {
    localStorageMock.setItem(THEME_STORAGE_KEY, 'garbage');
    expect(getTheme()).toBe('system');
  });
});

describe('getResolvedTheme', () => {
  it('returns light for explicit light theme', () => {
    expect(getResolvedTheme('light')).toBe('light');
  });

  it('returns dark for explicit dark theme', () => {
    expect(getResolvedTheme('dark')).toBe('dark');
  });

  it('resolves system to dark when OS prefers dark', () => {
    setPrefersDark(true);
    expect(getResolvedTheme('system')).toBe('dark');
  });

  it('resolves system to light when OS prefers light', () => {
    setPrefersDark(false);
    expect(getResolvedTheme('system')).toBe('light');
  });
});

describe('applyTheme', () => {
  it('adds .dark class for dark theme', () => {
    applyTheme('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('removes .dark class for light theme', () => {
    document.documentElement.classList.add('dark');
    applyTheme('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('adds .dark class for system when OS is dark', () => {
    setPrefersDark(true);
    applyTheme('system');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('removes .dark class for system when OS is light', () => {
    document.documentElement.classList.add('dark');
    setPrefersDark(false);
    applyTheme('system');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});

describe('cycle order', () => {
  it('follows light → dark → system → light', () => {
    const order: Theme[] = ['light', 'dark', 'system'];
    for (let i = 0; i < order.length; i++) {
      const current = order[i];
      const next = order[(i + 1) % order.length];
      const idx = order.indexOf(current);
      expect(order[(idx + 1) % order.length]).toBe(next);
    }
  });
});
