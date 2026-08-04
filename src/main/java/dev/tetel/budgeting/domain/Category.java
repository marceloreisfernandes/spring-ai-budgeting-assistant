package dev.tetel.budgeting.domain;

/** Allowlisted spending categories. Persisted as a readable string, never as an ordinal. */
public enum Category {
    GROCERIES,
    PHARMA,
    AUTO,
    RESTAURANT,
    ENTERTAINMENT,
    OTHER
}
