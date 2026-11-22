import * as Location from 'expo-location';

// Reads the phone's position once, for "I'm here". Nothing runs in the background.
// On web this uses the browser's location prompt.

export class LocationError extends Error {}

export async function getCurrentPosition() {
  const permission = await Location.requestForegroundPermissionsAsync();
  if (permission.status !== 'granted') {
    throw new LocationError(
      'Location access is needed to check in. Allow it for Caddie List in your phone settings, then try again.',
    );
  }
  try {
    const position = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
    return { latitude: position.coords.latitude, longitude: position.coords.longitude };
  } catch {
    throw new LocationError("Couldn't get your location. Make sure location is turned on and try again.");
  }
}
