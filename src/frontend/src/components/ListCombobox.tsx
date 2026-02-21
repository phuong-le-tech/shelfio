import { useState, useEffect, useRef, useCallback, useId } from 'react';
import { ChevronDown, Search, X } from 'lucide-react';
import { listsApi } from '../services/api';
import { ItemList } from '../types/item';

interface ListComboboxProps {
  value: string;
  onChange: (id: string) => void;
  error?: boolean;
  disabled?: boolean;
}

const DEBOUNCE_MS = 300;
const RESULTS_SIZE = 10;

export default function ListCombobox({ value, onChange, error = false, disabled = false }: ListComboboxProps) {
  const [inputValue, setInputValue] = useState('');
  const [options, setOptions] = useState<ItemList[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [searching, setSearching] = useState(false);

  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listboxId = useId();

  // Load initial display name when value is set (edit mode)
  useEffect(() => {
    if (!value) {
      setInputValue('');
      return;
    }
    const controller = new AbortController();
    listsApi.getById(value, controller.signal)
      .then((list) => {
        if (!controller.signal.aborted) {
          setInputValue(list.name);
        }
      })
      .catch(() => {
        // If we can't load the list, clear the input
        if (!controller.signal.aborted) setInputValue('');
      });
    return () => controller.abort();
  }, [value]);

  // Debounced search
  useEffect(() => {
    if (!isOpen) return;
    const controller = new AbortController();
    setSearching(true);

    const timer = setTimeout(async () => {
      try {
        const response = await listsApi.getAll({ search: inputValue, size: RESULTS_SIZE }, controller.signal);
        if (!controller.signal.aborted) {
          setOptions(response.content);
          setActiveIndex(-1);
        }
      } catch {
        if (!controller.signal.aborted) setOptions([]);
      } finally {
        if (!controller.signal.aborted) setSearching(false);
      }
    }, DEBOUNCE_MS);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [inputValue, isOpen]);

  // Close on click outside
  useEffect(() => {
    const handleMouseDown = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleMouseDown);
    return () => document.removeEventListener('mousedown', handleMouseDown);
  }, []);

  const selectOption = useCallback((list: ItemList) => {
    onChange(list.id);
    setInputValue(list.name);
    setIsOpen(false);
    setActiveIndex(-1);
  }, [onChange]);

  const clearSelection = useCallback(() => {
    onChange('');
    setInputValue('');
    setOptions([]);
    setIsOpen(false);
    inputRef.current?.focus();
  }, [onChange]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);
    // Clear the RHF value when user types — they need to re-select
    if (value) onChange('');
    setIsOpen(true);
  };

  const handleInputFocus = () => {
    setIsOpen(true);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setActiveIndex(i => Math.min(i + 1, options.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setActiveIndex(i => Math.max(i - 1, -1));
        break;
      case 'Enter':
        e.preventDefault();
        if (activeIndex >= 0 && options[activeIndex]) {
          selectOption(options[activeIndex]);
        }
        break;
      case 'Escape':
        setIsOpen(false);
        setActiveIndex(-1);
        break;
      case 'Tab':
        setIsOpen(false);
        break;
    }
  };

  const borderClass = error
    ? 'border-red-400/40 focus-within:ring-red-400/30 focus-within:border-red-400/50'
    : 'border-white/[0.08] focus-within:ring-amber-500/30 focus-within:border-amber-500/40';

  return (
    <div ref={containerRef} className="relative">
      <div
        className={`flex items-center w-full px-4 py-3 bg-surface-base/50 border rounded-xl text-stone-100 transition-all duration-200 focus-within:outline-none focus-within:ring-2 hover:border-white/[0.12] ${borderClass} ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
      >
        <Search className="h-4 w-4 text-stone-500 mr-2 flex-shrink-0" />
        <input
          ref={inputRef}
          type="text"
          role="combobox"
          aria-expanded={isOpen}
          aria-autocomplete="list"
          aria-controls={listboxId}
          aria-activedescendant={activeIndex >= 0 ? `${listboxId}-option-${activeIndex}` : undefined}
          value={inputValue}
          onChange={handleInputChange}
          onFocus={handleInputFocus}
          onKeyDown={handleKeyDown}
          placeholder="Rechercher une liste…"
          disabled={disabled}
          className="flex-1 bg-transparent outline-none placeholder-stone-500 min-w-0"
        />
        {value && !disabled && (
          <button
            type="button"
            onClick={clearSelection}
            aria-label="Effacer la sélection"
            className="ml-2 text-stone-500 hover:text-stone-300 transition-colors flex-shrink-0"
          >
            <X className="h-4 w-4" />
          </button>
        )}
        {!value && (
          <ChevronDown className={`h-4 w-4 text-stone-500 flex-shrink-0 ml-2 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
        )}
      </div>

      {isOpen && (
        <ul
          id={listboxId}
          role="listbox"
          className="absolute z-50 w-full mt-1 bg-surface-card border border-white/[0.08] rounded-xl shadow-premium-hover overflow-hidden"
        >
          {searching ? (
            <li className="px-4 py-3 text-stone-500 text-sm">Recherche…</li>
          ) : options.length === 0 ? (
            <li className="px-4 py-3 text-stone-500 text-sm">Aucune liste trouvée</li>
          ) : (
            options.map((list, index) => (
              <li
                key={list.id}
                id={`${listboxId}-option-${index}`}
                role="option"
                aria-selected={list.id === value}
                onMouseDown={(e) => {
                  e.preventDefault(); // prevent blur before click registers
                  selectOption(list);
                }}
                className={`px-4 py-3 cursor-pointer transition-colors duration-150 flex items-center justify-between ${
                  index === activeIndex
                    ? 'bg-amber-500/10 text-amber-400'
                    : list.id === value
                    ? 'bg-white/[0.04] text-stone-100'
                    : 'text-stone-300 hover:bg-white/[0.04] hover:text-stone-100'
                }`}
              >
                <span className="font-medium">{list.name}</span>
                {list.category && (
                  <span className="text-xs text-stone-500 ml-2 flex-shrink-0">{list.category}</span>
                )}
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  );
}
