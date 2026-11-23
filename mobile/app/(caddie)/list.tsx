import { StyleSheet, Text, View } from 'react-native';

import { fetchClubList, fetchMyDay } from '@/api/queue';
import { CaddieTag } from '@/components/CaddieTag';
import { ErrorText } from '@/components/ErrorText';
import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import { useLiveData } from '@/hooks/useLiveData';
import type { ClubList } from '@/types';

const DAY_LETTERS = ['M', 'T', 'W', 'T', 'F'];

/** "2026-11-25" for a JS Date, in the phone's own time zone. */
function isoDate(date: Date) {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** "today", "yesterday" or "on Tue 25 Nov" for the date the figures go up to. */
function whenLabel(countsUpTo: string) {
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  if (countsUpTo === isoDate(today)) return 'today';
  if (countsUpTo === isoDate(yesterday)) return 'yesterday';
  // "T12:00" avoids the date slipping a day when converted to local time
  return 'on ' + new Date(countsUpTo + 'T12:00').toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' });
}

/**
 * The club list as a spreadsheet: position, name with a coloured tag, bags Mon-Fri and the
 * week total. Changes at most twice a day, so a slow refresh is plenty.
 */
export default function ClubListScreen() {
  // The club list only changes at 4pm (or an admin move), so every 15 min plus each time the tab opens is enough
  const { data: list, error } = useLiveData(fetchClubList, 15 * 60000);
  const { data: me } = useLiveData(fetchMyDay, 15 * 60000);

  return (
    <Screen>
      {list && <Summary list={list} />}
      <ErrorText message={error} />

      {list && (
        <View style={styles.table}>
          {/* Header row */}
          <View style={[styles.row, styles.headerRow]}>
            <Text style={[styles.cell, styles.posCell, styles.headerText]}>#</Text>
            <Text style={[styles.nameCell, styles.headerText]}>Name</Text>
            {list.days.map((day, i) => (
              <Text
                key={day}
                style={[styles.cell, styles.headerText, day === list.countsUpTo && styles.latestDay]}>
                {DAY_LETTERS[i]}
              </Text>
            ))}
            <Text style={[styles.cell, styles.totalCell, styles.headerText]}>Wk</Text>
          </View>

          {list.rows.map((row, index) => {
            const isMe = row.caddieId === me?.caddieId;
            return (
              <View
                key={row.caddieId}
                style={[styles.row, index % 2 === 1 && styles.stripe, isMe && styles.me]}>
                <Text style={[styles.cell, styles.posCell, styles.bold]}>{row.position}</Text>
                <View style={styles.nameCell}>
                  <CaddieTag priority={row.priority} grade={row.grade} />
                  <Text style={[styles.name, isMe && styles.bold]} numberOfLines={1}>
                    {row.name}
                  </Text>
                </View>
                {row.bagsByDay.map((bags, i) => {
                  // Days after the last daily update haven't been counted yet: leave them blank
                  const notYet = list.days[i] > list.countsUpTo;
                  return (
                    <Text key={list.days[i]} style={[styles.cell, bags === 0 && styles.zero]}>
                      {notYet ? '' : bags}
                    </Text>
                  );
                })}
                <Text style={[styles.cell, styles.totalCell, styles.bold]}>{row.weekTotal}</Text>
              </View>
            );
          })}

          {/* Total row: the whole club's bags each day */}
          <View style={[styles.row, styles.totalRow]}>
            <Text style={[styles.cell, styles.posCell]} />
            <Text style={[styles.nameCell, styles.bold]}>Total</Text>
            {list.dayTotals.map((total, i) => (
              <Text key={list.days[i]} style={[styles.cell, styles.bold]}>
                {list.days[i] > list.countsUpTo ? '' : total}
              </Text>
            ))}
            <Text style={[styles.cell, styles.totalCell, styles.bold]}>{list.weekTotal}</Text>
          </View>
        </View>
      )}

      <Legend />
    </Screen>
  );
}

function Summary({ list }: { list: ClubList }) {
  return (
    <View style={styles.summary}>
      <Text style={styles.summaryNumber}>{list.totalBags}</Text>
      <View style={{ flex: 1 }}>
        <Text style={styles.summaryLabel}>
          {list.totalBags === 1 ? 'bag' : 'bags'} {whenLabel(list.countsUpTo)} (whole club)
        </Text>
        <Text style={styles.muted}>The order and bag figures update daily at {list.updatesAt}.</Text>
      </View>
    </View>
  );
}

function Legend() {
  return (
    <View style={styles.legend}>
      <View style={styles.legendItem}>
        <CaddieTag priority grade="SINGLE" />
        <Text style={styles.muted}>Priority</Text>
      </View>
      <View style={styles.legendItem}>
        <CaddieTag priority={false} grade="SINGLE" />
        <Text style={styles.muted}>Single</Text>
      </View>
      <View style={styles.legendItem}>
        <CaddieTag priority={false} grade="DOUBLE" />
        <Text style={styles.muted}>Double</Text>
      </View>
      <View style={styles.legendItem}>
        <CaddieTag priority={false} grade="JUNIOR" />
        <Text style={styles.muted}>Junior</Text>
      </View>
      <Text style={styles.muted}>Wk = week total including the weekend.</Text>
    </View>
  );
}

const CELL = 30;

const styles = StyleSheet.create({
  summary: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 12,
  },
  summaryNumber: { fontSize: 36, fontWeight: '700', color: colors.primary },
  summaryLabel: { fontSize: 16, fontWeight: '600', color: colors.text },
  muted: { color: colors.muted, fontSize: 12 },
  table: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border, borderRadius: 8, overflow: 'hidden' },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 34,
    paddingHorizontal: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
  },
  headerRow: { backgroundColor: colors.primary },
  headerText: { color: colors.primaryText, fontWeight: '700' },
  latestDay: { textDecorationLine: 'underline' },
  stripe: { backgroundColor: colors.stripe },
  totalRow: { borderTopWidth: 2, borderTopColor: colors.primary, borderBottomWidth: 0, backgroundColor: colors.stripe },
  me: { backgroundColor: colors.highlight },
  cell: { width: CELL, textAlign: 'center', fontSize: 14, color: colors.text, fontVariant: ['tabular-nums'] },
  posCell: { width: 36, textAlign: 'right', paddingRight: 6 },
  totalCell: { width: 36 },
  nameCell: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: 6, minWidth: 0 },
  name: { flexShrink: 1, fontSize: 14, color: colors.text },
  bold: { fontWeight: '700' },
  zero: { color: colors.border },
  legend: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 12 },
  legendItem: { flexDirection: 'row', alignItems: 'center', gap: 4 },
});
