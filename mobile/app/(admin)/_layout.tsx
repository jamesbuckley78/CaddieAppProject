import { Redirect, Tabs } from 'expo-router';

import { SignOutButton } from '@/components/SignOutButton';
import { colors } from '@/components/theme';
import { useAuth } from '@/hooks/useAuth';

// Admin (caddie master) side. Designed to work on a phone, tablet or PC browser.
// Route guard: only logged-in admins get past these checks.
export default function AdminLayout() {
  const { status, user } = useAuth();

  if (status === 'loading') return null;
  if (!user) return <Redirect href="/login" />;
  if (user.mustChangePassword) return <Redirect href="/set-password" />;
  if (user.role !== 'ADMIN') return <Redirect href="/home" />;

  return (
    <Tabs
      screenOptions={{
        headerRight: () => <SignOutButton />,
        tabBarActiveTintColor: colors.primary,
        tabBarIconStyle: { display: 'none' }, // no icons yet - hides the placeholder arrows
        tabBarLabelStyle: { fontSize: 14 },
      }}>
      <Tabs.Screen name="board" options={{ title: 'Board' }} />
      <Tabs.Screen name="caddies" options={{ title: 'Caddies' }} />
      <Tabs.Screen name="inbox" options={{ title: 'Inbox' }} />
      <Tabs.Screen name="attendance" options={{ title: 'Attendance' }} />
    </Tabs>
  );
}
