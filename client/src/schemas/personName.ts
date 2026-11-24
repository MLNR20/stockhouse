import { z } from 'zod'

/**
 * A person's first or last name: letters only (any alphabet), at least 2 characters. The same rule
 * the server enforces for customers and warehouse employees, so the form catches it before the request.
 * `label` is used in the messages, e.g. personName('First name').
 */
export const personName = (label: string) =>
  z
    .string()
    .trim()
    .min(1, `${label} is required`)
    .min(2, `${label} must be at least 2 letters`)
    .max(100, `${label} must be 100 characters or fewer`)
    .regex(/^\p{L}+$/u, `${label} can only contain letters`)
