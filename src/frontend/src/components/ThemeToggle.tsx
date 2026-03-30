import { Sun, Moon, Monitor } from 'lucide-react';
import { useTheme } from '../hooks/useTheme';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from './ui/tooltip';

const LABELS: Record<string, string> = {
  light: 'Clair',
  dark: 'Sombre',
  system: 'Système',
};

export function ThemeToggle() {
  const { theme, resolvedTheme, cycleTheme } = useTheme();

  const Icon = resolvedTheme === 'dark' ? Moon : theme === 'system' ? Monitor : Sun;

  return (
    <TooltipProvider delayDuration={300}>
      <Tooltip>
        <TooltipTrigger asChild>
          <button
            onClick={cycleTheme}
            aria-label={`Thème : ${LABELS[theme]}`}
            className="p-2 rounded-[10px] text-muted-foreground hover:text-foreground hover:bg-muted/50 transition-all duration-200"
          >
            <Icon className="h-[18px] w-[18px]" />
          </button>
        </TooltipTrigger>
        <TooltipContent side="right">
          {LABELS[theme]}
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}
