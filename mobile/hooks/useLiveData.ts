import { useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Loads data when a screen opens and keeps it fresh every `intervalMs` while the screen is
 * showing. All screens refresh through here, so changing how they refresh means changing only this file.
 *
 * `load` is any function returning a Promise, e.g. fetchBoard. `<T>` = whatever it returns.
 */
export function useLiveData<T>(load: () => Promise<T>, intervalMs = 15000) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);

  // useRef holds a value that survives re-draws without causing one - here, the latest `load`.
  const loadRef = useRef(load);
  useEffect(() => {
    loadRef.current = load;
  }, [load]);

  // Goes up by one on every load and every result shown from a POST. A load only shows its
  // result if nothing newer started meanwhile, so a slow refresh can't overwrite fresher data.
  const version = useRef(0);

  // useCallback keeps the same function between re-draws, so effects using it don't re-run.
  const reload = useCallback(async () => {
    const started = ++version.current;
    try {
      const result = await loadRef.current();
      if (started !== version.current) return;
      setData(result);
      setError(null);
    } catch (e) {
      if (started !== version.current) return;
      setError(e instanceof Error ? e.message : 'Could not load');
    }
  }, []);

  const showData = useCallback((value: T) => {
    version.current++;
    setData(value);
  }, []);

  // useFocusEffect = useEffect that runs when the screen comes into view (e.g. switching tabs),
  // and cleans up when it leaves - so hidden screens stop polling.
  useFocusEffect(
    useCallback(() => {
      reload();
      const timer = setInterval(reload, intervalMs);
      return () => clearInterval(timer);
    }, [reload, intervalMs]),
  );

  // setData is returned too, so a screen can show a fresh result straight from a POST.
  return { data, error, reload, setData: showData };
}
