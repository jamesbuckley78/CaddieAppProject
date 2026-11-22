import type { ReactNode } from 'react';
import { ScrollView, StyleSheet, Text } from 'react-native';

import { colors } from './theme';

// A component is a function that returns what to draw (JSX - the HTML-like syntax).
// `props` are its inputs, like constructor arguments.
type ScreenProps = {
  title?: string; // `?` = optional prop
  children?: ReactNode; // whatever is placed between <Screen> ... </Screen>
};

export function Screen({ title, children }: ScreenProps) {
  return (
    // ScrollView so long content (e.g. 200 caddies) can scroll on small phones
    <ScrollView style={styles.scroll} contentContainerStyle={styles.container}>
      {/* `{cond && <X/>}` draws X only when cond is true */}
      {title && <Text style={styles.title}>{title}</Text>}
      {children}
    </ScrollView>
  );
}

// React Native has no CSS files; styles are JS objects with camelCase names.
const styles = StyleSheet.create({
  scroll: { flex: 1, backgroundColor: colors.background },
  container: { padding: 16, gap: 12, width: '100%', maxWidth: 900, alignSelf: 'center' },
  title: { fontSize: 24, fontWeight: '600', color: colors.text },
});
