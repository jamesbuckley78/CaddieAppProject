// One place for talking to the Spring Boot backend. Screens call the functions in
// api/auth.ts, api/caddies.ts etc., which all go through request() below, so the base URL,
// the login token and error handling live here only.

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api';

// `let` = a variable that can be reassigned (`const` can't). This is module-level state:
// hooks/useAuth.tsx sets it after login, and every request after that sends it.
let authToken: string | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

// Our own error type, so screens can check `error.status === 401` etc.
// `public readonly status` in the constructor declares and sets a field in one go.
export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

// `async` functions return a Promise (like Java's CompletableFuture).
// `await` pauses inside the function until the Promise finishes, without freezing the UI.
// The <T> is a generic, same idea as in Java: the caller says what type comes back.
async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (authToken) headers.Authorization = `Bearer ${authToken}`;

  let res: Response;
  try {
    res = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    // fetch only throws when there was no response at all (server down, no wifi)
    throw new ApiError(0, "Can't reach the server. Check your connection and try again.");
  }

  const text = await res.text();
  let data: any = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    // not JSON (e.g. an HTML error page) - leave data as null
  }

  if (!res.ok) {
    // The backend sends {"error": "..."} (common/ApiExceptionHandler.java).
    // `?.` = "only read .error if data isn't null"; `??` = "use the right side if the left is null".
    throw new ApiError(res.status, data?.error ?? `Request failed (${res.status})`);
  }
  return data as T;
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body: unknown = {}) => request<T>('POST', path, body),
};

export function checkHealth() {
  return api.get<{ status: string; time: string }>('/health');
}

export { API_URL };
