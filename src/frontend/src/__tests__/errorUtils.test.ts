import { describe, it, expect } from 'vitest';
import { getApiErrorMessage, getApiErrorStatus } from '../utils/errorUtils';
import { AxiosError, AxiosHeaders } from 'axios';

function makeAxiosError(status: number, message?: string): AxiosError {
  const headers = new AxiosHeaders();
  const error = new AxiosError('Request failed', 'ERR_BAD_REQUEST', undefined, undefined, {
    status,
    statusText: 'Bad Request',
    headers,
    config: { headers } as never,
    data: message ? { error: { message } } : {},
  });
  return error;
}

describe('getApiErrorMessage', () => {
  it('extracts server error message from Axios error', () => {
    const err = makeAxiosError(400, 'Name is required');
    expect(getApiErrorMessage(err, 'fallback')).toBe('Name is required');
  });

  it('falls through to Error.message when Axios error has no server message', () => {
    const err = makeAxiosError(500);
    // AxiosError extends Error, so err.message is used when no server message exists
    expect(getApiErrorMessage(err, 'fallback')).toBe('Request failed');
  });

  it('returns Error.message for plain Error', () => {
    const err = new Error('Network failure');
    expect(getApiErrorMessage(err, 'fallback')).toBe('Network failure');
  });

  it('returns fallback for unknown error types', () => {
    expect(getApiErrorMessage('string error', 'fallback')).toBe('fallback');
    expect(getApiErrorMessage(42, 'fallback')).toBe('fallback');
    expect(getApiErrorMessage(null, 'fallback')).toBe('fallback');
  });
});

describe('getApiErrorStatus', () => {
  it('extracts HTTP status from Axios error', () => {
    const err = makeAxiosError(404);
    expect(getApiErrorStatus(err)).toBe(404);
  });

  it('returns undefined for non-Axios errors', () => {
    expect(getApiErrorStatus(new Error('fail'))).toBeUndefined();
    expect(getApiErrorStatus('string')).toBeUndefined();
  });
});
