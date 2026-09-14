package com.tcc.pjb.backend.ai.common.config;

import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.common.clients.local.LocalHeuristicAiModelClient;
import com.tcc.pjb.backend.ai.common.clients.ollama.OllamaChatClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.tcc.pjb.backend.ai.common.clients.guard.GuardedAiModelClient;
import com.tcc.pjb.backend.ai.common.clients.resilience.ResilientAiModelClient;
import com.tcc.pjb.backend.ai.common.clients.openai.OpenAiChatCompletionsClient;
import com.tcc.pjb.backend.ai.legalai.security.AiPromptEgressGuard;
import com.tcc.pjb.backend.core.security.audit.PjbSecurityEventLogger;

@Component
public class AiModelClientFactory {

    private final Environment env;
    private final PjbSecurityEventLogger securityEventLogger;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final AiPromptEgressGuard promptEgressGuard = new AiPromptEgressGuard();

    public AiModelClientFactory(Environment env,
                                PjbSecurityEventLogger securityEventLogger,
                                CircuitBreakerRegistry circuitBreakerRegistry) {
        this.env = env;
        this.securityEventLogger = securityEventLogger;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public AiModelClient create(String version) {
        String v = version == null ? "v1" : version.toLowerCase(Locale.ROOT);

        String provider = firstNonBlank(
                env.getProperty("pjb.ai." + v + ".provider"),
                env.getProperty("pjb.ai.provider"),
                "local"
        ).toLowerCase(Locale.ROOT);

        if ("ollama".equals(provider)) {
            String baseUrl = firstNonBlank(env.getProperty("pjb.ai.ollama.base-url"), "http://localhost:11434");
            String model = firstNonBlank(env.getProperty("pjb.ai.ollama.model"), "qwen2.5:7b");
            double temperature = parseDouble(env.getProperty("pjb.ai.ollama.temperature"), 0.2);
            long timeoutMs = parseLong(env.getProperty("pjb.ai.ollama.timeout-ms"), 180_000);

            OllamaChatClient client = new OllamaChatClient(baseUrl, model, temperature);
            client.setTimeout(timeoutMs);
            return guardado(resiliente(client, "ollama"), v);
        }

        if ("openai".equals(provider)) {
            String apiKey = firstNonBlank(
                    env.getProperty("pjb.ai.openai.api-key"),
                    env.getProperty("OPENAI_API_KEY")
            );

            if (apiKey != null && !apiKey.isBlank()) {
                String model = firstNonBlank(env.getProperty("pjb.ai.openai.model"), "gpt-5.2-thinking");
                String baseUrl = firstNonBlank(env.getProperty("pjb.ai.openai.base-url"), "https://api.openai.com/v1");
                double temperature = parseDouble(env.getProperty("pjb.ai.openai.temperature"), 0.2);
                int maxTokens = parseInt(env.getProperty("pjb.ai.openai.max-tokens"), 6000);

                OpenAiChatCompletionsClient client = new OpenAiChatCompletionsClient(apiKey, baseUrl, model, temperature, maxTokens, v);
                long timeoutMs = parseLong(env.getProperty("pjb.ai.openai.timeout-ms"), 180_000);
                client.setTimeout(timeoutMs);
                return guardado(resiliente(client, "openai"), v);
            }
        }


        return guardado(new LocalHeuristicAiModelClient(v), v);
    }

    private AiModelClient guardado(AiModelClient client, String versao) {
        return new GuardedAiModelClient(client, promptEgressGuard, securityEventLogger, versao);
    }

    private AiModelClient resiliente(AiModelClient client, String provedor) {
        return new ResilientAiModelClient(
                client,
                circuitBreakerRegistry.circuitBreaker(ResilientAiModelClient.CIRCUIT_BREAKER_NAME),
                provedor);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String s : values) {
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }

    private static double parseDouble(String s, double def) {
        if (s == null || s.isBlank()) return def;
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static long parseLong(String s, long def) {
        if (s == null || s.isBlank()) return def;
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }
}
