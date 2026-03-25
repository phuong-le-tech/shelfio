import { describe, it, expect } from "vitest";
import { ImageAnalysisResult } from "../types/item";

describe("ImageAnalysisResult type", () => {
  it("should represent a PENDING analysis", () => {
    const result: ImageAnalysisResult = {
      analysisId: "test-123",
      status: "PENDING",
    };
    expect(result.status).toBe("PENDING");
    expect(result.suggestedName).toBeUndefined();
  });

  it("should represent a COMPLETED analysis with suggestions", () => {
    const result: ImageAnalysisResult = {
      analysisId: "test-456",
      status: "COMPLETED",
      suggestedName: "Blue Keyboard",
      suggestedStatus: "AVAILABLE",
      suggestedStock: 1,
      suggestedCustomFieldValues: { color: "Blue" },
    };
    expect(result.status).toBe("COMPLETED");
    expect(result.suggestedName).toBe("Blue Keyboard");
    expect(result.suggestedStatus).toBe("AVAILABLE");
    expect(result.suggestedStock).toBe(1);
    expect(result.suggestedCustomFieldValues).toEqual({ color: "Blue" });
  });

  it("should represent a FAILED analysis", () => {
    const result: ImageAnalysisResult = {
      analysisId: "test-789",
      status: "FAILED",
      errorMessage: "AI service unavailable",
    };
    expect(result.status).toBe("FAILED");
    expect(result.errorMessage).toBe("AI service unavailable");
    expect(result.suggestedName).toBeUndefined();
  });

  it("should handle null suggestion fields", () => {
    const result: ImageAnalysisResult = {
      analysisId: "test-null",
      status: "COMPLETED",
      suggestedName: null,
      suggestedStatus: null,
      suggestedStock: null,
      suggestedCustomFieldValues: null,
    };
    expect(result.suggestedName).toBeNull();
    expect(result.suggestedStatus).toBeNull();
    expect(result.suggestedStock).toBeNull();
    expect(result.suggestedCustomFieldValues).toBeNull();
  });
});
