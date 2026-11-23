import type { ReactNode } from 'react';
import { ImageBackground, ScrollView, StyleSheet, Text } from 'react-native';

import { BACKGROUND, BACKGROUND_OPACITY } from './branding';
import { colors } from './theme';

// A component is a function that returns what to draw (JSX - the HTML-like syntax).
// `props` are its inputs, like constructor arguments.
type ScreenProps = {
  title?: string; // `?` = optional prop
  children?: ReactNode; // whatever is placed between <Screen> ... </Screen>
};

export function Screen({ title, children }: ScreenProps) {
  return (
    // The club background sits behind the content only - the top banner and bottom tabs are
    // drawn outside <Screen>. imageStyle fades just the picture, not the content on top.
    // It stays still while the content scrolls over it.
    <ImageBackground
      source={BACKGROUND}
      resizeMode="cover"
      style={styles.background}
      // width/height 100%: without them the web version draws the picture at its own size in a corner
      imageStyle={{ opacity: BACKGROUND_OPACITY, width: '100%', height: '100%' }}>
      {/* ScrollView so long content (e.g. 200 caddies) can scroll on small phones */}
      <ScrollView style={styles.scroll} contentContainerStyle={styles.container}>
        {/* `{cond && <X/>}` draws X only when cond is true */}
        {title && <Text style={styles.title}>{title}</Text>}
        {children}
      </ScrollView>
    </ImageBackground>
  );
}

// React Native has no CSS files; styles are JS objects with camelCase names.
const styles = StyleSheet.create({
  background: { flex: 1, backgroundColor: colors.background },
  scroll: { flex: 1 },
  container: { padding: 16, gap: 12, width: '100%', maxWidth: 900, alignSelf: 'center' },
  title: { fontSize: 24, fontWeight: '600', color: colors.text },
});
