import { StyleSheet, Text } from 'react-native';

import type { Grade } from '@/types';

import { tagColors } from './theme';

/**
 * One coloured letter beside a name: P for a priority caddie, otherwise S (single),
 * D (double) or J (junior).
 */
export function CaddieTag({ priority, grade }: { priority: boolean; grade: Grade }) {
  // `keyof typeof tagColors` = one of the keys of that object: 'P' | 'S' | 'D' | 'J'
  const letter: keyof typeof tagColors = priority ? 'P' : grade === 'DOUBLE' ? 'D' : grade === 'JUNIOR' ? 'J' : 'S';
  return (
    <Text style={[styles.tag, { backgroundColor: tagColors[letter] }]} accessibilityLabel={LABELS[letter]}>
      {letter}
    </Text>
  );
}

const LABELS = { P: 'Priority', S: 'Single', D: 'Double', J: 'Junior' };

const styles = StyleSheet.create({
  tag: {
    color: '#ffffff',
    fontSize: 11,
    fontWeight: '700',
    width: 18,
    textAlign: 'center',
    paddingVertical: 1,
    borderRadius: 4,
    overflow: 'hidden',
  },
});
