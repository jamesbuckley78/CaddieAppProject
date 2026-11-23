import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { ScrollView, StyleSheet, Switch, Text, View } from 'react-native';

import { fetchAttendance } from '@/api/attendance';
import { Button } from '@/components/Button';
import { CaddieTag } from '@/components/CaddieTag';
import { ErrorText } from '@/components/ErrorText';
import { addDays, clockTime, dayLabel, todayIso } from '@/components/format';
import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import type { AttendanceWeek } from '@/types';

const DAY_LETTERS = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

/**
 * Who came in each day of a week (check-in time), days worked per caddie and the club's daily
 * totals. A calendar view can come later if it's wanted.
 */
export default function AttendanceScreen() {
  const [day, setDay] = useState(todayIso()); // any day in the week shown
  const [onlyIn, setOnlyIn] = useState(true); // hide caddies who didn't come in that week
  const [week, setWeek] = useState<AttendanceWeek | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Loads when the tab opens and whenever `day` changes (the callback is rebuilt for each week)
  useFocusEffect(
    useCallback(() => {
      let current = true; // ignore a slow answer for a week we've already moved away from
      fetchAttendance(day)
        .then((w) => {
          if (current) {
            setWeek(w);
            setError(null);
          }
        })
        .catch((e) => current && setError(e instanceof Error ? e.message : 'Could not load'));
      return () => {
        current = false;
      };
    }, [day]),
  );

  function changeWeek(days: number) {
    setDay(addDays(week?.weekStart ?? day, days));
  }

  const rows = week ? week.rows.filter((r) => !onlyIn || r.daysWorked > 0) : [];
  const isThisWeek = week ? todayIso() >= week.days[0] && todayIso() <= week.days[6] : true;

  return (
    <Screen title="Attendance">
      <View style={styles.weekNav}>
        <Button label="◀ Earlier" variant="secondary" onPress={() => changeWeek(-7)} />
        <Text style={styles.weekTitle}>{week ? `Week of ${dayLabel(week.weekStart)}` : ''}</Text>
        <Button label="Later ▶" variant="secondary" disabled={isThisWeek} onPress={() => changeWeek(7)} />
      </View>
      <ErrorText message={error} />

      {week && (
        <>
          <Text style={styles.summary}>
            {week.caddiesIn} {week.caddiesIn === 1 ? 'caddie' : 'caddies'} came in this week
          </Text>
          <View style={styles.switchRow}>
            <Text style={styles.muted}>Only show caddies who came in</Text>
            <Switch value={onlyIn} onValueChange={setOnlyIn} />
          </View>

          {/* Seven day columns don't fit a phone's width, so the table scrolls sideways */}
          <ScrollView horizontal>
            <View style={styles.table}>
              <View style={[styles.row, styles.headerRow]}>
                <Text style={[styles.nameCell, styles.headerText]}>Name</Text>
                {week.days.map((d, i) => (
                  <Text key={d} style={[styles.cell, styles.headerText]}>
                    {DAY_LETTERS[i]} {d.slice(8)}
                  </Text>
                ))}
                <Text style={[styles.daysCell, styles.headerText]}>Days</Text>
              </View>
              {rows.map((r, index) => (
                <View key={r.caddieId} style={[styles.row, index % 2 === 1 && styles.stripe]}>
                  <View style={styles.nameCell}>
                    <CaddieTag priority={r.priority} grade={r.grade} />
                    <Text style={styles.name} numberOfLines={1}>
                      {r.name}
                    </Text>
                  </View>
                  {r.checkedInAt.map((t, i) => (
                    <Text key={week.days[i]} style={[styles.cell, !t && styles.absent]}>
                      {t ? clockTime(t) : '-'}
                    </Text>
                  ))}
                  <Text style={[styles.daysCell, styles.bold]}>{r.daysWorked}</Text>
                </View>
              ))}
              <View style={[styles.row, styles.totalRow]}>
                <Text style={[styles.nameCell, styles.bold]}>Caddies in</Text>
                {week.dayTotals.map((total, i) => (
                  <Text key={week.days[i]} style={[styles.cell, styles.bold]}>
                    {total}
                  </Text>
                ))}
                <Text style={styles.daysCell} />
              </View>
            </View>
          </ScrollView>
          {rows.length === 0 && <Text style={styles.muted}>Nobody came in this week.</Text>}
          <Text style={styles.muted}>Times are each caddie's first "I'm here" of the day.</Text>
        </>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  weekNav: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  weekTitle: { fontSize: 16, fontWeight: '600', color: colors.text, flexShrink: 1, textAlign: 'center' },
  summary: { fontSize: 18, fontWeight: '700', color: colors.text },
  switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  muted: { color: colors.muted, fontSize: 13 },
  table: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border, borderRadius: 8 },
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
  stripe: { backgroundColor: colors.stripe },
  totalRow: { borderTopWidth: 2, borderTopColor: colors.primary, borderBottomWidth: 0, backgroundColor: colors.stripe },
  nameCell: { width: 170, flexDirection: 'row', alignItems: 'center', gap: 6 },
  name: { flexShrink: 1, fontSize: 14, color: colors.text },
  cell: { width: 56, textAlign: 'center', fontSize: 13, color: colors.text, fontVariant: ['tabular-nums'] },
  daysCell: { width: 44, textAlign: 'center', fontSize: 14, color: colors.text },
  absent: { color: colors.border },
  bold: { fontWeight: '700' },
});
