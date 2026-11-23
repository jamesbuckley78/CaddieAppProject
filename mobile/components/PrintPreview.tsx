import { useRef } from 'react';
import { Modal, StyleSheet, Text, View } from 'react-native';

import { Button } from './Button';
import { colors } from './theme';

type PrintPreviewProps = {
  html: string; // a complete page, e.g. from printableHtml()
  onClose: () => void;
};

/**
 * Web only: shows a printable page over the app, with Print and Close buttons. No pop-up
 * window, so pop-up blockers (and the Claude browser pane) can't get in the way.
 *
 * The page sits in an <iframe>: a box showing a separate HTML document inside this one.
 * `srcDoc` hands it the HTML directly, and printing the iframe prints only that page, not
 * the app around it. Plain HTML tags like <iframe> work here because on the web React Native
 * draws with ordinary HTML.
 */
export function PrintPreview({ html, onClose }: PrintPreviewProps) {
  // useRef gives us the iframe element itself once it's on screen, so we can call print() on it
  const frame = useRef<HTMLIFrameElement>(null);

  return (
    // Modal = a layer over the whole screen until it's closed
    <Modal visible animationType="fade" onRequestClose={onClose}>
      <View style={styles.wrap}>
        <View style={styles.toolbar}>
          <Button label="Print" onPress={() => frame.current?.contentWindow?.print()} />
          <Button label="Close" variant="secondary" onPress={onClose} />
          <Text style={styles.note}>Preview - this is exactly what will print.</Text>
        </View>
        <iframe ref={frame} srcDoc={html} title="Print preview" style={{ flex: 1, border: 0, width: '100%' }} />
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1, backgroundColor: '#fff' },
  toolbar: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 10,
    padding: 10,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
    backgroundColor: colors.card,
  },
  note: { color: colors.muted, fontSize: 14 },
});
