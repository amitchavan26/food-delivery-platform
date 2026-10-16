// Thin wrapper around fetch. Errors carry the server's message and field errors.
export async function api(path, { method = 'GET', body, token } = {}) {
  const res = await fetch('/api' + path, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  const data = await res.json().catch(() => null)
  if (!res.ok) {
    const err = new Error(data?.message || 'Something went wrong')
    err.status = res.status
    err.fields = data?.fields || {}
    throw err
  }
  return data
}

export const rupees = (paise) => '₹' + (paise / 100).toFixed(2)
