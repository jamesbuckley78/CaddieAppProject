import { Redirect } from 'expo-router';
import { ActivityIndicator, View } from 'react-native';

import { homeFor, useAuth } from '@/hooks/useAuth';

// Entry point (/): sends each person to the right place.
export default function Index() {
  const { status, user } = useAuth();

  if (status === 'loading') {
    // still reading the saved login from the device
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator />
      </View>
    );
  }
  if (!user) return <Redirect href="/login" />;
  return <Redirect href={homeFor(user)} />;
}
