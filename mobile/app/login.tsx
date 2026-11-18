import { Link } from 'expo-router';
import { Text } from 'react-native';

import { API_URL } from '@/api/client';
import { Screen } from '@/components/Screen';
import { useServerStatus } from '@/hooks/useServerStatus';

export default function LoginScreen() {
  const server = useServerStatus();

  return (
    <Screen title="Caddie List - Log in">
      <Text>Login form goes here (phone + password).</Text>
      <Text>
        Server ({API_URL}): {server}
      </Text>

      {/* Temporary links so you can look at both sides of the app before login exists */}
      <Link href="/home">Open caddie screens</Link>
      <Link href="/board">Open admin board</Link>
    </Screen>
  );
}
