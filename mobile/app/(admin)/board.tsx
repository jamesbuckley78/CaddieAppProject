import { StyleSheet, Text, View } from 'react-native';

import { Screen } from '@/components/Screen';

export default function AdminBoard() {
  return (
    <Screen title="Board">
      {/* Two tables side by side - both are filtered views of the same queue */}
      <View style={styles.row}>
        <View style={styles.column}>
          <Text style={styles.heading}>Singles</Text>
          <Text>Position, name, priority badge, time waiting, Assign</Text>
        </View>
        <View style={styles.column}>
          <Text style={styles.heading}>Doubles</Text>
          <Text>Position, name, priority badge, time waiting, Assign</Text>
        </View>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 12 }, // flexDirection 'row' = side by side
  column: { flex: 1, padding: 12, borderWidth: 1, borderColor: '#ccc', borderRadius: 8 },
  heading: { fontSize: 18, fontWeight: '600', marginBottom: 8 },
});
