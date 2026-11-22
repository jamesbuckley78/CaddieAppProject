import { useEffect, useState } from 'react';

import { checkHealth } from '@/api/client';

// A "hook" is a function starting with `use` that a component calls to get some
// state that stays between re-renders. This one asks the backend if it's up.
export function useServerStatus() {
  // useState gives a value plus a setter; calling the setter re-draws the screen.
  const [status, setStatus] = useState<'checking' | 'online' | 'offline'>('checking');

  // useEffect runs code after the screen first appears ([] = only once).
  useEffect(() => {
    checkHealth()
      .then(() => setStatus('online'))
      .catch(() => setStatus('offline'));
  }, []);

  return status;
}
