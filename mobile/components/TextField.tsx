import { StyleSheet, Text, TextInput, View, type TextInputProps } from 'react-native';

import { colors } from './theme';

// `TextInputProps & {...}` = all the normal TextInput props plus a label.
// `...rest` collects the remaining props and passes them straight to TextInput.
type TextFieldProps = TextInputProps & { label: string };

export function TextField({ label, style, ...rest }: TextFieldProps) {
  return (
    <View style={styles.wrapper}>
      <Text style={styles.label}>{label}</Text>
      <TextInput style={[styles.input, style]} placeholderTextColor={colors.muted} {...rest} />
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { gap: 4 },
  label: { fontSize: 14, fontWeight: '500', color: colors.text },
  input: {
    minHeight: 44,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    paddingHorizontal: 12,
    fontSize: 16,
    backgroundColor: colors.card,
    color: colors.text,
  },
});
