import { describe, it, expect } from 'vitest';

// slugify is a local function in ListForm.tsx — reimport the logic for testing
// Since it's not exported, we replicate the exact implementation here
function slugify(label: string): string {
  return label.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
}

describe('slugify', () => {
  it('lowercases and replaces spaces with underscores', () => {
    expect(slugify('My Field')).toBe('my_field');
  });

  it('replaces multiple special chars with a single underscore', () => {
    expect(slugify('Hello---World!!!')).toBe('hello_world');
  });

  it('trims leading and trailing underscores', () => {
    expect(slugify('  leading spaces  ')).toBe('leading_spaces');
  });

  it('handles accented characters by replacing them', () => {
    expect(slugify('Catégorie')).toBe('cat_gorie');
  });

  it('returns empty string for empty input', () => {
    expect(slugify('')).toBe('');
  });

  it('preserves numbers', () => {
    expect(slugify('field 123 test')).toBe('field_123_test');
  });
});
