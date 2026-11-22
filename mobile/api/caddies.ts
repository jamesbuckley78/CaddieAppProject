import type { Caddie, CaddieWithSetupCode, NewCaddie } from '@/types';

import { api } from './client';

// Admin-only endpoints (the backend returns 403 for caddies).

export function listCaddies() {
  return api.get<Caddie[]>('/admin/caddies');
}

export function createCaddie(caddie: NewCaddie) {
  return api.post<CaddieWithSetupCode>('/admin/caddies', caddie);
}

export function resetSetupCode(caddieId: number) {
  return api.post<CaddieWithSetupCode>(`/admin/caddies/${caddieId}/reset-code`);
}
