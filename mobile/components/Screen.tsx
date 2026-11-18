import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

// A component is a function that returns what to draw (JSX - the HTML-like syntax).
// `props` are its inputs, like constructor arguments.
type ScreenProps = {
  title: string;
  children?: ReactNode; // whatever is placed between <Screen> ... </Screen>
};

export function Screen({ title, children }: ScreenProps) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>{title}</Text>
      {children}
    </View>
  );
}

// React Native has no CSS files; styles are JS objects with camelCase names.
const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, gap: 12 },
  title: { fontSize: 24, fontWeight: '600' },
});
