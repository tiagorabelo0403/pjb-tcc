package com.tcc.pjb.backend.adapter.worker;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.adapter.factory.PJeAdapterFactory;
import com.tcc.pjb.backend.adapter.strategies.IPJeAdapter;
import com.tcc.pjb.backend.workflow.zeebe.ZeebeCompat;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.JobWorker;

@Profile({"dev", "test"})
@Component
@SuppressWarnings({"removal","deprecation"})
@ConditionalOnProperty(prefix = "pjb.integrations.pje.submission", name = "mock-enabled", havingValue = "true", matchIfMissing = false)
public class PJeSubmissionWorker {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PJeSubmissionWorker.class);

    private final PJeAdapterFactory adapterFactory;

    public PJeSubmissionWorker(PJeAdapterFactory adapterFactory) {
        this.adapterFactory = adapterFactory;
    }

    @SuppressWarnings({"removal", "deprecation"})
    @JobWorker(type = "TAREFA_SUBMETER_PROCESSO_LEGADO", autoComplete = false)
    public void handleSubmission(final JobClient client, final ActivatedJob job) {

        final Map<String, Object> variables = job.getVariablesAsMap();
        final String correlationId = getCorrelationId(variables);
        final Map<String, Object> orgao = getOrgaoConfig(variables);
        final String adapterKey = getAdapterKey(orgao, correlationId);

        log.info("[PJE_SUBMISSAO] [{}] Iniciando submissão via adaptador '{}'", correlationId, adapterKey);

        try {

            final IPJeAdapter adapter = adapterFactory.getAdapter(adapterKey);

            final String numeroProcessoTribunal = "0700123-45.2025.8.06.0001";

            ZeebeCompat.await(client
                    .newCompleteCommand(job.getKey())
                    .variables(Map.of(
                            "numeroProcessoLegado", numeroProcessoTribunal,
                            "statusSubmissao", "SUCESSO"
                    ))
                    .send());

            log.info("[PJE_SUBMISSAO] [{}] Submissão concluída com sucesso. Nº Tribunal: {}",
                    correlationId, numeroProcessoTribunal);

        } catch (Exception ex) {

            final String errorMsg = String.format(
                    "Falha ao submeter processo via adaptador '%s'. Motivo: %s",
                    adapterKey, ex.getMessage()
            );

            log.error("[PJE_SUBMISSAO] [{}] {}", correlationId, errorMsg, ex);

            ZeebeCompat.await(client
                    .newThrowErrorCommand(job.getKey())
                    .errorCode("ERRO_ADAPTADOR_PJE")
                    .errorMessage(errorMsg)
                    .send());
        }
    }
    private String getCorrelationId(Map<String, Object> vars) {
        String id = (String) vars.get("correlationId");
        if (id == null) {
            throw new IllegalArgumentException("Variável 'correlationId' não encontrada no job.");
        }
        return id;
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrgaoConfig(Map<String, Object> vars) {
        Map<String, Object> orgao = (Map<String, Object>) vars.get("orgaoCompleto");
        if (orgao == null) {
            throw new IllegalArgumentException("Variável 'orgaoCompleto' ausente nas variáveis do job.");
        }
        return orgao;
    }
    private String getAdapterKey(Map<String, Object> orgao, String correlationId) {
        String key = (String) orgao.get("adapterBeanName");
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "[" + correlationId + "] Orgao sem 'adapterBeanName'. Configuração inválida."
            );
        }
        return key;
    }
}
