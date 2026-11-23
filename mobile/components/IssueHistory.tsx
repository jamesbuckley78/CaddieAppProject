import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { fetchIssueSummary } from '@/api/issues';
import type { IssueSummary, IssueType } from '@/types';

import { Button } from './Button';
import { ErrorText } from './ErrorText';
import { addDays, clockTime, dayLabel, ISSUE_LABEL, todayIso } from './format';
import { colors } from './theme';

const TOP_CADDIES = 5; // how many names "Most reports" shows before "Show all"

/**
 * Every issue report for a week or all time (Inbox tab): totals per type, who reports the
 * most, and the list with who resolved each one.
 *
 * @param refreshKey changes when the Inbox resolves an issue, so this reloads too
 */
export function IssueHistory({ refreshKey }: { refreshKey: number }) {
  const [allTime, setAllTime] = useState(false);
  const [day, setDay] = useState(todayIso()); // any day in the week shown
  const [summary, setSummary] = useState<IssueSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showAllCaddies, setShowAllCaddies] = useState(false);

  // Loads when the tab opens and whenever the week, the all-time switch or refreshKey changes
  useFocusEffect(
    useCallback(() => {
      let current = true; // ignore a slow answer for a week we've already moved away from
      fetchIssueSummary(allTime ? undefined : day)
        .then((s) => {
          if (current) {
            setSummary(s);
            setError(null);
          }
        })
        .catch((e) => current && setError(e instanceof Error ? e.message : 'Could not load'));
      return () => {
        current = false;
      };
    }, [allTime, day, refreshKey]),
  );

  const weekStart = summary?.weekStart ?? null;
  const isThisWeek = weekStart !== null && todayIso() < addDays(weekStart, 7);
  const caddies = summary ? (showAllCaddies ? summary.byCaddie : summary.byCaddie.slice(0, TOP_CADDIES)) : [];
  // Object.entries turns {RANGER: 3, ...} into [['RANGER', 3], ...]; biggest first
  const types = summary
    ? (Object.entries(summary.byType) as [IssueType, number][]).sort((a, b) => b[1] - a[1])
    : [];

  return (
    <View style={styles.wrap}>
      <Text style={styles.heading}>Issue history</Text>
      <View style={styles.segments}>
        {[false, true].map((all) => (
          <Pressable
            key={String(all)}
            onPress={() => setAllTime(all)}
            accessibilityRole="tab"
            accessibilityState={{ selected: allTime === all }}
            style={[styles.segment, allTime === all && styles.segmentOn]}>
            <Text style={[styles.segmentText, allTime === all && styles.segmentTextOn]}>
              {all ? 'All time' : 'By week'}
            </Text>
          </Pressable>
        ))}
      </View>
      {!allTime && (
        <View style={styles.weekNav}>
          <Button label="◀ Earlier" variant="secondary" onPress={() => setDay(addDays(weekStart ?? day, -7))} />
          <Text style={styles.weekTitle}>{weekStart ? `Week of ${dayLabel(weekStart)}` : ''}</Text>
          <Button
            label="Later ▶"
            variant="secondary"
            disabled={isThisWeek}
            onPress={() => setDay(addDays(weekStart ?? day, 7))}
          />
        </View>
      )}
      <ErrorText message={error} />

      {summary && (
        <>
          <View style={styles.card}>
            <Text style={styles.total}>
              {summary.total} {summary.total === 1 ? 'report' : 'reports'}
              {allTime ? ' in total' : ' this week'}
            </Text>
            {types.map(([type, count]) => (
              <Text key={type} style={styles.line}>
                {ISSUE_LABEL[type]}: {count}
              </Text>
            ))}
          </View>

          {summary.byCaddie.length > 0 && (
            <View style={styles.card}>
              <Text style={styles.subheading}>Most reports</Text>
              {caddies.map((c, i) => (
                <View key={c.caddieId} style={styles.countRow}>
                  <Text style={styles.line}>
                    {i + 1}. {c.name}
                  </Text>
                  <Text style={styles.count}>{c.reports}</Text>
                </View>
              ))}
              {summary.byCaddie.length > TOP_CADDIES && (
                <Button
                  label={showAllCaddies ? 'Show fewer' : `Show all ${summary.byCaddie.length}`}
                  variant="secondary"
                  onPress={() => setShowAllCaddies(!showAllCaddies)}
                />
              )}
            </View>
          )}

          {summary.issues.map((i) => (
            <View key={i.id} style={[styles.card, !i.resolved && styles.openCard]}>
              <Text style={styles.subheading}>
                {ISSUE_LABEL[i.type]}
                {i.hole ? ` · hole ${i.hole}` : ''}
              </Text>
              <Text style={styles.muted}>
                {i.caddieName} · {when(i.createdAt)}
              </Text>
              {i.message && <Text style={styles.line}>{i.message}</Text>}
              <Text style={[styles.muted, !i.resolved && styles.openText]}>
                {i.resolved ? `Resolved by ${i.resolvedBy ?? 'an admin'} · ${when(i.resolvedAt)}` : 'Still open'}
              </Text>
            </View>
          ))}
          {allTime && summary.issues.length < summary.total && (
            <Text style={styles.muted}>Showing the newest {summary.issues.length}. The counts above include all of them.</Text>
          )}
        </>
      )}
    </View>
  );
}

/** "14:05 Mon 23 Nov" in the phone's time zone. */
function when(iso: string | null) {
  if (!iso) return '';
  const date = new Date(iso).toLocaleDateString([], { weekday: 'short', day: 'numeric', month: 'short' });
  return `${clockTime(iso)} ${date}`;
}

const styles = StyleSheet.create({
  wrap: { gap: 10 },
  heading: { fontSize: 18, fontWeight: '600', color: colors.text },
  subheading: { fontSize: 16, fontWeight: '600', color: colors.text },
  segments: { flexDirection: 'row', backgroundColor: colors.card, borderRadius: 8, borderWidth: 1, borderColor: colors.border },
  segment: { flex: 1, minHeight: 44, alignItems: 'center', justifyContent: 'center', borderRadius: 8 },
  segmentOn: { backgroundColor: colors.primary },
  segmentText: { fontSize: 14, fontWeight: '600', color: colors.text },
  segmentTextOn: { color: colors.primaryText },
  weekNav: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  weekTitle: { fontSize: 16, fontWeight: '600', color: colors.text, flexShrink: 1, textAlign: 'center' },
  card: {
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 14,
    gap: 4,
  },
  openCard: { borderLeftWidth: 4, borderLeftColor: colors.danger },
  total: { fontSize: 20, fontWeight: '700', color: colors.text },
  line: { fontSize: 15, color: colors.text },
  countRow: { flexDirection: 'row', justifyContent: 'space-between' },
  count: { fontSize: 15, fontWeight: '700', color: colors.text },
  muted: { color: colors.muted, fontSize: 14 },
  openText: { color: colors.danger, fontWeight: '600' },
});
