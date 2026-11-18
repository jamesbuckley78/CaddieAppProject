import { Text } from 'react-native';

import { Screen } from '@/components/Screen';

export default function AbsenceScreen() {
  return (
    <Screen title="Not coming in">
      <Text>Pick a date (24h+ ahead) and an optional reason. Admin approves or rejects.</Text>
    </Screen>
  );
}
