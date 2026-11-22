import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Switch, Text, View } from 'react-native';

import { createCaddie, listCaddies, resetSetupCode } from '@/api/caddies';
import { Button } from '@/components/Button';
import { ErrorText } from '@/components/ErrorText';
import { Screen } from '@/components/Screen';
import { TextField } from '@/components/TextField';
import { colors } from '@/components/theme';
import type { Caddie, CaddieWithSetupCode, Grade } from '@/types';

const GRADES: Grade[] = ['SINGLE', 'DOUBLE', 'JUNIOR'];
const GRADE_LABEL: Record<Grade, string> = { SINGLE: 'Single', DOUBLE: 'Double', JUNIOR: 'Junior' };

function errorMessage(e: unknown) {
  return e instanceof Error ? e.message : 'Something went wrong';
}

export default function CaddiesScreen() {
  // Form state
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [grade, setGrade] = useState<Grade>('SINGLE');
  const [priority, setPriority] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  // Last setup code to show the admin (created or reset)
  const [newCode, setNewCode] = useState<CaddieWithSetupCode | null>(null);

  // List state
  const [caddies, setCaddies] = useState<Caddie[]>([]);
  const [listError, setListError] = useState<string | null>(null);

  async function refresh() {
    try {
      setCaddies(await listCaddies());
      setListError(null);
    } catch (e) {
      setListError(errorMessage(e));
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  async function handleCreate() {
    setFormError(null);
    setSaving(true);
    try {
      const result = await createCaddie({ name, phone, grade, priority });
      setNewCode(result);
      setName('');
      setPhone('');
      setPriority(false);
      await refresh();
    } catch (e) {
      setFormError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  async function handleReset(caddie: Caddie) {
    try {
      setNewCode(await resetSetupCode(caddie.id));
      await refresh();
    } catch (e) {
      setListError(errorMessage(e));
    }
  }

  return (
    <Screen title="Caddies">
      {newCode && (
        <View style={[styles.card, styles.codeCard]}>
          <Text style={styles.cardTitle}>Setup code for {newCode.caddie.name}</Text>
          <Text style={styles.code}>{newCode.setupCode}</Text>
          <Text style={styles.muted}>
            Give them this code. They log in with their phone number ({newCode.caddie.phone}) and this code, then
            choose a password. It won't be shown again.
          </Text>
          <Button label="Done" variant="secondary" onPress={() => setNewCode(null)} />
        </View>
      )}

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Add a caddie</Text>
        <TextField label="Name" value={name} onChangeText={setName} placeholder="Sean Murphy" />
        <TextField
          label="Phone number"
          value={phone}
          onChangeText={setPhone}
          keyboardType="phone-pad"
          placeholder="087 123 4567"
        />

        <Text style={styles.label}>Grade</Text>
        <View style={styles.row}>
          {/* .map() turns each item of an array into a piece of UI, like a Java stream.
              `key` lets React tell the items apart when the list changes. */}
          {GRADES.map((g) => (
            <Pressable
              key={g}
              onPress={() => {
                setGrade(g);
                if (g === 'JUNIOR') setPriority(false); // juniors can't be priority
              }}
              accessibilityRole="radio"
              accessibilityState={{ selected: grade === g }}
              style={[styles.choice, grade === g && styles.choiceSelected]}>
              <Text style={[styles.choiceText, grade === g && styles.choiceTextSelected]}>{GRADE_LABEL[g]}</Text>
            </Pressable>
          ))}
        </View>

        <View style={styles.switchRow}>
          <Text style={styles.label}>Priority caddie (top 50)</Text>
          <Switch value={priority} onValueChange={setPriority} disabled={grade === 'JUNIOR'} />
        </View>

        <ErrorText message={formError} />
        <Button label="Create caddie" onPress={handleCreate} loading={saving} disabled={!name || !phone} />
      </View>

      <Text style={styles.cardTitle}>All caddies ({caddies.length})</Text>
      <ErrorText message={listError} />
      {caddies.length === 0 && !listError && <Text style={styles.muted}>No caddies yet.</Text>}
      {caddies.map((c) => (
        <View key={c.id} style={[styles.card, styles.caddieRow]}>
          <View style={{ flex: 1, gap: 2 }}>
            <Text style={styles.caddieName}>
              {c.name} {c.priority && <Text style={styles.badge}> P </Text>}
            </Text>
            <Text style={styles.muted}>
              {GRADE_LABEL[c.grade]} · {c.phone} · {c.passwordSet ? 'Active' : 'Waiting for first login'}
            </Text>
          </View>
          <Button label="New code" variant="secondary" onPress={() => handleReset(c)} />
        </View>
      ))}
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
    gap: 10,
  },
  codeCard: { backgroundColor: colors.highlight },
  cardTitle: { fontSize: 18, fontWeight: '600', color: colors.text },
  code: { fontSize: 36, fontWeight: '700', letterSpacing: 6, color: colors.text },
  label: { fontSize: 14, fontWeight: '500', color: colors.text },
  muted: { color: colors.muted, fontSize: 14 },
  row: { flexDirection: 'row', gap: 8 },
  choice: {
    flex: 1,
    minHeight: 44,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  choiceSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  choiceText: { color: colors.text, fontWeight: '500' },
  choiceTextSelected: { color: colors.primaryText },
  switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  caddieRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  caddieName: { fontSize: 16, fontWeight: '600', color: colors.text },
  badge: { backgroundColor: colors.primary, color: colors.primaryText, fontSize: 12, fontWeight: '700' },
});
