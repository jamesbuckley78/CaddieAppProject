import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { createAdmin, listAdmins, removeAdmin, resetAdminCode } from '@/api/admins';
import { Button } from '@/components/Button';
import { ErrorText } from '@/components/ErrorText';
import { Screen } from '@/components/Screen';
import { TextField } from '@/components/TextField';
import { colors } from '@/components/theme';
import { useAuth } from '@/hooks/useAuth';
import { useLiveData } from '@/hooks/useLiveData';
import type { Admin, AdminWithSetupCode } from '@/types';

/** The caddie masters' own accounts: add one, give a new setup code, remove one who has left. */
export default function AdminsScreen() {
  const { user } = useAuth();
  const { data: admins, error: loadError, reload } = useLiveData(listAdmins, 60000);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [newCode, setNewCode] = useState<AdminWithSetupCode | null>(null);
  const [confirmRemove, setConfirmRemove] = useState<number | null>(null);

  async function run(action: () => Promise<unknown>) {
    setError(null);
    setBusy(true);
    try {
      await action();
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong');
    } finally {
      setBusy(false);
      setConfirmRemove(null);
    }
  }

  function handleCreate() {
    run(async () => {
      setNewCode(await createAdmin(name, phone));
      setName('');
      setPhone('');
    });
  }

  function handleRemove(admin: Admin) {
    if (confirmRemove !== admin.id) {
      setConfirmRemove(admin.id); // first tap asks to confirm
      return;
    }
    run(() => removeAdmin(admin.id));
  }

  return (
    <Screen title="Admins">
      {newCode && (
        <View style={[styles.card, styles.codeCard]}>
          <Text style={styles.cardTitle}>Setup code for {newCode.admin.name}</Text>
          <Text style={styles.code}>{newCode.setupCode}</Text>
          <Text style={styles.muted}>
            They log in with their phone number ({newCode.admin.phone}) and this code within 48 hours, then choose a
            password. It won't be shown again.
          </Text>
          <Button label="Done" variant="secondary" onPress={() => setNewCode(null)} />
        </View>
      )}

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Add a caddie master</Text>
        <TextField label="Name" value={name} onChangeText={setName} />
        <TextField label="Phone number" value={phone} onChangeText={setPhone} keyboardType="phone-pad" />
        <Button label="Create admin" onPress={handleCreate} loading={busy} disabled={!name || !phone} />
      </View>

      <ErrorText message={loadError ?? error} />
      <Text style={styles.cardTitle}>Admins ({admins?.length ?? 0})</Text>
      {admins?.map((a) => (
        <View key={a.id} style={styles.card}>
          <Text style={styles.name}>
            {a.name}
            {a.id === user?.id ? ' (you)' : ''}
          </Text>
          <Text style={styles.muted}>
            {a.phone} · {a.passwordSet ? 'Active' : 'Waiting for first login'}
          </Text>
          {a.id !== user?.id && (
            <View style={styles.actions}>
              <Button label="New code" variant="secondary" disabled={busy} onPress={() => run(async () => setNewCode(await resetAdminCode(a.id)))} />
              <Button
                label={confirmRemove === a.id ? 'Tap again to remove' : 'Remove'}
                variant="secondary"
                disabled={busy}
                onPress={() => handleRemove(a)}
              />
            </View>
          )}
        </View>
      ))}
      <Text style={styles.muted}>
        A new code also logs that admin out everywhere (use it for a forgotten password). Removing someone logs them out
        and stops their login; the audit log keeps what they did.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 16,
    gap: 8,
  },
  codeCard: { backgroundColor: colors.highlight },
  cardTitle: { fontSize: 18, fontWeight: '600', color: colors.text },
  code: { fontSize: 36, fontWeight: '700', letterSpacing: 6, color: colors.text },
  name: { fontSize: 16, fontWeight: '600', color: colors.text },
  muted: { color: colors.muted, fontSize: 14 },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
});
