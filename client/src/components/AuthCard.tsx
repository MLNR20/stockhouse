import type { FormEventHandler, ReactNode } from 'react'

export const authInputClass =
  'mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500'

export const authButtonClass =
  'w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60'

/** The centred card used by the signed-out pages. */
export default function AuthCard({
  title,
  subtitle,
  onSubmit,
  children,
}: {
  title: string
  subtitle: string
  onSubmit: FormEventHandler<HTMLFormElement>
  children: ReactNode
}) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <form
        onSubmit={onSubmit}
        noValidate
        className="w-full max-w-sm space-y-4 rounded-lg border border-slate-200 bg-white p-6 shadow-sm"
      >
        <div>
          <h1 className="text-xl font-semibold text-slate-900">{title}</h1>
          <p className="mt-1 text-sm text-slate-500">{subtitle}</p>
        </div>
        {children}
      </form>
    </div>
  )
}
