package dev.tetel.budgeting.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the single {@link ChatClient} used by the voice assistant flow from the OpenAI-backed builder Spring AI auto-configures. */
@Configuration
class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
