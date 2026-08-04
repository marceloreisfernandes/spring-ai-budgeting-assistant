package dev.tetel.budgeting.infrastructure.audio;

/** Thrown when an uploaded audio file fails local validation, before OpenAI is ever called. */
public class InvalidAudioUploadException extends RuntimeException {

    public InvalidAudioUploadException(String message) {
        super(message);
    }
}
