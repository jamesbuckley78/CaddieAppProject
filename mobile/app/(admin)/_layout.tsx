import { Tabs } from 'expo-router';

// Admin (caddie master) side. Designed to work on a phone, tablet or PC browser.
export default function AdminLayout() {
  return (
    <Tabs>
      <Tabs.Screen name="board" options={{ title: 'Board' }} />
      <Tabs.Screen name="caddies" options={{ title: 'Caddies' }} />
      <Tabs.Screen name="inbox" options={{ title: 'Inbox' }} />
      <Tabs.Screen name="attendance" options={{ title: 'Attendance' }} />
    </Tabs>
  );
}
