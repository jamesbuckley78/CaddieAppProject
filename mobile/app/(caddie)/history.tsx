import { StyleSheet, Text, View } from 'react-native';

import { fetchHistory } from '@/api/history';
import { ErrorText } from '@/components/ErrorText';
import { clockTime, dayLabel } from '@/components/format';
import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import { useLiveData } from '@/hooks/useLiveData';

/** The caddie's loops and days worked, week by week (last 8 weeks). */
export default function HistoryScreen() {
  const { data: history, error } = useLiveData(() => fetchHistory(8), 60000);

  return (
    <Screen title="History">
      <ErrorText message={error} />
      {history && (
        <View style={styles.totals}>
          <Total value={history.totalBags} label="bags (last 8 weeks)" />
          <Total value={history.totalDaysWorked} label="days worked" />
        </View>
      )}
      {history && history.weeks.length === 0 && <Text style={styles.muted}>No loops yet.</Text>}

      {history?.weeks.map((week) => (
        <View key={week.weekStart} style={styles.card}>
          <View style={styles.weekHeader}>
            <Text style={styles.weekTitle}>Week of {dayLabel(week.weekStart)}</Text>
            <Text style={styles.weekSummary}>
              {week.bags} {week.bags === 1 ? 'bag' : 'bags'} · {week.daysWorked} {week.daysWorked === 1 ? 'day' : 'days'}
            </Text>
          </View>
          {week.loopRows.length === 0 && <Text style={styles.muted}>Checked in, no bags.</Text>}
          {week.loopRows.map((loop) => (
            <View key={loop.id} style={styles.loopRow}>
              <Text style={styles.loopDay}>{dayLabel(loop.date)}</Text>
              <Text style={[styles.bag, loop.bagType === 'DOUBLE' && styles.double]}>
                {loop.bagType === 'DOUBLE' ? 'Double' : 'Single'}
              </Text>
              <Text style={styles.muted}>
                {loop.sentOutAt ? `out ${clockTime(loop.sentOutAt)}` : ''}
                {loop.returnedAt ? ` · back ${clockTime(loop.returnedAt)}` : ''}
              </Text>
            </View>
          ))}
        </View>
      ))}
    </Screen>
  );
}

function Total({ value, label }: { value: number; label: string }) {
  return (
    <View style={styles.total}>
      <Text style={styles.totalValue}>{value}</Text>
      <Text style={styles.muted}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  muted: { color: colors.muted, fontSize: 13 },
  totals: { flexDirection: 'row', gap: 12 },
  total: {
    flex: 1,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 12,
  },
  totalValue: { fontSize: 24, fontWeight: '700', color: colors.text },
  card: {
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 14,
    gap: 6,
  },
  weekHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', flexWrap: 'wrap' },
  weekTitle: { fontSize: 16, fontWeight: '700', color: colors.text },
  weekSummary: { fontSize: 14, fontWeight: '600', color: colors.primary },
  loopRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 4,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
  },
  loopDay: { width: 96, fontSize: 14, color: colors.text },
  bag: { width: 56, fontSize: 14, fontWeight: '600', color: colors.text },
  double: { color: '#b25a00' },
});
