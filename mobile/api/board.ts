import type { BagType, Board } from '@/types';

import { api } from './client';

// Admin's buttons on the board. Each returns the updated board, so the screen can redraw
// straight away instead of waiting for the next refresh.

export function callCaddie(caddieId: number, bagType: BagType) {
  return api.post<Board>(`/admin/board/${caddieId}/call`, { bagType });
}

export function sendOut(caddieId: number) {
  return api.post<Board>(`/admin/board/${caddieId}/send-out`);
}

export function noReply(caddieId: number) {
  return api.post<Board>(`/admin/board/${caddieId}/no-reply`);
}

export function adminBackIn(caddieId: number) {
  return api.post<Board>(`/admin/board/${caddieId}/back-in`);
}

/** lineNumber null = undo the move. */
export function moveInLine(caddieId: number, lineNumber: number | null) {
  return api.post<Board>(`/admin/board/${caddieId}/move`, { lineNumber });
}

/** Round twice on the same bag: counts twice. false undoes it. */
export function markDoubleLoop(loopId: number, doubleLoop: boolean) {
  return api.post<Board>(`/admin/loops/${loopId}/double-loop`, { doubleLoop });
}

export function changeBagType(loopId: number, bagType: BagType) {
  return api.post<Board>(`/admin/loops/${loopId}/bag-type`, { bagType });
}
