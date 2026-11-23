import { Image, StyleSheet } from 'react-native';

// The club's look in one place. Both images are placeholders: replace the files in
// assets/branding/ (same names) with the real crest and background - no code changes needed.
//
// require('...png') is how React Native bundles an image into the app: the path is checked
// when the app is built, and the result is passed to <Image source={...}>.

/** The club crest, shown on the left of the top banner. A PNG with a transparent background works best. */
export const CREST = require('../assets/branding/crest.png');

/** Shown faintly behind every screen's content (not the top or bottom banners). */
export const BACKGROUND = require('../assets/branding/background.png');

/** How visible the background is: 0 = invisible, 1 = full strength. Keep it low so text stays readable. */
export const BACKGROUND_OPACITY = 0.08;

/** The crest sized for the top banner. */
export function ClubCrest() {
  return <Image source={CREST} style={styles.crest} resizeMode="contain" accessibilityLabel="Club crest" />;
}

const styles = StyleSheet.create({
  crest: { width: 32, height: 32, marginLeft: 12 },
});
