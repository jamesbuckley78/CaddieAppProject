import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { cancelAbsence, fetchMyAbsences, requestAbsence } from '@/api/absences';
import { Button } from '@/components/Button';
import { ErrorText } from '@/components/ErrorText';
import { addDays, dayLabel } from '@/components/format';
import { Screen } from '@/components/Screen';
import { TextField } from '@/components/TextField';
import { colors } from '@/components/theme';
import { useLiveData } from '@/hooks/useLiveData';
import type { AbsenceStatus } from '@/types';

const DAYS_SHOWN = 21; // how far ahead the date buttons go

const STATUS_LABEL: Record<AbsenceStatus, string> = {
  PENDING: 'Waiting for the caddie master',
  APPROVED: 'Approved',
  REJECTED: 'Not approved',
};

export default function AbsenceScreen() {
  const { data, error: loadError, reload } = useLiveData(fetchMyAbsences, 60000);
  const [date, setDate] = useState<string | null>(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState<string | null>(null);

  // Dates already asked about can't be picked again
  const taken = new Set(data?.requests.map((r) => r.date) ?? []);
  // Array.from({ length: n }, fn) makes a list of n items - here the next 21 pickable days
  const choices = data ? Array.from({ length: DAYS_SHOWN }, (_, i) => addDays(data.earliestDate, i)) : [];

  async function handleSend() {
    if (!date) return;
    setError(null);
    setBusy(true);
    try {
      await requestAbsence(date, reason);
      setSent(`Sent - ${dayLabel(date)}. The caddie master will approve or reject it.`);
      setDate(null);
      setReason('');
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not send');
    } finally {
      setBusy(false);
    }
  }

  async function handleCancel(id: number) {
    setError(null);
    try {
      await cancelAbsence(id);
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not cancel');
    }
  }

  return (
    <Screen title="Not coming in">
      <ErrorText message={loadError} />
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Pick a day</Text>
        <Text style={styles.muted}>At least 24 hours ahead. The caddie master approves or rejects it.</Text>
        <View style={styles.dates}>
          {choices.map((d) => (
            <Pressable
              key={d}
              onPress={() => setDate(d)}
              disabled={taken.has(d)}
              accessibilityRole="radio"
              accessibilityState={{ selected: date === d, disabled: taken.has(d) }}
              style={[styles.date, date === d && styles.dateOn, taken.has(d) && styles.dateTaken]}>
              <Text style={[styles.dateText, date === d && styles.dateTextOn]}>{dayLabel(d)}</Text>
            </Pressable>
          ))}
        </View>
        <TextField
          label="Reason (optional)"
          value={reason}
          onChangeText={setReason}
          placeholder="e.g. exam, family event"
          maxLength={500}
        />
        <ErrorText message={error} />
        {sent && <Text style={styles.sent}>{sent}</Text>}
        <Button label={date ? `Send for ${dayLabel(date)}` : 'Pick a day first'} onPress={handleSend} loading={busy} disabled={!date} />
      </View>

      <Text style={styles.cardTitle}>Your requests</Text>
      {data && data.requests.length === 0 && <Text style={styles.muted}>None coming up.</Text>}
      {data?.requests.map((r) => (
        <View key={r.id} style={[styles.card, styles.requestRow]}>
          <View style={{ flex: 1, gap: 2 }}>
            <Text style={styles.requestDate}>{dayLabel(r.date)}</Text>
            {r.reason && <Text style={styles.muted}>{r.reason}</Text>}
            <Text style={[styles.status, r.status === 'APPROVED' && styles.ok, r.status === 'REJECTED' && styles.no]}>
              {STATUS_LABEL[r.status]}
            </Text>
          </View>
          {r.status === 'PENDING' && <Button label="Cancel" variant="secondary" onPress={() => handleCancel(r.id)} />}
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
  cardTitle: { fontSize: 18, fontWeight: '600', color: colors.text },
  muted: { color: colors.muted, fontSize: 14 },
  dates: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  date: {
    minHeight: 44,
    minWidth: 96,
    paddingHorizontal: 10,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dateOn: { backgroundColor: colors.primary, borderColor: colors.primary },
  dateTaken: { opacity: 0.35 },
  dateText: { color: colors.text, fontWeight: '500' },
  dateTextOn: { color: colors.primaryText },
  sent: { color: colors.primary, fontWeight: '600' },
  requestRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  requestDate: { fontSize: 16, fontWeight: '600', color: colors.text },
  status: { fontSize: 14, fontWeight: '600', color: '#b25a00' },
  ok: { color: colors.primary },
  no: { color: colors.danger },
});
