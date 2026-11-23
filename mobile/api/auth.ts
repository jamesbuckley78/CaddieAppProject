import type { LoginResponse, User } from '@/types';

import { api } from './client';

// Thin wrappers so screens never type URLs. See docs/API.md for what each endpoint does.

export function login(phone: string, password: string) {
  return api.post<LoginResponse>('/auth/login', { phone, password });
}

export function setPassword(newPassword: string) {
  return api.post<LoginResponse>('/auth/set-password', { newPassword });
}

export function fetchMe() {
  return api.get<User>('/auth/me');
}

/** Caddies only: a fresh 90-day login, so they never have to log in again while they use the app. */
export function renewLogin() {
  return api.post<LoginResponse>('/auth/renew');
}
