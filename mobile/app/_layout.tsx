import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

// Expo Router: every file in app/ is a screen, and its path is the URL.
//   app/login.tsx           -> /login
//   app/(caddie)/index.tsx  -> /           (folders in brackets are "groups": they share a
//   app/(admin)/board.tsx   -> /board       layout but don't appear in the URL)
// _layout.tsx files wrap the screens beside them; this root one wraps the whole app.
export default function RootLayout() {
  return (
    <>
      <StatusBar style="auto" />
      <Stack screenOptions={{ headerShown: false }} />
    </>
  );
}
