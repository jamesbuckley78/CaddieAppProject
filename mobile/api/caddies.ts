import type { Caddie, CaddieWithSetupCode, NewCaddie } from '@/types';

import { api } from './client';

// Admin-only endpoints (the backend returns 403 for caddies).

export function listCaddies() {
  return api.get<Caddie[]>('/admin/caddies');
}

export function createCaddie(caddie: NewCaddie) {
  return api.post<CaddieWithSetupCode>('/admin/caddies', caddie);
}

/** Move on the club list, within the caddie's own group (priority / adults / juniors). */
export function moveCaddie(caddieId: number, position: number) {
  return api.post<Caddie>(`/admin/caddies/${caddieId}/move`, { position });
}

export function resetSetupCode(caddieId: number) {
  return api.post<CaddieWithSetupCode>(`/admin/caddies/${caddieId}/reset-code`);
}
