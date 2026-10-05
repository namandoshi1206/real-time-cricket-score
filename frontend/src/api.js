const API_ROOT = import.meta.env.VITE_API_URL || 'http://localhost:9090/api';

function authorizationHeader(credentials) {
  if (!credentials) return {};
  const bytes = new TextEncoder().encode(`${credentials.username}:${credentials.password}`);
  const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join('');
  return { Authorization: `Basic ${btoa(binary)}` };
}

export async function api(path, { credentials, ...options } = {}) {
  const response = await fetch(`${API_ROOT}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...authorizationHeader(credentials),
      ...options.headers,
    },
  });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const validation = payload?.validationErrors
      ? Object.values(payload.validationErrors).join(', ')
      : '';
    throw new Error(validation || payload?.message || `Request failed (${response.status})`);
  }
  return payload;
}

export const send = (path, method, body, credentials) =>
  api(path, { method, body: JSON.stringify(body), credentials });