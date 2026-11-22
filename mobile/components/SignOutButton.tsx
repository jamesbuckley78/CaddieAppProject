import { router } from 'expo-router';
import { Pressable, StyleSheet, Text } from 'react-native';

import { useAuth } from '@/hooks/useAuth';

import { colors } from './theme';

/** Small "Log out" link for the top bar of the caddie and admin tabs. */
export function SignOutButton() {
  const { signOut } = useAuth();

  async function handlePress() {
    await signOut();
    router.replace('/login'); // replace = the back button won't return to the logged-in screen
  }

  return (
    <Pressable onPress={handlePress} accessibilityRole="button" style={styles.button}>
      <Text style={styles.text}>Log out</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: { paddingHorizontal: 16, paddingVertical: 8 },
  text: { color: colors.primary, fontWeight: '600' },
});
