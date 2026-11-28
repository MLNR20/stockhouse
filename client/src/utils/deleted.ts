type Option = { value: string; label: string }

/** A record's name as plain text, marked when it has since been deleted. */
export const markDeleted = (name: string, active: boolean) => (active ? name : `${name} (deleted)`)

/**
 * The options of a dropdown, plus any of these records that has since been deleted. Pass only what the record being
 * edited already uses, so it still shows and can be kept; deleted records are never offered otherwise.
 */
export function withDeleted(
  options: Option[],
  ...current: { id: string | null; name: string | null; active: boolean }[]
): Option[] {
  const result = [...options]
  for (const { id, name, active } of current) {
    if (id && !active && !result.some((o) => o.value === id)) {
      result.push({ value: id, label: markDeleted(name ?? 'Unknown', false) })
    }
  }
  return result
}
