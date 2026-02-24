import { describe, it, expect } from 'vitest';
import { formatCustomFieldValue, formatStatus, STATUS_LABELS } from '../types/item';

describe('formatCustomFieldValue', () => {
  it('formats BOOLEAN true as "Oui"', () => {
    expect(formatCustomFieldValue('BOOLEAN', true)).toBe('Oui');
  });

  it('formats BOOLEAN false as "Non"', () => {
    expect(formatCustomFieldValue('BOOLEAN', false)).toBe('Non');
  });

  it('formats DATE to fr-FR locale', () => {
    const result = formatCustomFieldValue('DATE', '2024-01-15');
    expect(result).toBe('15/01/2024');
  });

  it('formats NUMBER as string', () => {
    expect(formatCustomFieldValue('NUMBER', 42)).toBe('42');
  });

  it('formats SELECT as string', () => {
    expect(formatCustomFieldValue('SELECT', 'Option A')).toBe('Option A');
  });

  it('formats TEXT as string', () => {
    expect(formatCustomFieldValue('TEXT', 'hello')).toBe('hello');
  });
});

describe('formatStatus', () => {
  it('maps all known statuses to French labels', () => {
    expect(formatStatus('TO_PREPARE')).toBe('À préparer');
    expect(formatStatus('TO_VERIFY')).toBe('À vérifier');
    expect(formatStatus('PENDING')).toBe('En attente');
    expect(formatStatus('READY')).toBe('Prêt');
    expect(formatStatus('ARCHIVED')).toBe('Archivé');
  });

  it('covers all entries in STATUS_LABELS', () => {
    for (const [status, label] of Object.entries(STATUS_LABELS)) {
      expect(formatStatus(status as Parameters<typeof formatStatus>[0])).toBe(label);
    }
  });
});
