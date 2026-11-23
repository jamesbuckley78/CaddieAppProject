import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Switch, Text, TextInput, View } from 'react-native';

import {
  addBag,
  bringBack,
  createCaddie,
  editCaddie,
  listCaddies,
  markLeft,
  moveCaddie,
  resetSetupCode,
} from '@/api/caddies';
import { Button } from '@/components/Button';
import { CaddieTag } from '@/components/CaddieTag';
import { ErrorText } from '@/components/ErrorText';
import { addDays, dayLabel, GRADE_LABEL, todayIso } from '@/components/format';
import { Screen } from '@/components/Screen';
import { TextField } from '@/components/TextField';
import { colors } from '@/components/theme';
import type { BagType, Caddie, CaddieWithSetupCode, Grade } from '@/types';

const GRADES: Grade[] = ['SINGLE', 'DOUBLE', 'JUNIOR'];

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
  const [notice, setNotice] = useState<string | null>(null); // e.g. "Bag added for Sean"
  const [shown, setShown] = useState(30); // rows shown before "Show more"
  const [openId, setOpenId] = useState<number | null>(null); // row with its buttons showing

  const onList = caddies.filter((c) => c.active);
  const left = caddies.filter((c) => !c.active);

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

  /** Runs one action on a caddie, then reloads the list. Returns whether it worked. */
  async function run(action: () => Promise<unknown>, done?: string) {
    setListError(null);
    setNotice(null);
    try {
      await action();
      setNotice(done ?? null);
      await refresh();
      return true;
    } catch (e) {
      setListError(errorMessage(e));
      return false;
    }
  }

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
        <GradePicker grade={grade} priority={priority} onGrade={setGrade} onPriority={setPriority} />

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

      <Text style={styles.cardTitle}>Club list ({onList.length})</Text>
      <Text style={styles.muted}>
        Tap a caddie to move them, edit their details or grade, add a bag by hand, give a new setup code or mark them
        as left. A move holds until the next 4pm update re-sorts by bags.
      </Text>
      <ErrorText message={listError} />
      {notice && <Text style={styles.notice}>{notice}</Text>}
      {onList.length === 0 && !listError && <Text style={styles.muted}>No caddies yet.</Text>}
      {onList.slice(0, shown).map((c) => (
        <View key={c.id} style={styles.card}>
          <Pressable
            style={styles.caddieRow}
            onPress={() => setOpenId(openId === c.id ? null : c.id)}
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
            // `key` makes React start this panel fresh (empty fields) for each caddie
            <CaddieActions
              key={c.id}
              caddie={c}
              run={run}
              onCode={setNewCode}
              onClose={() => setOpenId(null)}
            />
          )}
        </View>
      ))}
      {onList.length > shown && (
        <Button
          label={`Show ${Math.min(30, onList.length - shown)} more (${onList.length - shown} left)`}
          variant="secondary"
          onPress={() => setShown(shown + 30)}
        />
      )}

      {left.length > 0 && (
        <>
          <Text style={styles.cardTitle}>Left the club ({left.length})</Text>
          <Text style={styles.muted}>
            Not on the club list and can't log in. Their bags and history are kept. Bringing someone back puts them at
            the top of their group; their old password works again.
          </Text>
          {left.map((c) => (
            <View key={c.id} style={[styles.card, styles.leftRow]}>
              <View style={{ flex: 1 }}>
                <Text style={styles.caddieName}>{c.name}</Text>
                <Text style={styles.muted}>
                  {GRADE_LABEL[c.grade]} · {c.phone}
                </Text>
              </View>
              <Button label="Bring back" variant="secondary" onPress={() => run(() => bringBack(c.id), `${c.name} is back on the list`)} />
            </View>
          ))}
        </>
      )}
    </Screen>
  );
}

type GradePickerProps = {
  grade: Grade;
  priority: boolean;
  onGrade: (grade: Grade) => void;
  onPriority: (priority: boolean) => void;
};

/** Grade buttons and the priority switch, used when adding and when editing a caddie. */
function GradePicker({ grade, priority, onGrade, onPriority }: GradePickerProps) {
  return (
    <>
      <Text style={styles.label}>Grade</Text>
      <View style={styles.row}>
        {/* .map() turns each item of an array into a piece of UI, like a Java stream.
            `key` lets React tell the items apart when the list changes. */}
        {GRADES.map((g) => (
          <Pressable
            key={g}
            onPress={() => {
              onGrade(g);
              if (g === 'JUNIOR') onPriority(false); // juniors can't be priority
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
        <Switch value={priority} onValueChange={onPriority} disabled={grade === 'JUNIOR'} />
      </View>
    </>
  );
}

type CaddieActionsProps = {
  caddie: Caddie;
  run: (action: () => Promise<unknown>, done?: string) => Promise<boolean>;
  onCode: (code: CaddieWithSetupCode) => void;
  onClose: () => void;
};

/** The panel under a tapped caddie. `mode` picks which small form is showing. */
function CaddieActions({ caddie, run, onCode, onClose }: CaddieActionsProps) {
  const [mode, setMode] = useState<'menu' | 'edit' | 'bag'>('menu');
  const [moveTo, setMoveTo] = useState('');
  const [confirmLeft, setConfirmLeft] = useState(false);
  // Edit form, starting from their current details
  const [name, setName] = useState(caddie.name);
  const [phone, setPhone] = useState(caddie.phone);
  const [grade, setGrade] = useState<Grade>(caddie.grade);
  const [priority, setPriority] = useState(caddie.priority);
  // Add a bag: today or one of the 6 days before
  const today = todayIso();
  const [bagDay, setBagDay] = useState(today);
  const lastWeek = [0, -1, -2, -3, -4, -5, -6].map((n) => addDays(today, n));

  async function act(action: () => Promise<unknown>, done?: string) {
    if (await run(action, done)) onClose();
  }

  if (mode === 'edit') {
    const groupChanges = (grade === 'JUNIOR') !== (caddie.grade === 'JUNIOR') || priority !== caddie.priority;
    return (
      <View style={styles.form}>
        <TextField label="Name" value={name} onChangeText={setName} />
        <TextField label="Phone number" value={phone} onChangeText={setPhone} keyboardType="phone-pad" />
        <GradePicker grade={grade} priority={priority} onGrade={setGrade} onPriority={setPriority} />
        {groupChanges && (
          <Text style={styles.muted}>They'll move to the top of their new group until the next 4pm update.</Text>
        )}
        <View style={styles.rowActions}>
          <Button
            label="Save"
            disabled={!name || !phone}
            onPress={() => act(() => editCaddie(caddie.id, { name, phone, grade, priority }), `${name} updated`)}
          />
          <Button label="Cancel" variant="secondary" onPress={() => setMode('menu')} />
        </View>
      </View>
    );
  }

  if (mode === 'bag') {
    const record = (bag: BagType) =>
      act(
        () => addBag(caddie.id, bagDay, bag),
        `${bag === 'DOUBLE' ? 'Double' : 'Single'} added for ${caddie.name} on ${dayLabel(bagDay)}`,
      );
    return (
      <View style={styles.form}>
        <Text style={styles.label}>Which day?</Text>
        <View style={styles.dayRow}>
          {lastWeek.map((d) => (
            <Pressable
              key={d}
              onPress={() => setBagDay(d)}
              accessibilityRole="radio"
              accessibilityState={{ selected: bagDay === d }}
              style={[styles.dayChip, bagDay === d && styles.choiceSelected]}>
              <Text style={[styles.choiceText, bagDay === d && styles.choiceTextSelected]}>
                {d === today ? 'Today' : dayLabel(d)}
              </Text>
            </Pressable>
          ))}
        </View>
        <Text style={styles.muted}>It counts straight away, like a bag given out on the board.</Text>
        <View style={styles.rowActions}>
          <Button label="Add a single" onPress={() => record('SINGLE')} />
          <Button label="Add a double" onPress={() => record('DOUBLE')} />
          <Button label="Cancel" variant="secondary" onPress={() => setMode('menu')} />
        </View>
      </View>
    );
  }

  return (
    <View style={styles.form}>
      <View style={styles.rowActions}>
        <Text style={styles.muted}>Move to</Text>
        <TextInput
          value={moveTo}
          onChangeText={setMoveTo}
          keyboardType="number-pad"
          placeholder={String(caddie.listPosition)}
          style={styles.moveInput}
        />
        <Button
          label="Move"
          variant="secondary"
          disabled={!Number(moveTo)}
          onPress={() => act(() => moveCaddie(caddie.id, Number(moveTo)))}
        />
      </View>
      <View style={styles.rowActions}>
        <Button label="Edit details" variant="secondary" onPress={() => setMode('edit')} />
        <Button label="Add a bag" variant="secondary" onPress={() => setMode('bag')} />
        <Button
          label="New code"
          variant="secondary"
          onPress={() => act(async () => onCode(await resetSetupCode(caddie.id)))}
        />
        <Button
          label={confirmLeft ? 'Tap again: they have left' : 'Mark as left'}
          variant="secondary"
          onPress={() =>
            confirmLeft ? act(() => markLeft(caddie.id), `${caddie.name} marked as left`) : setConfirmLeft(true)
          }
        />
      </View>
    </View>
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
  notice: { color: colors.primary, fontSize: 14, fontWeight: '600' },
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
  dayRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  dayChip: {
    minHeight: 44,
    paddingHorizontal: 10,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  caddieRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  leftRow: { flexDirection: 'row', alignItems: 'center' },
  position: { width: 32, fontSize: 18, fontWeight: '700', color: colors.text, textAlign: 'right' },
  caddieName: { fontSize: 16, fontWeight: '600', color: colors.text },
  nameLine: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  form: { gap: 10 },
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
