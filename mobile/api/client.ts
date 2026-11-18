// One place for talking to the Spring Boot backend. Screens call these functions
// instead of using fetch() directly, so the base URL and auth header live here only.

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api';

// `async` functions return a Promise (like Java's CompletableFuture).
// `await` pauses inside the function until the Promise finishes, without freezing the UI.
// The <T> is a generic, same idea as in Java: the caller says what type comes back.
export async function apiGet<T>(path: string, token?: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error(`GET ${path} failed: ${res.status}`);
  }
  return (await res.json()) as T;
}

export async function apiPost<T>(path: string, body: unknown, token?: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    throw new Error(`POST ${path} failed: ${res.status}`);
  }
  return (await res.json()) as T;
}

export function checkHealth() {
  return apiGet<{ status: string; time: string }>('/health');
}

export { API_URL };
