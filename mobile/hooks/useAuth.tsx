import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

import * as authApi from '@/api/auth';
import { ApiError, setAuthToken, setOnLoggedOut } from '@/api/client';
import { registerForPushNotifications } from '@/api/push';
import { clearSession, loadSession, saveSession } from '@/api/sessionStorage';
import type { LoginResponse, User } from '@/types';

// "Context" shares one value with every screen without passing it down by hand.
// <AuthProvider> (in app/_layout.tsx) holds who is logged in; any screen calls useAuth()
// to read it. Think of it as a singleton bean that the UI re-draws from when it changes.

type AuthStatus = 'loading' | 'signedOut' | 'signedIn';

type AuthContextValue = {
  status: AuthStatus;
  user: User | null;
  signIn: (phone: string, password: string) => Promise<User>;
  setPassword: (newPassword: string) => Promise<User>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<User | null>(null);

  async function startSession(session: LoginResponse) {
    setAuthToken(session.token);
    await saveSession(session);
    setUser(session.user);
    setStatus('signedIn');
  }

  async function signOut() {
    setAuthToken(null);
    await clearSession();
    setUser(null);
    setStatus('signedOut');
  }

  // If any request finds the login expired/cancelled, log out (the screens then go to /login)
  useEffect(() => {
    setOnLoggedOut(() => {
      signOut();
    });
    return () => setOnLoggedOut(null);
  }, []);

  // Once a caddie is properly logged in (own password set), register this phone for "You're
  // up!" push notifications. The [..] list means: run again only if one of these changes.
  const pushUserId = user && user.role === 'CADDIE' && !user.mustChangePassword ? user.id : null;
  useEffect(() => {
    if (pushUserId !== null) {
      registerForPushNotifications();
    }
  }, [pushUserId]);

  // On app start: reuse the saved login straight away (works offline), then ask the
  // server for fresh details. Only a 401 (token expired/invalid) logs the user out.
  useEffect(() => {
    (async () => {
      const saved = await loadSession();
      if (!saved) {
        setStatus('signedOut');
        return;
      }
      setAuthToken(saved.token);
      setUser(saved.user);
      setStatus('signedIn');
      try {
        const fresh = await authApi.fetchMe();
        setUser(fresh);
        await saveSession({ token: saved.token, user: fresh });
      } catch (e) {
        // `instanceof` works like Java's: is this error one of ours, with a status?
        if (e instanceof ApiError && e.status === 401) {
          await signOut();
        }
      }
    })();
  }, []);

  async function signIn(phone: string, password: string) {
    const session = await authApi.login(phone, password);
    await startSession(session);
    return session.user;
  }

  async function setPassword(newPassword: string) {
    const session = await authApi.setPassword(newPassword);
    await startSession(session); // the server sends a new token without the "must change" flag
    return session.user;
  }

  return (
    <AuthContext.Provider value={{ status, user, signIn, setPassword, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth() must be used inside <AuthProvider>');
  }
  return value;
}

/** Where a logged-in user belongs. `as const` keeps the exact strings for typed routes. */
export function homeFor(user: User) {
  if (user.mustChangePassword) return '/set-password' as const;
  return user.role === 'ADMIN' ? ('/board' as const) : ('/home' as const);
}
