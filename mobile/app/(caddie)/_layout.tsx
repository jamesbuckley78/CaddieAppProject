import { Tabs } from 'expo-router';

// Bottom tabs for the caddie side of the app. Each screen in this folder becomes a tab.
export default function CaddieLayout() {
  return (
    <Tabs>
      <Tabs.Screen name="home" options={{ title: 'Today' }} />
      <Tabs.Screen name="absence" options={{ title: 'Not coming in' }} />
      <Tabs.Screen name="report" options={{ title: 'Report issue' }} />
      <Tabs.Screen name="history" options={{ title: 'History' }} />
    </Tabs>
  );
}
