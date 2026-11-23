import type { Issue, IssueSummary, IssueType } from '@/types';

import { api } from './client';

// Caddie: report an issue. hole and message are optional.
export function reportIssue(type: IssueType, hole: number | null, message: string) {
  return api.post<void>('/issues', { type, hole, message });
}

// Admin: Inbox
export function fetchOpenIssues() {
  return api.get<Issue[]>('/admin/issues');
}

/** Every report in the week containing `date` ("2026-11-25"), or all time when it's left out. */
export function fetchIssueSummary(date?: string) {
  return api.get<IssueSummary>(date ? `/admin/issues/summary?date=${date}` : '/admin/issues/summary');
}

export function resolveIssue(id: number) {
  return api.post<void>(`/admin/issues/${id}/resolve`);
}
