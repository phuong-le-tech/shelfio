import { useState, useRef, useCallback, useEffect } from 'react';
import { imageAnalysisApi } from '../services/api';
import { ImageAnalysisResult } from '../types/item';
import axios from 'axios';

const INITIAL_POLL_INTERVAL_MS = 1000;
const MAX_POLL_INTERVAL_MS = 8000;
const POLL_TIMEOUT_MS = 90_000;

interface UseImageAnalysisReturn {
  suggestions: ImageAnalysisResult | null;
  isAnalyzing: boolean;
  error: string | null;
  isAvailable: boolean;
  startAnalysis: (file: File, listId?: string) => void;
  reset: () => void;
}

export function useImageAnalysis(): UseImageAnalysisReturn {
  const [suggestions, setSuggestions] = useState<ImageAnalysisResult | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isAvailable, setIsAvailable] = useState(false);

  const pollIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const abortRef = useRef(false);

  const cleanup = useCallback(() => {
    if (pollIntervalRef.current) {
      clearTimeout(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
    abortRef.current = true;
  }, []);

  const reset = useCallback(() => {
    cleanup();
    setSuggestions(null);
    setIsAnalyzing(false);
    setError(null);
  }, [cleanup]);

  // Check AI availability on mount
  useEffect(() => {
    imageAnalysisApi.checkStatus()
      .then(({ available }) => setIsAvailable(available))
      .catch(() => setIsAvailable(false));
  }, []);

  const startAnalysis = useCallback((file: File, listId?: string) => {
    if (!isAvailable) return;
    reset();
    abortRef.current = false;
    setIsAnalyzing(true);

    imageAnalysisApi.analyze(file, listId)
      .then(({ analysisId }) => {
        if (abortRef.current) return;

        // Start polling with exponential backoff
        let currentInterval = INITIAL_POLL_INTERVAL_MS;
        const schedulePoll = () => {
          pollIntervalRef.current = setTimeout(async () => {
            if (abortRef.current) return;
            try {
              const result = await imageAnalysisApi.getResult(analysisId);
              if (abortRef.current) return;

              if (result.status === 'COMPLETED') {
                cleanup();
                setSuggestions(result);
                setIsAnalyzing(false);
                return;
              } else if (result.status === 'FAILED') {
                cleanup();
                setIsAnalyzing(false);
                setError("L'analyse a échoué");
                return;
              }
            } catch {
              // Polling error — silently ignore, will retry
            }
            currentInterval = Math.min(currentInterval * 2, MAX_POLL_INTERVAL_MS);
            schedulePoll();
          }, currentInterval);
        };
        schedulePoll();

        // Timeout
        timeoutRef.current = setTimeout(() => {
          if (!abortRef.current) {
            cleanup();
            setIsAnalyzing(false);
          }
        }, POLL_TIMEOUT_MS);
      })
      .catch((err) => {
        if (abortRef.current) return;
        if (axios.isAxiosError(err) && err.response?.status === 503) {
          setIsAvailable(false);
        } else {
          setError("L'analyse a échoué");
        }
        setIsAnalyzing(false);
      });
  }, [reset, cleanup, isAvailable]);

  useEffect(() => {
    return cleanup;
  }, [cleanup]);

  return { suggestions, isAnalyzing, error, isAvailable, startAnalysis, reset };
}
