import { getApiClient } from '@/services/api/client';

export interface ApiKeyResponse {
  id: number;
  keyPrefix: string;
  status: string;
  createdBy?: string;
  createdAt?: string;
  revokedBy?: string;
  revokedAt?: string;
  lastUsedAt?: string;
  /** Present only on rotate response */
  secret?: string;
}

const BASE = '/api/api-keys';

export const apiKeyService = {
  async list(): Promise<ApiKeyResponse[]> {
    const client = getApiClient();
    const res = await client.get<ApiKeyResponse[]>(BASE);
    if (!res.success || res.data == null) return [];
    return res.data;
  },

  async rotate(): Promise<ApiKeyResponse> {
    const client = getApiClient();
    const res = await client.post<ApiKeyResponse>(`${BASE}/rotate`, {});
    if (!res.success || !res.data) throw new Error(res.message ?? 'Failed to generate API key');
    return res.data;
  },

  async revoke(id: number): Promise<ApiKeyResponse> {
    const client = getApiClient();
    const res = await client.post<ApiKeyResponse>(`${BASE}/${id}/revoke`, {});
    if (!res.success || !res.data) throw new Error(res.message ?? 'Failed to revoke API key');
    return res.data;
  },
};
