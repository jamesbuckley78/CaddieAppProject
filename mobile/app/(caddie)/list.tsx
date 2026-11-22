import { StyleSheet, Text, View } from 'react-native';

import { fetchClubList, fetchMyDay } from '@/api/queue';
import { ErrorText } from '@/components/ErrorText';
import { GRADE_LABEL } from '@/components/format';
import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import { useLiveData } from '@/hooks/useLiveData';

/** The club list - recalculated every night, so a slow refresh is plenty. */
export default function ClubListScreen() {
  const { data: rows, error } = useLiveData(fetchClubList, 60000);
  const { data: me } = useLiveData(fetchMyDay, 60000);

  return (
    <Screen title="Club list">
      <Text style={styles.muted}>
        Updated every night at midnight. Priority caddies first, then fewest bags this week.
      </Text>
      <ErrorText message={error} />
      {rows?.map((row) => {
        const isMe = row.caddieId === me?.caddieId;
        return (
          <View key={row.caddieId} style={[styles.row, isMe && styles.me]}>
            <Text style={styles.position}>{row.position}</Text>
            <View style={{ flex: 1 }}>
              <Text style={styles.name}>
                {row.name}
                {isMe ? ' (you)' : ''}
              </Text>
              <Text style={styles.muted}>
                {GRADE_LABEL[row.grade]} · {row.bagsThisWeek} {row.bagsThisWeek === 1 ? 'bag' : 'bags'} this week
              </Text>
            </View>
            {row.priority && <Text style={styles.badge}>P</Text>}
          </View>
        );
      })}
    </Screen>
  );
}

const styles = StyleSheet.create({
  muted: { color: colors.muted, fontSize: 14 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    padding: 12,
  },
  me: { backgroundColor: colors.highlight, borderColor: colors.primary },
  position: { width: 36, fontSize: 18, fontWeight: '700', color: colors.text, textAlign: 'right' },
  name: { fontSize: 16, fontWeight: '600', color: colors.text },
  badge: {
    backgroundColor: colors.primary,
    color: colors.primaryText,
    fontWeight: '700',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 4,
    overflow: 'hidden',
  },
});
