import { StyleSheet, Text } from 'react-native';

import { Screen } from '@/components/Screen';
import { colors } from '@/components/theme';
import { useAuth } from '@/hooks/useAuth';

const GRADE_LABEL = { SINGLE: 'Single', DOUBLE: 'Double', JUNIOR: 'Junior' } as const;

export default function CaddieHome() {
  const { user } = useAuth();
  if (!user) return null; // the layout already redirected; this just keeps TypeScript happy

  return (
    <Screen title={`Hi ${user.name}`}>
      <Text style={styles.meta}>
        {user.grade ? GRADE_LABEL[user.grade] : ''} caddie{user.priority ? ' · Priority' : ''}
      </Text>
      <Text style={styles.placeholder}>
        Next step: the "I'm here - available" button, your place in today's list, and how many are ahead of you by
        bag type.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  meta: { fontSize: 16, color: colors.muted },
  placeholder: { fontSize: 15, color: colors.text },
});
