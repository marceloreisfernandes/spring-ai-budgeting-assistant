package dev.tetel.budgeting.infrastructure.audio;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * Validates an uploaded audio file before it is ever sent to OpenAI. Every check here runs
 * against untrusted, attacker-controlled input (filename, declared content type, and bytes),
 * so extension and media type are both checked and neither is trusted alone. The original
 * filename is only ever used for extension sniffing here; it is never used to build a filesystem
 * path, and the file is never written to disk.
 */
@Component
public class AudioUploadValidator {

    /** Small, documented limit suitable for short voice commands in this educational project. */
    public static final long MAX_AUDIO_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "wav", "m4a");

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/mpeg", "audio/wav", "audio/x-wav", "audio/wave", "audio/mp4", "audio/x-m4a");

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidAudioUploadException("An audio file is required.");
        }
        if (file.getSize() > MAX_AUDIO_SIZE_BYTES) {
            throw new InvalidAudioUploadException("The audio file exceeds the maximum allowed size.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidAudioUploadException("Unsupported audio file extension.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new InvalidAudioUploadException("Unsupported audio media type.");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        // Reject anything that could look like a path rather than a bare filename.
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }
}
