import type { Grade, QueueStatus } from '@/types';

// Small helpers for showing values to people. Kept together so wording is the same everywhere.

export const GRADE_LABEL: Record<Grade, string> = { SINGLE: 'Single', DOUBLE: 'Double', JUNIOR: 'Junior' };

export const STATUS_LABEL: Record<QueueStatus, string> = {
  OFF: 'Not checked in',
  AVAILABLE: 'Available',
  FOOD: 'Getting food',
  BACK_LATER: 'Will be back',
  CALLED: 'Called',
  OUT: 'Out on course',
};

/** "10:45" in the phone's own time zone. */
export function clockTime(iso: string | null) {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

/** Status text including the "back ~10:45" estimate. */
export function statusText(status: QueueStatus, backAt: string | null) {
  return status === 'BACK_LATER' && backAt ? `Back ~${clockTime(backAt)}` : STATUS_LABEL[status];
}

// Dates from the backend are plain "2026-11-25" strings (no time). Adding "T12:00" before
// turning them into a JS Date keeps them on the right day whatever the phone's time zone.

/** "2026-11-25" + 2 -> "2026-11-27" */
export function addDays(isoDate: string, days: number) {
  const d = new Date(isoDate + 'T12:00');
  d.setDate(d.getDate() + days);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** "2026-11-25" -> "Wed 25 Nov" */
export function dayLabel(isoDate: string) {
  return new Date(isoDate + 'T12:00').toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' });
}

/** 1 -> "1st", 2 -> "2nd", 11 -> "11th", 23 -> "23rd" */
export function ordinal(n: number) {
  const teen = n % 100 >= 11 && n % 100 <= 13;
  const suffix = teen ? 'th' : ({ 1: 'st', 2: 'nd', 3: 'rd' } as Record<number, string>)[n % 10] ?? 'th';
  return `${n}${suffix}`;
}
