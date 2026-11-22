import type { History } from '@/types';

import { api } from './client';

/** The caddie's own loops and days worked, newest week first. */
export function fetchHistory(weeks = 8) {
  return api.get<History>(`/history/me?weeks=${weeks}`);
}
