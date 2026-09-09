package com.tcc.pjb.backend.ai.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.common.clients.guard.GuardedAiModelClient;
import com.tcc.pjb.backend.ai.common.clients.local.LocalHeuristicAiModelClient;
import com.tcc.pjb.backend.ai.common.clients.ollama.OllamaChatClient;
import com.tcc.pjb.backend.ai.common.clients.openai.OpenAiChatCompletionsClient;
import com.tcc.pjb.backend.core.security.audit.PjbSecurityEventLogger;

class AiModelClientFactoryGuardTest {

    private final PjbSecurityEventLogger securityEventLogger =
            new PjbSecurityEventLogger(new SimpleMeterRegistry());

    private AiModelClientFactory factoryCom(MockEnvironment env) {
        return new AiModelClientFactory(env, securityEventLogger);
    }

    @Test
    void provedorLocalPadraoSaiEnvolvidoPelaGuarda() {
        AiModelClient client = factoryCom(new MockEnvironment()).create("v1");

        assertThat(client).isInstanceOf(GuardedAiModelClient.class);
        assertThat(((GuardedAiModelClient) client).delegate())
                .isInstanceOf(LocalHeuristicAiModelClient.class);
    }

    @Test
    void provedorOllamaSaiEnvolvidoPelaGuarda() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("pjb.ai.provider", "ollama");

        AiModelClient client = factoryCom(env).create("v2");

        assertThat(client).isInstanceOf(GuardedAiModelClient.class);
        assertThat(((GuardedAiModelClient) client).delegate())
                .isInstanceOf(OllamaChatClient.class);
    }

    @Test
    void provedorOpenAiSaiEnvolvidoPelaGuarda() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("pjb.ai.provider", "openai");
        env.setProperty("pjb.ai.openai.api-key", "chave-de-teste");

        AiModelClient client = factoryCom(env).create("v3");

        assertThat(client).isInstanceOf(GuardedAiModelClient.class);
        assertThat(((GuardedAiModelClient) client).delegate())
                .isInstanceOf(OpenAiChatCompletionsClient.class);
    }

    @Test
    void openAiSemChaveCaiParaLocalMasContinuaEnvolvidoPelaGuarda() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("pjb.ai.provider", "openai");

        AiModelClient client = factoryCom(env).create("v1");

        assertThat(client).isInstanceOf(GuardedAiModelClient.class);
        assertThat(((GuardedAiModelClient) client).delegate())
                .isInstanceOf(LocalHeuristicAiModelClient.class);
    }
}
