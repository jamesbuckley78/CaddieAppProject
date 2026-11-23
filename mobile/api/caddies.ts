import type { BagType, Caddie, CaddieEdit, CaddieWithSetupCode, NewCaddie } from '@/types';

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

/** Name, phone, grade, priority. A new group puts them at the top of it. */
export function editCaddie(caddieId: number, edit: CaddieEdit) {
  return api.post<Caddie>(`/admin/caddies/${caddieId}/edit`, edit);
}

export function markLeft(caddieId: number) {
  return api.post<Caddie>(`/admin/caddies/${caddieId}/left`);
}

export function bringBack(caddieId: number) {
  return api.post<Caddie>(`/admin/caddies/${caddieId}/back`);
}

/** A bag entered by hand. `date` like "2026-11-25", today or earlier. */
export function addBag(caddieId: number, date: string, bagType: BagType) {
  return api.post<void>('/admin/loops', { caddieId, date, bagType });
}
