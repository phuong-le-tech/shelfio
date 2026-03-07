import { describe, it, expect } from 'vitest';

type Role = 'USER' | 'PREMIUM_USER' | 'ADMIN';

function isPremium(role: Role): boolean {
  return role === 'PREMIUM_USER' || role === 'ADMIN';
}

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
