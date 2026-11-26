import type { ReactNode } from 'react'
import Modal from './Modal.tsx'

/** A short confirmation that something worked, dismissed with OK, Escape or the close button. */
export default function NoticeModal({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}) {
  return (
    <Modal open={open} title={title} onClose={onClose}>
      <div className="flex items-start gap-3">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-green-50 text-green-600">
          <svg
            className="h-5 w-5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2.2}
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M5 13l4 4L19 7" />
          </svg>
        </span>
        <p role="status" className="pt-2 text-sm text-slate-600">
          {children}
        </p>
      </div>
      <div className="mt-5 flex justify-end">
        <button
          type="button"
          autoFocus
          onClick={onClose}
          className="rounded-md bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          OK
        </button>
      </div>
    </Modal>
  )
}
