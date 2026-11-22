import { StyleSheet, Text, View } from 'react-native';

import { fetchBoard } from '@/api/queue';
import { ErrorText } from '@/components/ErrorText';
import { clockTime, statusText } from '@/components/format';
import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import { useLiveData } from '@/hooks/useLiveData';
import type { BoardEntry } from '@/types';

export default function AdminBoard() {
  const { data: board, error } = useLiveData(fetchBoard, 10000);
  const entries = board?.entries ?? [];

  // One queue, filtered two ways (CLAUDE.md). Juniors can take singles and doubles, so they
  // appear in both tables. `.filter()` keeps the items where the function returns true.
  const waiting = entries.filter((e) => e.status !== 'OUT');
  const singles = waiting.filter((e) => e.grade !== 'DOUBLE');
  const doubles = waiting.filter((e) => e.grade !== 'SINGLE');
  const out = entries.filter((e) => e.status === 'OUT');

  return (
    <Screen title="Board">
      <View style={styles.summary}>
        <Text style={styles.summaryText}>{board?.totalBagsToday ?? 0} bags today</Text>
        <Text style={styles.muted}>
          {waiting.length} waiting · {out.length} out on course · refreshes every 10 s
        </Text>
      </View>
      <ErrorText message={error} />

      {/* flexWrap: side by side on a tablet/PC, stacked on a phone */}
      <View style={styles.tables}>
        <Table title="Singles" entries={singles} />
        <Table title="Doubles" entries={doubles} />
      </View>

      {out.length > 0 && <Table title="Out on course" entries={out} />}
    </Screen>
  );
}

function Table({ title, entries }: { title: string; entries: BoardEntry[] }) {
  return (
    <View style={styles.table}>
      <Text style={styles.heading}>
        {title} ({entries.length})
      </Text>
      {entries.length === 0 && <Text style={styles.muted}>Nobody here yet.</Text>}
      {entries.map((e) => (
        <Row key={e.caddieId} entry={e} />
      ))}
    </View>
  );
}

function Row({ entry }: { entry: BoardEntry }) {
  const away = entry.status === 'FOOD' || entry.status === 'BACK_LATER';
  return (
    <View style={[styles.row, away && styles.rowAway]}>
      <Text style={styles.position}>{entry.lineNumber ?? '–'}</Text>
      <View style={{ flex: 1, gap: 2 }}>
        <View style={styles.nameLine}>
          <Text style={styles.name}>{entry.name}</Text>
          {entry.priority && <Text style={styles.badge}>P</Text>}
          {entry.grade === 'JUNIOR' && <Text style={[styles.badge, styles.junior]}>J</Text>}
        </View>
        <Text style={styles.muted}>
          {entry.bagsThisWeek} {entry.bagsThisWeek === 1 ? 'bag' : 'bags'} this week · in since{' '}
          {clockTime(entry.checkedInAt)}
        </Text>
      </View>
      <Text style={[styles.status, away && styles.statusAway]}>{statusText(entry.status, entry.backAt)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  summary: { gap: 2 },
  summaryText: { fontSize: 20, fontWeight: '700', color: colors.text },
  muted: { color: colors.muted, fontSize: 13 },
  tables: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  table: {
    flexGrow: 1,
    flexBasis: 320,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 12,
    gap: 8,
  },
  heading: { fontSize: 18, fontWeight: '600', color: colors.text },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  rowAway: { opacity: 0.75 },
  position: { width: 28, fontSize: 18, fontWeight: '700', color: colors.text, textAlign: 'right' },
  nameLine: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  name: { fontSize: 16, fontWeight: '600', color: colors.text },
  badge: {
    backgroundColor: colors.primary,
    color: colors.primaryText,
    fontSize: 11,
    fontWeight: '700',
    paddingHorizontal: 6,
    paddingVertical: 1,
    borderRadius: 4,
    overflow: 'hidden',
  },
  junior: { backgroundColor: colors.muted },
  status: { fontSize: 13, fontWeight: '600', color: colors.primary },
  statusAway: { color: colors.danger },
});
