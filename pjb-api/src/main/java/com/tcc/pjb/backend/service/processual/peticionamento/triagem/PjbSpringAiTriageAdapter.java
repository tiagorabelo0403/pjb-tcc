package com.tcc.pjb.backend.service.processual.peticionamento.triagem;

import com.tcc.pjb.backend.core.peticionamento.triagem.AiTriageContext;
import com.tcc.pjb.backend.core.peticionamento.triagem.AiTriageSuggestion;
import com.tcc.pjb.backend.core.peticionamento.triagem.PjbAiTriageSuggestionPort;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;

public class PjbSpringAiTriageAdapter implements PjbAiTriageSuggestionPort {

    private final ChatClient chatClient;

    public PjbSpringAiTriageAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = Objects.requireNonNull(chatClientBuilder).build();
    }

    @Override
    public AiTriageSuggestion suggest(AiTriageContext context) {
        String prompt = buildTriagePrompt(context);

        String resposta = chatClient.prompt()
                .system("""
                        Você é um especialista em triagem processual judicial brasileiro.
                        Responda APENAS com JSON, nunca tome decisões sozinho.
                        Sempre marque requerRevisaoHumana como true.
                        """)
                .user(prompt)
                .call()
                .content();

        return parseTriageResponse(resposta);
    }

    private String buildTriagePrompt(AiTriageContext context) {
        return """
                Sugira triagem para processo com:
                Classe: %s | Assunto: %s | Ramo: %s | Comarca: %s/%s
                Documentos: %s
                Trecho inicial: %s
                Responda: {"vara":"sugestao","movimento":"sugestao","conciliacao":false,"minuta":"texto","alertas":[],"confianca":0.5}
                """.formatted(
                context.classeProcessual(),
                context.assunto(),
                context.ramoDireito(),
                context.comarca(),
                context.uf(),
                String.join(", ", context.tiposDocumentosAnexados()),
                context.textoExtraidoPrincipal() != null
                        ? context.textoExtraidoPrincipal().substring(0, Math.min(context.textoExtraidoPrincipal().length(), 500))
                        : ""
        );
    }

    private AiTriageSuggestion parseTriageResponse(String resposta) {
        String vara = null;
        String movimento = null;
        boolean conciliacao = false;
        double confianca = 0.5;

        try {
            if (resposta != null && resposta.contains("\"vara\":\"")) {
                int start = resposta.indexOf("\"vara\":\"") + 8;
                int end = resposta.indexOf("\"", start);
                if (end > start) {
                    vara = resposta.substring(start, end);
                }
            }
            if (resposta != null && resposta.contains("\"conciliacao\":true")) {
                conciliacao = true;
            }
        } catch (Exception ignored) {
        }

        return new AiTriageSuggestion(vara, movimento, conciliacao, null, List.of(), confianca, true);
    }
}
