import { Redirect, Tabs } from 'expo-router';

import { SignOutButton } from '@/components/SignOutButton';
import { colors } from '@/components/theme';
import { useAuth } from '@/hooks/useAuth';

// Bottom tabs for the caddie side of the app. Each screen in this folder becomes a tab.
// The checks below are a "route guard": only logged-in caddies get past them.
// (The backend checks too - this just stops people landing on a screen that can't work.)
export default function CaddieLayout() {
  const { status, user } = useAuth();

  if (status === 'loading') return null; // draw nothing for a moment while the saved login loads
  if (!user) return <Redirect href="/login" />;
  if (user.mustChangePassword) return <Redirect href="/set-password" />;
  if (user.role !== 'CADDIE') return <Redirect href="/board" />;

  return (
    <Tabs
      screenOptions={{
        headerRight: () => <SignOutButton />,
        tabBarActiveTintColor: colors.primary,
        tabBarIconStyle: { display: 'none' }, // no icons yet - hides the placeholder arrows
        tabBarLabelStyle: { fontSize: 14 },
      }}>
      <Tabs.Screen name="home" options={{ title: 'Today' }} />
      <Tabs.Screen name="list" options={{ title: 'Club list' }} />
      <Tabs.Screen name="absence" options={{ title: 'Not coming in' }} />
      <Tabs.Screen name="report" options={{ title: 'Report issue' }} />
      <Tabs.Screen name="history" options={{ title: 'History' }} />
    </Tabs>
  );
}
