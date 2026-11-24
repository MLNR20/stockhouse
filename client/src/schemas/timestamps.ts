import { z } from 'zod'

/** Every record from the API carries these (BaseEntity); spread into an entity schema with `...timestampFields`. */
export const timestampFields = {
  createdAt: z.string(),
  /** Null until the record is first edited. */
  updatedAt: z.string().nullable(),
}
