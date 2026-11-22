import { Redirect, router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';

import { API_URL } from '@/api/client';
import { Button } from '@/components/Button';
import { ErrorText } from '@/components/ErrorText';
import { Screen } from '@/components/Screen';
import { TextField } from '@/components/TextField';
import { colors } from '@/components/theme';
import { homeFor, useAuth } from '@/hooks/useAuth';
import { useServerStatus } from '@/hooks/useServerStatus';

export default function LoginScreen() {
  const { status, user, signIn } = useAuth();
  const server = useServerStatus();

  // "Controlled inputs": the text lives in state, and each keystroke updates it.
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Already logged in (e.g. opened /login by hand) - go to the right home screen.
  if (status === 'signedIn' && user) return <Redirect href={homeFor(user)} />;

  async function handleLogin() {
    setError(null);
    setBusy(true);
    // try/catch/finally works just like Java's, including around `await`
    try {
      const loggedIn = await signIn(phone, password);
      router.replace(homeFor(loggedIn));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Login failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen title="Caddie List">
      <Text style={styles.intro}>
        Log in with your phone number. First time? Use the 6-digit setup code from the caddie master as your
        password.
      </Text>

      <TextField
        label="Phone number"
        value={phone}
        onChangeText={setPhone}
        keyboardType="phone-pad"
        autoComplete="tel"
        placeholder="087 123 4567"
      />
      <TextField
        label="Password or setup code"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        autoComplete="password"
        onSubmitEditing={handleLogin} // Enter key on web / "go" on phone keyboards
      />
      <ErrorText message={error} />
      <Button label="Log in" onPress={handleLogin} loading={busy} disabled={!phone || !password} />

      <Text style={styles.server}>
        Server {API_URL}: {server}
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  intro: { color: colors.muted, fontSize: 15 },
  server: { color: colors.muted, fontSize: 12, marginTop: 24 },
});
