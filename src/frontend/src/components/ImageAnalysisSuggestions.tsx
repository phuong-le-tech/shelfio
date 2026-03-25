import { Check, Loader2, Sparkles, X } from 'lucide-react';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import { ImageAnalysisResult, STATUS_LABELS, ItemStatus } from '../types/item';

interface ImageAnalysisSuggestionsProps {
  suggestions: ImageAnalysisResult | null;
  isAnalyzing: boolean;
  onApply: (field: string, value: unknown) => void;
  onApplyAll: () => void;
  onDismiss: () => void;
}

interface SuggestionEntry {
  field: string;
  label: string;
  value: unknown;
  displayValue: string;
}

function getSuggestionEntries(suggestions: ImageAnalysisResult): SuggestionEntry[] {
  const entries: SuggestionEntry[] = [];

  if (suggestions.suggestedName) {
    entries.push({
      field: 'name',
      label: 'Nom',
      value: suggestions.suggestedName,
      displayValue: suggestions.suggestedName,
    });
  }

  if (suggestions.suggestedStatus) {
    entries.push({
      field: 'status',
      label: 'Statut',
      value: suggestions.suggestedStatus,
      displayValue: STATUS_LABELS[suggestions.suggestedStatus as ItemStatus] || suggestions.suggestedStatus,
    });
  }

  if (suggestions.suggestedStock != null) {
    entries.push({
      field: 'stock',
      label: 'Stock',
      value: suggestions.suggestedStock,
      displayValue: String(suggestions.suggestedStock),
    });
  }

  if (suggestions.suggestedCustomFieldValues) {
    for (const [key, value] of Object.entries(suggestions.suggestedCustomFieldValues)) {
      entries.push({
        field: `customFieldValues.${key}`,
        label: key,
        value,
        displayValue: String(value),
      });
    }
  }

  return entries;
}

export function ImageAnalysisSuggestions({
  suggestions,
  isAnalyzing,
  onApply,
  onApplyAll,
  onDismiss,
}: ImageAnalysisSuggestionsProps) {
  if (!isAnalyzing && !suggestions) return null;

  if (isAnalyzing) {
    return (
      <div className="rounded-lg border border-border/50 bg-muted/30 p-4">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin" />
          <span>Analyse de l'image en cours...</span>
        </div>
        <div className="mt-3 space-y-2">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
        </div>
      </div>
    );
  }

  if (!suggestions || suggestions.status !== 'COMPLETED') return null;

  const entries = getSuggestionEntries(suggestions);
  if (entries.length === 0) return null;

  return (
    <div className="rounded-lg border border-primary/20 bg-primary/5 p-4">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2 text-sm font-medium">
          <Sparkles className="h-4 w-4 text-primary" />
          <span>Suggestions IA</span>
        </div>
        <button
          type="button"
          onClick={onDismiss}
          className="text-muted-foreground hover:text-foreground transition-colors"
          aria-label="Fermer les suggestions"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="space-y-2">
        {entries.map((entry) => (
          <div key={entry.field} className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2 min-w-0">
              <Badge variant="secondary" className="shrink-0 text-xs">
                {entry.label}
              </Badge>
              <span className="text-sm truncate">{entry.displayValue}</span>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              className="shrink-0 h-7 px-2 text-xs"
              onClick={() => onApply(entry.field, entry.value)}
            >
              <Check className="h-3 w-3 mr-1" />
              Appliquer
            </Button>
          </div>
        ))}
      </div>

      {entries.length > 1 && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="mt-3 w-full text-xs"
          onClick={onApplyAll}
        >
          <Check className="h-3 w-3 mr-1" />
          Tout appliquer
        </Button>
      )}
    </div>
  );
}
