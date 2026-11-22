import { Redirect, router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';

import { Button } from '@/components/Button';
import { ErrorText } from '@/components/ErrorText';
import { Screen } from '@/components/Screen';
import { SignOutButton } from '@/components/SignOutButton';
import { TextField } from '@/components/TextField';
import { colors } from '@/components/theme';
import { homeFor, useAuth } from '@/hooks/useAuth';

export default function SetPasswordScreen() {
  const { status, user, setPassword } = useAuth();
  const [password, setPasswordText] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (status === 'loading') return null;
  if (!user) return <Redirect href="/login" />;

  async function handleSave() {
    if (password.length < 6) {
      setError('Use at least 6 characters.');
      return;
    }
    if (password !== confirm) {
      setError("The two passwords don't match.");
      return;
    }
    setError(null);
    setBusy(true);
    try {
      const updated = await setPassword(password);
      router.replace(homeFor(updated));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not save password');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen title={`Welcome, ${user.name}`}>
      <Text style={styles.intro}>Choose your own password. You'll stay logged in on this device afterwards.</Text>
      <TextField label="New password" value={password} onChangeText={setPasswordText} secureTextEntry />
      <TextField
        label="Type it again"
        value={confirm}
        onChangeText={setConfirm}
        secureTextEntry
        onSubmitEditing={handleSave}
      />
      <ErrorText message={error} />
      <Button label="Save password" onPress={handleSave} loading={busy} disabled={!password || !confirm} />
      <SignOutButton />
    </Screen>
  );
}

const styles = StyleSheet.create({
  intro: { color: colors.muted, fontSize: 15 },
});
