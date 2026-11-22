import Constants from 'expo-constants';
import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';

import { api } from './client';

// Push notifications ("You're up!") through Expo's push service.
//
// They only work in a real installed build of the app on a phone (an EAS development or
// release build). They don't work in a web browser, the Android emulator, or Expo Go (Expo
// removed remote push from Expo Go in SDK 53). In those cases this quietly does nothing and
// the caddie still sees the call on their Today screen, which refreshes every 10 seconds.

const isExpoGo = Constants.executionEnvironment === 'storeClient';
const pushPossible = Platform.OS !== 'web' && Device.isDevice && !isExpoGo;

if (pushPossible) {
  // Show the notification even if the app is open on screen
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: true,
      shouldSetBadge: false,
    }),
  });
}

/** Asks permission, gets this phone's push token and sends it to the backend. */
export async function registerForPushNotifications(): Promise<string | null> {
  if (!pushPossible) return null;
  // Set by `eas init` (app.json -> extra.eas.projectId). Without it Expo can't issue a token.
  const projectId = Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
  if (!projectId) return null;

  try {
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('default', {
        name: 'Calls',
        importance: Notifications.AndroidImportance.MAX,
      });
    }
    let { status } = await Notifications.getPermissionsAsync();
    if (status !== 'granted') {
      status = (await Notifications.requestPermissionsAsync()).status;
    }
    if (status !== 'granted') return null;

    const token = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
    await api.post('/auth/push-token', { provider: 'EXPO', token });
    return token;
  } catch {
    return null; // push is a bonus - never block the app over it
  }
}
