import type { Absence, Inbox, MyAbsences } from '@/types';

import { api } from './client';

// Caddie: "Not coming in"
export function fetchMyAbsences() {
  return api.get<MyAbsences>('/absences/mine');
}

export function requestAbsence(date: string, reason: string) {
  return api.post<Absence>('/absences', { date, reason });
}

export function cancelAbsence(id: number) {
  return api.delete<void>(`/absences/${id}`);
}

// Admin: Inbox
export function fetchInbox() {
  return api.get<Inbox>('/admin/absences');
}

export function decideAbsence(id: number, approve: boolean) {
  return api.post<Absence>(`/admin/absences/${id}/decision`, { approve });
}
