package dev.tetel.budgeting.infrastructure.audio;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests (no Spring context, no Docker, no network): the validator is a pure function
 * over a {@code MultipartFile}, so invalid uploads can be proven to never reach OpenAI here.
 */
class AudioUploadValidatorTest {

    private final AudioUploadValidator validator = new AudioUploadValidator();

    @Test
    void acceptsAValidMp3Upload() {
        MockMultipartFile file = new MockMultipartFile("file", "note.mp3", "audio/mpeg", "fake-audio-bytes".getBytes());

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void acceptsAValidWavUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "note.wav", "audio/wav", "fake-audio-bytes".getBytes());

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAMissingFile() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(InvalidAudioUploadException.class);
    }

    @Test
    void rejectsAnEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "note.mp3", "audio/mpeg", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidAudioUploadException.class);
    }

    @Test
    void rejectsAFileAboveTheMaximumSize() {
        byte[] tooBig = new byte[(int) AudioUploadValidator.MAX_AUDIO_SIZE_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "note.mp3", "audio/mpeg", tooBig);

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidAudioUploadException.class);
    }

    @Test
    void rejectsAnUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "note.exe", "audio/mpeg", "bytes".getBytes());

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidAudioUploadException.class);
    }

    @Test
    void rejectsAnUnsupportedMediaTypeEvenWithAnAllowedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "note.mp3", "application/octet-stream", "bytes".getBytes());

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidAudioUploadException.class);
    }

    @Test
    void rejectsAMismatchedExtensionEvenWithAnAllowedMediaType() {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "audio/mpeg", "bytes".getBytes());

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidAudioUploadException.class);
    }

    @Test
    void rejectsAFilenameWithNoExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "note", "audio/mpeg", "bytes".getBytes());

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidAudioUploadException.class);
    }

    @Test
    void rejectsAPathTraversalFilenameEvenIfOtherwiseValid() {
        MockMultipartFile file = new MockMultipartFile("file", "../../etc/passwd.mp3", "audio/mpeg", "bytes".getBytes());

        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(InvalidAudioUploadException.class);
    }
}
