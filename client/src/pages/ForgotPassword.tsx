import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { api } from '../api.ts'
import AuthCard, { authButtonClass, authInputClass } from '../components/AuthCard.tsx'
import { apiErrorMessage } from '../utils/apiError.ts'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [sent, setSent] = useState(false)
  const [sending, setSending] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const parsed = z.string().trim().pipe(z.email()).safeParse(email)
    if (!parsed.success) {
      setError('Enter a valid email address.')
      return
    }
    setSending(true)
    try {
      await api.post('/api/auth/forgot-password', { email: parsed.data })
      setSent(true)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setSending(false)
    }
  }

  return (
    <AuthCard title="Forgot password" subtitle="We will send you a link to choose a new one." onSubmit={submit}>
      {error && (
        <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      {sent ? (
        <p role="status" className="rounded-md bg-green-50 px-3 py-2 text-sm text-green-800">
          If an account exists for that email, a reset link is on its way. It works for 30 minutes.
        </p>
      ) : (
        <>
          <label className="block text-sm font-medium text-slate-700">
            Email
            <input
              type="email"
              autoComplete="username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={authInputClass}
            />
          </label>
          <button type="submit" disabled={sending} className={authButtonClass}>
            {sending ? 'Sending…' : 'Send reset link'}
          </button>
        </>
      )}

      <p className="text-center text-sm">
        <Link to="/login" className="font-medium text-blue-600 hover:underline">
          Back to sign in
        </Link>
      </p>
    </AuthCard>
  )
}
