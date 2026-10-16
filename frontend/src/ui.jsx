// Small shared UI pieces so every screen handles loading / error / empty the same way.
export function Loading() {
  return <p className="py-10 text-center text-stone-500">Loading…</p>
}

export function ErrorBox({ message, onRetry }) {
  return (
    <div className="my-6 rounded-lg border border-red-200 bg-red-50 p-4 text-red-800">
      <p>{message}</p>
      {onRetry && (
        <button onClick={onRetry} className="mt-2 rounded bg-red-700 px-3 py-1 text-sm text-white">
          Try again
        </button>
      )}
    </div>
  )
}

export function Empty({ children }) {
  return <p className="rounded-lg border border-dashed border-stone-300 p-8 text-center text-stone-500">{children}</p>
}

const COLORS = {
  PLACED: 'bg-amber-100 text-amber-800',
  ACCEPTED: 'bg-blue-100 text-blue-800',
  PREPARING: 'bg-indigo-100 text-indigo-800',
  READY: 'bg-purple-100 text-purple-800',
  OUT_FOR_DELIVERY: 'bg-cyan-100 text-cyan-800',
  DELIVERED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  CANCELLED: 'bg-stone-200 text-stone-700',
}

export function StatusBadge({ status }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${COLORS[status] || ''}`}>
      {status.replaceAll('_', ' ')}
    </span>
  )
}

export const btn = 'rounded-lg bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700 disabled:opacity-50'
export const btnGhost = 'rounded-lg border border-stone-300 px-3 py-1.5 text-sm hover:bg-stone-100 disabled:opacity-50'
export const input = 'w-full rounded-lg border border-stone-300 px-3 py-2 text-sm focus:border-orange-500 focus:outline-none'
