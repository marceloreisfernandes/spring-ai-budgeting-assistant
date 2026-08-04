package dev.tetel.budgeting.infrastructure.web;

import dev.tetel.budgeting.infrastructure.ai.VoiceAssistantOrchestrationService;
import dev.tetel.budgeting.infrastructure.audio.AudioUploadValidator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

/**
 * The voice assistant endpoint. {@link AudioUploadValidator} always runs first, and rejects
 * unsupported/oversized/empty uploads before OpenAI is ever called - see
 * docs/security/prompt-injection.md and docs/security/security-controls.md for why that ordering
 * matters (it is what "invalid uploads do not consume OpenAI calls" means in practice).
 */
@RestController
@RequestMapping("/api/v1/assistant")
class AssistantController {

    private final AudioUploadValidator audioUploadValidator;
    private final VoiceAssistantOrchestrationService orchestrationService;

    AssistantController(AudioUploadValidator audioUploadValidator, VoiceAssistantOrchestrationService orchestrationService) {
        this.audioUploadValidator = Objects.requireNonNull(audioUploadValidator);
        this.orchestrationService = Objects.requireNonNull(orchestrationService);
    }

    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<byte[]> handleAudio(@RequestParam("file") MultipartFile file, Authentication authentication) {
        audioUploadValidator.validate(file);
        byte[] mp3 = orchestrationService.handle(file.getResource(), authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(mp3);
    }
}
