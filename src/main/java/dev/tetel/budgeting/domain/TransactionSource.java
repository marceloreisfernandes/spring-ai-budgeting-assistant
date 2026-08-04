package dev.tetel.budgeting.domain;

/** Origin of a transaction: a direct REST call, or the voice assistant flow. */
public enum TransactionSource {
    REST,
    VOICE
}
