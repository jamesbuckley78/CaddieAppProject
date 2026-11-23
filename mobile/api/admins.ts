import type { Admin, AdminWithSetupCode } from '@/types';

import { api } from './client';

// Admin-only: the caddie masters' own accounts.

export function listAdmins() {
  return api.get<Admin[]>('/admin/admins');
}

export function createAdmin(name: string, phone: string) {
  return api.post<AdminWithSetupCode>('/admin/admins', { name, phone });
}

export function resetAdminCode(id: number) {
  return api.post<AdminWithSetupCode>(`/admin/admins/${id}/reset-code`);
}

export function removeAdmin(id: number) {
  return api.post<void>(`/admin/admins/${id}/remove`);
}
