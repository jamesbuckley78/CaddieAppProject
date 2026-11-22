import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { decideAbsence, fetchInbox } from '@/api/absences';
import { Button } from '@/components/Button';
import { ErrorText } from '@/components/ErrorText';
import { dayLabel } from '@/components/format';
import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import { useLiveData } from '@/hooks/useLiveData';

/** "Not coming in" requests to approve or reject. Issue reports will join them here later. */
export default function InboxScreen() {
  const { data: inbox, error: loadError, reload } = useLiveData(fetchInbox, 30000);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function decide(id: number, approve: boolean) {
    setError(null);
    setBusyId(id);
    try {
      await decideAbsence(id, approve);
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not save');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <Screen title="Inbox">
      <ErrorText message={loadError ?? error} />

      <Text style={styles.heading}>Not coming in - waiting ({inbox?.pending.length ?? 0})</Text>
      {inbox && inbox.pending.length === 0 && <Text style={styles.muted}>Nothing waiting.</Text>}
      {inbox?.pending.map((a) => (
        <View key={a.id} style={styles.card}>
          <Text style={styles.name}>{a.caddieName}</Text>
          <Text style={styles.date}>{dayLabel(a.date)}</Text>
          <Text style={styles.muted}>{a.reason ?? 'No reason given'}</Text>
          <View style={styles.actions}>
            <Button label="Approve" onPress={() => decide(a.id, true)} loading={busyId === a.id} />
            <Button label="Reject" variant="secondary" onPress={() => decide(a.id, false)} disabled={busyId === a.id} />
          </View>
        </View>
      ))}

      <Text style={styles.heading}>Answered in the last 14 days</Text>
      {inbox && inbox.recentlyDecided.length === 0 && <Text style={styles.muted}>None.</Text>}
      {inbox?.recentlyDecided.map((a) => (
        <View key={a.id} style={[styles.card, styles.decided]}>
          <Text style={styles.name}>
            {a.caddieName} · {dayLabel(a.date)}
          </Text>
          <Text style={[styles.result, a.status === 'APPROVED' ? styles.ok : styles.no]}>
            {a.status === 'APPROVED' ? 'Approved' : 'Rejected'}
          </Text>
        </View>
      ))}
    </Screen>
  );
}

const styles = StyleSheet.create({
  heading: { fontSize: 18, fontWeight: '600', color: colors.text },
  muted: { color: colors.muted, fontSize: 14 },
  card: {
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 14,
    gap: 6,
  },
  decided: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  name: { fontSize: 16, fontWeight: '600', color: colors.text },
  date: { fontSize: 15, color: colors.text },
  actions: { flexDirection: 'row', gap: 8, marginTop: 4 },
  result: { fontWeight: '600' },
  ok: { color: colors.primary },
  no: { color: colors.danger },
});
