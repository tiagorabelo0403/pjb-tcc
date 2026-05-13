package com.tcc.pjb.backend.ai.juridica.v1;

import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.workflow.zeebe.ZeebeCompat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.core.util.SafeMaps;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@SuppressWarnings({"removal","deprecation","unchecked"})
public class TriagemRAGWorker {

    private final AiModelClient aiModelV1;
    private final VectorSearchService vectorSearchService;

    public TriagemRAGWorker(@Qualifier("aiModelV1") AiModelClient aiModelV1,
                            VectorSearchService vectorSearchService) {
        this.aiModelV1 = aiModelV1;
        this.vectorSearchService = vectorSearchService;
    }

    @SuppressWarnings({"removal", "deprecation"})
    @JobWorker(
            type = "TAREFA_IA_TRIAGEM_INICIAL_V1",
            autoComplete = false
    )
    public void handleTriagem(final JobClient client, final ActivatedJob job) {

        Map<String, Object> vars = job.getVariablesAsMap();

        String correlationId = Objects.toString(vars.get("correlationId"), "N/A");
        String assunto = Objects.toString(vars.get("assunto"), "");

        Map<String, Object> jurisdicao =
                (Map<String, Object>) vars.get("jurisdicaoCompleta");

        String materia = Objects.toString(
                jurisdicao != null ? jurisdicao.get("materia") : null,
                ""
        );

        log.info("[{}] IA-V1 Triagem iniciada | materiaSha256={} materiaLen={}",
                correlationId,
                Hashes.sha256Hex(materia),
                (materia == null ? 0 : materia.length()));

        
        
        
        String contextoJuridico = vectorSearchService.searchSimilar(
                assunto,
                SafeMaps.of("materia", materia),
                10
        );

        
        
        
        String prompt = """
                Você é uma IA jurídica de triagem inicial (IA-V1).

                Analise os dados abaixo e responda EXCLUSIVAMENTE em JSON válido.

                Entrada:
                {
                  "assunto": "%s",
                  "materia": "%s",
                  "contextoJuridico": "%s"
                }

                Retorne:
                {
                  "classificacao": "string",
                  "keywords": ["string"],
                  "documentosFaltantesSugeridos": ["string"]
                }
                """.formatted(
                sanitize(assunto),
                sanitize(materia),
                sanitize(contextoJuridico)
        );

        String respostaJson = Objects.toString(aiModelV1.generate(prompt), "{}");

        
        
        
        ZeebeCompat.await(client.newCompleteCommand(job.getKey())
                .variables(SafeMaps.of("analiseTriagemV1", respostaJson))
                .send());

        log.info("[{}] IA-V1 Triagem concluída com sucesso", correlationId);
    }

    private String sanitize(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
