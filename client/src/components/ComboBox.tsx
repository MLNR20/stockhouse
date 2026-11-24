import { useEffect, useId, useRef, useState } from 'react'

export type ComboOption = { value: string; label: string }

type ComboBoxProps = {
  id?: string
  /** The chosen option's value, or '' for none. */
  value: string
  options: ComboOption[]
  onChange: (value: string) => void
  /** Shown while the box is empty, e.g. "Select a product". */
  placeholder?: string
  className?: string
  disabled?: boolean
  'aria-invalid'?: boolean
  'aria-describedby'?: string
  autoFocus?: boolean
}

const labelOf = (options: ComboOption[], value: string) => options.find((o) => o.value === value)?.label ?? ''

/**
 * A typeable replacement for <select>: start typing to narrow the suggestions, or pick one from the list.
 * What counts is the option the text matches (ignoring case); text that matches none leaves the value empty
 * and is cleared when the box loses focus, so the field never holds something that is not on the list.
 */
export default function ComboBox({
  id,
  value,
  options,
  onChange,
  placeholder,
  className,
  disabled,
  autoFocus,
  ...aria
}: ComboBoxProps) {
  const listId = useId()
  const [text, setText] = useState(() => labelOf(options, value))
  /** The value this box last reported, so an echo of it is not mistaken for a change from outside. */
  const reported = useRef(value)

  // Follow changes made from outside (the form was reset, a record was loaded, the options arrived).
  useEffect(() => {
    if (value === reported.current && value === '') return
    reported.current = value
    setText(labelOf(options, value))
  }, [value, options])

  function handleInput(next: string) {
    setText(next)
    const match = options.find((o) => o.label.toLowerCase() === next.trim().toLowerCase())
    const nextValue = match?.value ?? ''
    if (nextValue !== reported.current) {
      reported.current = nextValue
      onChange(nextValue)
    }
  }

  return (
    <>
      <input
        id={id}
        type="text"
        role="combobox"
        list={listId}
        value={text}
        placeholder={placeholder}
        disabled={disabled}
        autoFocus={autoFocus}
        autoComplete="off"
        className={className}
        onChange={(e) => handleInput(e.target.value)}
        onBlur={() => setText(labelOf(options, value))}
        {...aria}
      />
      <datalist id={listId}>
        {options.map((o) => (
          <option key={o.value} value={o.label} />
        ))}
      </datalist>
    </>
  )
}
