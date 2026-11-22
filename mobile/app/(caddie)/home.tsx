import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { getCurrentPosition } from '@/api/location';
import { checkIn, checkOut, fetchMyDay, setStatus } from '@/api/queue';
import { Button } from '@/components/Button';
import { ErrorText } from '@/components/ErrorText';
import { ordinal, statusText } from '@/components/format';
import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import { useAuth } from '@/hooks/useAuth';
import { useLiveData } from '@/hooks/useLiveData';
import type { MyDay, QueueStatus } from '@/types';

const BACK_IN_OPTIONS = [15, 30, 45, 60, 90];

export default function CaddieHome() {
  const { user } = useAuth();
  const { data: day, error: loadError, setData } = useLiveData(fetchMyDay, 15000);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [choosingTime, setChoosingTime] = useState(false);

  // Runs one action (check in, change status...) and shows the updated day it returns.
  // `action` is a function that returns a Promise<MyDay> - functions can be passed around
  // like any other value in TypeScript.
  async function run(action: () => Promise<MyDay>) {
    setError(null);
    setBusy(true);
    try {
      setData(await action());
      setChoosingTime(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  }

  function handleCheckIn() {
    run(async () => {
      const position = day?.locationRequired ? await getCurrentPosition() : null;
      return checkIn(position);
    });
  }

  function handleStatus(status: QueueStatus, minutes?: number) {
    run(() => setStatus(status, minutes));
  }

  if (!user) return null;
  if (!day) {
    return (
      <Screen title={`Hi ${user.name}`}>
        <ErrorText message={loadError} />
        {!loadError && <Text style={styles.muted}>Loading…</Text>}
      </Screen>
    );
  }

  return (
    <Screen title={`Hi ${user.name}`}>
      {!day.checkedIn ? (
        <View style={styles.card}>
          <Text style={styles.big}>Not checked in</Text>
          <Text style={styles.muted}>
            {day.locationRequired
              ? 'You need to be at the club. Your location is checked once, when you tap the button.'
              : "Tap when you're at the club and ready to work."}
          </Text>
          <Button label="I'm here - available" onPress={handleCheckIn} loading={busy} />
        </View>
      ) : (
        <View style={styles.card}>
          {/* The big number - only their own, never who else is here */}
          {day.status === 'OUT' ? (
            <Text style={styles.big}>Out on course</Text>
          ) : (
            <Text style={styles.big}>
              You're {day.lineNumber ? ordinal(day.lineNumber) : '-'} in line
            </Text>
          )}
          <Text style={styles.status}>{statusText(day.status, day.backAt)}</Text>

          {day.status !== 'OUT' && day.status !== 'CALLED' && (
            <>
              <View style={styles.row}>
                <Choice label="Available" selected={day.status === 'AVAILABLE'} onPress={() => handleStatus('AVAILABLE')} />
                <Choice label="Getting food" selected={day.status === 'FOOD'} onPress={() => handleStatus('FOOD')} />
                <Choice
                  label="Will be back"
                  selected={day.status === 'BACK_LATER' || choosingTime}
                  onPress={() => setChoosingTime(!choosingTime)}
                />
              </View>
              {choosingTime && (
                <View style={styles.timeBox}>
                  <Text style={styles.muted}>Back in roughly…</Text>
                  <View style={styles.row}>
                    {BACK_IN_OPTIONS.map((m) => (
                      <Choice key={m} label={`${m} min`} onPress={() => handleStatus('BACK_LATER', m)} />
                    ))}
                  </View>
                </View>
              )}
              <Text style={styles.muted}>You keep your place whichever you pick.</Text>
              <Button label="Finished for today" variant="secondary" onPress={() => run(checkOut)} loading={busy} />
            </>
          )}
        </View>
      )}
      <ErrorText message={error} />

      <View style={styles.stats}>
        <Stat value={day.clubPosition} label="your number on the club list" />
        <Stat value={day.bagsThisWeek} label="your bags this week" />
      </View>
    </Screen>
  );
}

// Small components used only on this screen can live in the same file.
function Choice({ label, selected, onPress }: { label: string; selected?: boolean; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected: !!selected }}
      style={[styles.choice, selected && styles.choiceSelected]}>
      <Text style={[styles.choiceText, selected && styles.choiceTextSelected]}>{label}</Text>
    </Pressable>
  );
}

function Stat({ value, label }: { value: number | string; label: string }) {
  return (
    <View style={styles.stat}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.muted}>{label}</Text>
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
    gap: 12,
  },
  big: { fontSize: 28, fontWeight: '700', color: colors.text },
  status: { fontSize: 18, color: colors.primary, fontWeight: '600' },
  muted: { color: colors.muted, fontSize: 14 },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  timeBox: { gap: 8 },
  choice: {
    flexGrow: 1,
    minHeight: 44,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  choiceSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  choiceText: { color: colors.text, fontWeight: '500' },
  choiceTextSelected: { color: colors.primaryText },
  stats: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  stat: {
    flexGrow: 1,
    flexBasis: 140,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 10,
    padding: 12,
    gap: 2,
  },
  statValue: { fontSize: 24, fontWeight: '700', color: colors.text },
});
