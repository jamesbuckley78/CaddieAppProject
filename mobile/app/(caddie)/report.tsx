import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { reportIssue } from '@/api/issues';
import { Button } from '@/components/Button';
import { ErrorText } from '@/components/ErrorText';
import { ISSUE_LABEL } from '@/components/format';
import { Screen } from '@/components/Screen';
import { TextField } from '@/components/TextField';
import { colors } from '@/components/theme';
import type { IssueType } from '@/types';

const TYPES = Object.keys(ISSUE_LABEL) as IssueType[];

/** Tell the caddie master about a problem. It shows as a banner on their board. */
export default function ReportScreen() {
  const [type, setType] = useState<IssueType | null>(null);
  const [hole, setHole] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const holeNumber = hole ? Number(hole) : null;
  const holeOk = holeNumber === null || (Number.isInteger(holeNumber) && holeNumber >= 1 && holeNumber <= 18);

  async function send() {
    if (!type) return;
    setError(null);
    setBusy(true);
    try {
      await reportIssue(type, holeNumber, message);
      setSent(true);
      setType(null);
      setHole('');
      setMessage('');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not send');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen title="Report an issue">
      {sent && (
        <View style={[styles.card, styles.sentCard]}>
          <Text style={styles.big}>Sent</Text>
          <Text style={styles.muted}>The caddie master will see it on their board.</Text>
          <Button label="Report another" variant="secondary" onPress={() => setSent(false)} />
        </View>
      )}

      {!sent && (
        <View style={styles.card}>
          <Text style={styles.label}>What's wrong?</Text>
          {TYPES.map((t) => (
            <Pressable
              key={t}
              onPress={() => setType(t)}
              accessibilityRole="radio"
              accessibilityState={{ selected: type === t }}
              style={[styles.choice, type === t && styles.choiceSelected]}>
              <Text style={[styles.choiceText, type === t && styles.choiceTextSelected]}>{ISSUE_LABEL[t]}</Text>
            </Pressable>
          ))}
          <TextField label="Hole (optional)" value={hole} onChangeText={setHole} keyboardType="number-pad" placeholder="1-18" />
          {!holeOk && <Text style={styles.warn}>Hole must be a number from 1 to 18.</Text>}
          <TextField label="Details (optional)" value={message} onChangeText={setMessage} placeholder="e.g. 4-ball on 7 very slow" />
          <ErrorText message={error} />
          <Button label="Send" onPress={send} loading={busy} disabled={!type || !holeOk} />
        </View>
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
  sentCard: { backgroundColor: colors.highlight },
  big: { fontSize: 22, fontWeight: '700', color: colors.text },
  label: { fontSize: 14, fontWeight: '500', color: colors.text },
  muted: { color: colors.muted, fontSize: 14 },
  warn: { color: colors.danger, fontSize: 14 },
  choice: {
    minHeight: 44,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    justifyContent: 'center',
  },
  choiceSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  choiceText: { color: colors.text, fontWeight: '500' },
  choiceTextSelected: { color: colors.primaryText },
});
