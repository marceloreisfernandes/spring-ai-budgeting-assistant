package dev.tetel.budgeting.infrastructure.web;

import dev.tetel.budgeting.infrastructure.ai.VoiceAssistantOrchestrationService;
import dev.tetel.budgeting.infrastructure.audio.AudioUploadValidator;
import dev.tetel.budgeting.infrastructure.audio.InvalidAudioUploadException;
import dev.tetel.budgeting.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
@Import(SecurityConfig.class)
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AudioUploadValidator audioUploadValidator;

    @MockitoBean
    private VoiceAssistantOrchestrationService orchestrationService;

    @Test
    void returnsMp3AudioForAValidUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "note.mp3", "audio/mpeg", "audio-bytes".getBytes());
        given(orchestrationService.handle(any(), anyString())).willReturn(new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/v1/assistant/audio")
                        .file(file)
                        .with(httpBasic("local-dev-user", "local-dev-password")))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getContentType()).startsWith("audio/mpeg"));
    }

    @Test
    void rejectsAnInvalidUploadWithoutCallingTheOrchestrationService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "note.exe", "application/octet-stream", "bytes".getBytes());
        willThrow(new InvalidAudioUploadException("Unsupported audio file extension."))
                .given(audioUploadValidator).validate(any());

        mockMvc.perform(multipart("/api/v1/assistant/audio")
                        .file(file)
                        .with(httpBasic("local-dev-user", "local-dev-password")))
                .andExpect(status().isBadRequest());

        verify(orchestrationService, never()).handle(any(), anyString());
    }

    @Test
    void rejectsUploadWithoutCredentials() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "note.mp3", "audio/mpeg", "audio-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/assistant/audio").file(file))
                .andExpect(status().isUnauthorized());

        verify(orchestrationService, never()).handle(any(), anyString());
    }
}
