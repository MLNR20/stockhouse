package com.example.warehouse_proj.common;

/**
 * Validation rules for a person's first or last name, shared so customers and employees agree.
 * Use as {@code @Size(min = 2, max = 100) @Pattern(regexp = NameRules.LETTERS_ONLY, message = NameRules.LETTERS_ONLY_MESSAGE)}.
 */
public final class NameRules {

    /** Letters only, from any alphabet (so "José" and "Zoë" are fine). No digits, spaces, hyphens or apostrophes. */
    public static final String LETTERS_ONLY = "^\\p{L}+$";

    public static final String LETTERS_ONLY_MESSAGE = "must contain letters only";

    private NameRules() {
    }
}
