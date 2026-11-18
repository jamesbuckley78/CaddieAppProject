import { Redirect } from 'expo-router';

// Entry point. Later: read the saved token (expo-secure-store) and send caddies to their
// home screen and admins to the board. For now, always go to login.
export default function Index() {
  return <Redirect href="/login" />;
}
