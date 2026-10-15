import { API_BASE_URL, API_KEY } from '../config/api';
import { getSession } from '../lib/session';
import type { ResponseWrapper } from '../types/cms';

export class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
  }
}

function authHeaders(includeJson = true): HeadersInit {
  const headers: Record<string, string> = {
    Accept: 'application/json',
  };
  if (includeJson) {
    headers['Content-Type'] = 'application/json';
  }
  if (API_KEY) {
    headers['X-Api-Key'] = API_KEY;
  }
  const token = getSession()?.token;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return headers;
}

export async function apiPost<T>(
  path: string,
  body: unknown,
): Promise<ResponseWrapper<T>> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: authHeaders(true),
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const err = (await res.json()) as ResponseWrapper<unknown>;
      message = err.responseMessage || message;
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, message);
  }
  return (await res.json()) as ResponseWrapper<T>;
}

export async function apiGet<T>(path: string): Promise<ResponseWrapper<T>> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method: 'GET',
    headers: authHeaders(false),
  });
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const err = (await res.json()) as ResponseWrapper<unknown>;
      message = err.responseMessage || message;
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, message);
  }
  return (await res.json()) as ResponseWrapper<T>;
}

export function unwrap<T>(
  wrapper: ResponseWrapper<T>,
  allowCodes: number[] = [0],
): T | null {
  if (!allowCodes.includes(wrapper.responseCode)) {
    throw new ApiError(
      wrapper.responseCode,
      wrapper.responseMessage || 'Request failed',
    );
  }
  return wrapper.responseBody;
}
