import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Switch, Text, TextInput, View } from 'react-native';

import { createCaddie, listCaddies, moveCaddie, resetSetupCode } from '@/api/caddies';
import { Button } from '@/components/Button';
import { CaddieTag } from '@/components/CaddieTag';
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
  // On while copying the paper list in order: each new caddie goes to the bottom of their group
  const [paperList, setPaperList] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  // Last setup code to show the admin (created or reset)
  const [newCode, setNewCode] = useState<CaddieWithSetupCode | null>(null);

  // List state
  const [caddies, setCaddies] = useState<Caddie[]>([]);
  const [listError, setListError] = useState<string | null>(null);
  const [shown, setShown] = useState(30); // rows shown before "Show more"
  const [openId, setOpenId] = useState<number | null>(null); // row with its buttons showing
  const [moveTo, setMoveTo] = useState('');

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
      const result = await createCaddie({
        name,
        phone,
        grade,
        priority,
        placement: paperList ? 'BOTTOM_OF_GROUP' : 'TOP_OF_GROUP',
      });
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

  async function handleMove(caddie: Caddie) {
    try {
      await moveCaddie(caddie.id, Number(moveTo));
      setOpenId(null);
      setListError(null);
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

        <View style={styles.switchRow}>
          <View style={{ flex: 1 }}>
            <Text style={styles.label}>Copying the paper list</Text>
            <Text style={styles.muted}>
              {paperList
                ? 'Adds at the bottom of their group, so entering names in paper-list order keeps that order.'
                : 'New caddie: goes to the top of their group with 0 bags.'}
            </Text>
          </View>
          <Switch value={paperList} onValueChange={setPaperList} />
        </View>

        <ErrorText message={formError} />
        <Button label="Create caddie" onPress={handleCreate} loading={saving} disabled={!name || !phone} />
      </View>

      <Text style={styles.cardTitle}>Club list ({caddies.length})</Text>
      <Text style={styles.muted}>
        Tap a caddie to move them on the club list (within their group) or give them a new setup code. A move holds
        until the next 4pm update re-sorts by bags.
      </Text>
      <ErrorText message={listError} />
      {caddies.length === 0 && !listError && <Text style={styles.muted}>No caddies yet.</Text>}
      {caddies.slice(0, shown).map((c) => (
        <View key={c.id} style={styles.card}>
          <Pressable
            style={styles.caddieRow}
            onPress={() => {
              setOpenId(openId === c.id ? null : c.id);
              setMoveTo('');
            }}
            accessibilityRole="button">
            <Text style={styles.position}>{c.listPosition}</Text>
            <View style={{ flex: 1, gap: 2 }}>
              <View style={styles.nameLine}>
                <CaddieTag priority={c.priority} grade={c.grade} />
                <Text style={styles.caddieName}>{c.name}</Text>
              </View>
              <Text style={styles.muted}>
                {GRADE_LABEL[c.grade]} · {c.phone} · {c.passwordSet ? 'Active' : 'Waiting for first login'}
              </Text>
            </View>
          </Pressable>
          {openId === c.id && (
            <View style={styles.rowActions}>
              <Text style={styles.muted}>Move to</Text>
              <TextInput
                value={moveTo}
                onChangeText={setMoveTo}
                keyboardType="number-pad"
                placeholder={String(c.listPosition)}
                style={styles.moveInput}
              />
              <Button label="Move" variant="secondary" disabled={!Number(moveTo)} onPress={() => handleMove(c)} />
              <Button label="New code" variant="secondary" onPress={() => handleReset(c)} />
            </View>
          )}
        </View>
      ))}
      {caddies.length > shown && (
        <Button
          label={`Show ${Math.min(30, caddies.length - shown)} more (${caddies.length - shown} left)`}
          variant="secondary"
          onPress={() => setShown(shown + 30)}
        />
      )}
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
  position: { width: 32, fontSize: 18, fontWeight: '700', color: colors.text, textAlign: 'right' },
  caddieName: { fontSize: 16, fontWeight: '600', color: colors.text },
  nameLine: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  rowActions: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 8 },
  moveInput: {
    width: 64,
    minHeight: 44,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 8,
    fontSize: 16,
    backgroundColor: colors.card,
    color: colors.text,
  },
});
