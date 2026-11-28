/** The name of a related record. One that has since been deleted is greyed out and marked. */
export default function RecordName({ name, active }: { name: string | null; active: boolean }) {
  if (!name) return <>—</>
  return (
    <span className={active ? '' : 'text-slate-400'}>
      {name}
      {!active && <span className="ml-1.5 text-xs italic">(deleted)</span>}
    </span>
  )
}
