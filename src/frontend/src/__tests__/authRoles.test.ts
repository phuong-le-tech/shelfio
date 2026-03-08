import { describe, it, expect } from 'vitest';
import { isPremium } from '../types/auth';

describe('isPremium', () => {
  it('should return false for USER role', () => {
    expect(isPremium('USER')).toBe(false);
  });

  it('should return true for PREMIUM_USER role', () => {
    expect(isPremium('PREMIUM_USER')).toBe(true);
  });

  it('should return true for ADMIN role', () => {
    expect(isPremium('ADMIN')).toBe(true);
  });
});
