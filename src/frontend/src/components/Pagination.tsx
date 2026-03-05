import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-center gap-4 mt-8">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(Math.max(0, page - 1))}
        disabled={page === 0}
      >
        <ChevronLeft className="h-4 w-4 mr-1" />
        Precedent
      </Button>
      <span className="text-sm text-muted-foreground">
        Page {page + 1} sur {totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
        disabled={page >= totalPages - 1}
      >
        Suivant
        <ChevronRight className="h-4 w-4 ml-1" />
      </Button>
    </div>
  );
}
