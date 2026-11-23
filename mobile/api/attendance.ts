import type { AttendanceWeek } from '@/types';

import { api } from './client';

/** Admin: the Monday-Sunday week containing `date` ("2026-11-25"), or this week. */
export function fetchAttendance(date?: string) {
  return api.get<AttendanceWeek>(date ? `/admin/attendance?date=${date}` : '/admin/attendance');
}
