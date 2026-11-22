import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

import { AuthProvider } from '@/hooks/useAuth';

// Expo Router: every file in app/ is a screen, and its path is the URL.
//   app/login.tsx           -> /login
//   app/(caddie)/home.tsx   -> /home    (folders in brackets are "groups": they share a
//   app/(admin)/board.tsx   -> /board    layout but don't appear in the URL)
// _layout.tsx files wrap the screens beside them; this root one wraps the whole app,
// so <AuthProvider> makes the logged-in user available on every screen.
export default function RootLayout() {
  return (
    <AuthProvider>
      <StatusBar style="dark" />
      <Stack screenOptions={{ headerShown: false }} />
    </AuthProvider>
  );
}
