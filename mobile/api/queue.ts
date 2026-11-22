import type { Board, ClubList, MyDay, QueueStatus } from '@/types';

import { api } from './client';

// Caddie: "I'm here" and statuses. Every call returns the caddie's updated day.

export function fetchMyDay() {
  return api.get<MyDay>('/queue/me');
}

/** latitude/longitude may be missing if the phone couldn't get a position - the server decides. */
export function checkIn(position: { latitude: number; longitude: number } | null) {
  return api.post<MyDay>('/availability/check-in', position ?? {});
}

export function setStatus(status: QueueStatus, backInMinutes?: number) {
  return api.post<MyDay>('/availability/status', { status, backInMinutes });
}

export function checkOut() {
  return api.post<MyDay>('/availability/check-out');
}

/** After being called. */
export function onMyWay() {
  return api.post<MyDay>('/availability/on-my-way');
}

/** After a loop. */
export function backIn() {
  return api.post<MyDay>('/availability/back-in');
}

// Everyone: the club list table (order and bag figures update together daily at 4pm).
export function fetchClubList() {
  return api.get<ClubList>('/queue/club-list');
}

// Admin: everyone checked in today, in line order.
export function fetchBoard() {
  return api.get<Board>('/admin/board');
}
