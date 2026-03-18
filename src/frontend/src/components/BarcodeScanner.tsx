import { useEffect, useRef, useState, useCallback } from "react";
import { Html5Qrcode } from "html5-qrcode";
import { CheckCircle2, ScanLine } from "lucide-react";
import { cn } from "@/lib/utils";

export type ScanStatus = "scanning" | "detected" | "idle";

interface BarcodeScannerProps {
  onScan: (decodedText: string) => void;
  onError?: (error: string) => void;
  onStatusChange?: (status: ScanStatus) => void;
}

export default function BarcodeScanner({ onScan, onError, onStatusChange }: BarcodeScannerProps) {
  const scannerRef = useRef<Html5Qrcode | null>(null);
  const containerId = "barcode-scanner-container";
  const [status, setStatus] = useState<ScanStatus>("idle");

  const updateStatus = useCallback((newStatus: ScanStatus) => {
    setStatus(newStatus);
    onStatusChange?.(newStatus);
  }, [onStatusChange]);

  useEffect(() => {
    const scanner = new Html5Qrcode(containerId);
    scannerRef.current = scanner;

    scanner
      .start(
        { facingMode: "environment" },
        {
          fps: 10,
          qrbox: { width: 250, height: 250 },
          aspectRatio: 1,
        },
        (decodedText) => {
          updateStatus("detected");
          onScan(decodedText);
        },
        () => {
          // Frame scanned but no code found
          updateStatus("scanning");
        },
      )
      .then(() => {
        updateStatus("scanning");
      })
      .catch((err) => {
        const message =
          err instanceof Error ? err.message : String(err);
        onError?.(message);
      });

    return () => {
      if (scannerRef.current?.isScanning) {
        scannerRef.current.stop().catch(() => {
          // Ignore stop errors during cleanup
        });
      }
    };
  }, []);

  return (
    <div className="space-y-3">
      <div className="relative">
        <div
          id={containerId}
          className={cn(
            "w-full rounded-lg overflow-hidden border-2 transition-colors duration-300",
            status === "detected" ? "border-green-500" : status === "scanning" ? "border-red-500" : "border-transparent",
          )}
          style={{ minHeight: 280 }}
        />
      </div>

      <div
        className={cn(
          "flex items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-all duration-300",
          status === "detected"
            ? "bg-green-500/10 text-green-600 dark:text-green-400"
            : status === "scanning"
              ? "bg-red-500/10 text-red-600 dark:text-red-400"
              : "bg-muted text-muted-foreground",
        )}
      >
        {status === "detected" ? (
          <>
            <CheckCircle2 className="h-4 w-4" />
            Code détecté !
          </>
        ) : (
          <>
            <ScanLine className="h-4 w-4 animate-pulse" />
            Aucun code détecté
          </>
        )}
      </div>
    </div>
  );
}
