import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

import type { LoginResponse } from '@/types';

// Keeps the login (token + user) on the device so caddies "log in once and stay logged in".
// Phones: expo-secure-store (Android Keystore / iOS Keychain - encrypted).
// Web: expo-secure-store doesn't work in browsers, so fall back to localStorage.
// Platform.OS tells you which one the code is running on: 'android', 'ios' or 'web'.

const KEY = 'caddie.session';

export async function saveSession(session: LoginResponse) {
  const value = JSON.stringify(session);
  if (Platform.OS === 'web') {
    try {
      localStorage.setItem(KEY, value);
    } catch {
      // private browsing etc. - the user just has to log in again next time
    }
    return;
  }
  await SecureStore.setItemAsync(KEY, value);
}

export async function loadSession(): Promise<LoginResponse | null> {
  try {
    const value = Platform.OS === 'web' ? localStorage.getItem(KEY) : await SecureStore.getItemAsync(KEY);
    return value ? (JSON.parse(value) as LoginResponse) : null;
  } catch {
    return null;
  }
}

export async function clearSession() {
  try {
    if (Platform.OS === 'web') {
      localStorage.removeItem(KEY);
    } else {
      await SecureStore.deleteItemAsync(KEY);
    }
  } catch {
    // nothing stored - nothing to clear
  }
}
