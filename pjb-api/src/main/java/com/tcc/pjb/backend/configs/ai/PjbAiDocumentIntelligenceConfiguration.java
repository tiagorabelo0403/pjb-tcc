package com.tcc.pjb.backend.configs.ai;

import com.tcc.pjb.backend.core.digitalizacao.DocumentIntelligenceRequest;
import com.tcc.pjb.backend.core.digitalizacao.DocumentIntelligenceResult;
import com.tcc.pjb.backend.core.digitalizacao.PjbDocumentIntelligencePort;
import com.tcc.pjb.backend.core.peticionamento.triagem.AiTriageContext;
import com.tcc.pjb.backend.core.peticionamento.triagem.AiTriageSuggestion;
import com.tcc.pjb.backend.core.peticionamento.triagem.AiTriageSuggestionLayer;
import com.tcc.pjb.backend.core.peticionamento.triagem.LitigantePerfilPort;
import com.tcc.pjb.backend.core.peticionamento.triagem.LitiganteRiskNivel;
import com.tcc.pjb.backend.core.peticionamento.triagem.LitiganteRiskScore;
import com.tcc.pjb.backend.core.peticionamento.triagem.PjbAiTriageSuggestionPort;
import com.tcc.pjb.backend.service.processual.peticionamento.triagem.PjbSpringAiDocumentIntelligenceAdapter;
import com.tcc.pjb.backend.service.processual.peticionamento.triagem.PjbSpringAiTriageAdapter;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PjbAiDocumentIntelligenceConfiguration {

    @Bean
    @ConditionalOnProperty(name = "pjb.ai.spring-ai.enabled", havingValue = "true")
    public PjbDocumentIntelligencePort springAiDocumentIntelligencePort(ChatClient.Builder chatClientBuilder) {
        return new PjbSpringAiDocumentIntelligenceAdapter(chatClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean(PjbDocumentIntelligencePort.class)
    public PjbDocumentIntelligencePort noOpDocumentIntelligencePort() {
        return (DocumentIntelligenceRequest request) -> new DocumentIntelligenceResult(
                "",
                0.0,
                "DESCONHECIDO",
                List.of(),
                request.conteudo() != null && request.conteudo().length > 0,
                false
        );
    }

    @Bean
    @ConditionalOnProperty(name = "pjb.ai.spring-ai.enabled", havingValue = "true")
    public PjbAiTriageSuggestionPort springAiTriageSuggestionPort(ChatClient.Builder chatClientBuilder) {
        return new PjbSpringAiTriageAdapter(chatClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean(PjbAiTriageSuggestionPort.class)
    public PjbAiTriageSuggestionPort noOpTriageSuggestionPort() {
        return (AiTriageContext context) -> new AiTriageSuggestion(
                null,
                null,
                false,
                null,
                List.of(),
                0.0,
                true
        );
    }

    @Bean
    @ConditionalOnMissingBean(LitigantePerfilPort.class)
    public LitigantePerfilPort noOpLitigantePerfilPort() {
        return (String cpfCnpj) -> new LitiganteRiskScore(cpfCnpj, 0, 0.0, List.of(), LitiganteRiskNivel.BAIXO);
    }
}
