import { useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { api } from '../api.ts'
import AuthCard, { authButtonClass, authInputClass } from '../components/AuthCard.tsx'
import { apiErrorMessage } from '../utils/apiError.ts'

/** Opened from the emailed link: /reset-password#<token>. */
export default function ResetPassword() {
  const token = useLocation().hash.slice(1)
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [saving, setSaving] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (password.length < 8 || password.length > 72) {
      setError('The password must be 8 to 72 characters.')
      return
    }
    if (password !== confirm) {
      setError('The two passwords do not match.')
      return
    }
    setSaving(true)
    try {
      await api.post('/api/auth/reset-password', { token, password })
      setDone(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <AuthCard title="Choose a new password" subtitle="Signing in again is needed on every device." onSubmit={submit}>
      {error && (
        <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      {done ? (
        <p role="status" className="rounded-md bg-green-50 px-3 py-2 text-sm text-green-800">
          Your password was changed. You can sign in with it now.
        </p>
      ) : !token ? (
        <p role="alert" className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">
          This link is incomplete. Request a new one.
        </p>
      ) : (
        <>
          <label className="block text-sm font-medium text-slate-700">
            New password
            <input
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={authInputClass}
            />
          </label>
          <label className="block text-sm font-medium text-slate-700">
            Repeat the new password
            <input
              type="password"
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              className={authInputClass}
            />
          </label>
          <button type="submit" disabled={saving} className={authButtonClass}>
            {saving ? 'Saving…' : 'Change password'}
          </button>
        </>
      )}

      <p className="flex justify-between text-sm">
        <Link to="/login" className="font-medium text-blue-600 hover:underline">
          Back to sign in
        </Link>
        {!done && (
          <Link to="/forgot-password" className="font-medium text-blue-600 hover:underline">
            Request a new link
          </Link>
        )}
      </p>
    </AuthCard>
  )
}
