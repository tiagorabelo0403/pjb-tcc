package com.tcc.pjb.backend.workflow.consumer;

import java.util.LinkedHashMap;
import com.tcc.pjb.backend.core.lgpd.PjbProcessoSigiloRlsEntryPointSupport;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.configs.PjbFeatureFlagsProperties;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;

@Component
public class ProcessoAjuizadoWorkflowBridge {

    private static final Logger log = LoggerFactory.getLogger(ProcessoAjuizadoWorkflowBridge.class);

    private final ObjectProvider<ComandoAjuizamentoConsumer> consumerProvider;
    private final PjbFeatureFlagsProperties featureFlagsProperties;
    private final PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport;

    public ProcessoAjuizadoWorkflowBridge(ObjectProvider<ComandoAjuizamentoConsumer> consumerProvider,
                                          PjbFeatureFlagsProperties featureFlagsProperties,
                                          PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport) {
        this.consumerProvider = Objects.requireNonNull(consumerProvider, "consumerProvider");
        this.featureFlagsProperties = Objects.requireNonNull(featureFlagsProperties, "featureFlagsProperties");
        this.processoSigiloRlsEntryPointSupport = Objects.requireNonNull(processoSigiloRlsEntryPointSupport, "processoSigiloRlsEntryPointSupport");
    }

    @EventListener
    public void onProcessoAjuizado(ProcessoAjuizadoEvent event) {
        if (event == null || event.getProcessoId() == null) {
            return;
        }
        if (!featureFlagsProperties.getWorkflow().isEnabled()) {
            return;
        }
        ComandoAjuizamentoConsumer consumer = consumerProvider.getIfAvailable();
        if (consumer == null) {
            return;
        }
        try {
            processoSigiloRlsEntryPointSupport.runWithProcessoContext(
                    event.getProcessoId(),
                    "EVENT",
                    () -> consumer.startAjuizamento(buildWorkflowVariables(event))
            );
        } catch (Exception ex) {
            log.warn("Falha nao bloqueante ao iniciar workflow de ajuizamento. processoId={} erro={}", event.getProcessoId(), ex.getMessage());
        }
    }

    private Map<String, Object> buildWorkflowVariables(ProcessoAjuizadoEvent event) {
        LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
        variables.put("processoId", event.getProcessoId());
        variables.put("numeroUnificado", normalize(event.getNumeroUnificado()));
        variables.put("ramoDireito", normalize(event.getRamoDireito()));
        variables.put("rito", normalize(event.getRito()));
        variables.put("tipoJustica", normalize(event.getTipoJustica()));
        variables.put("classeProcessual", normalize(event.getClasseProcessual()));
        variables.put("assunto", normalize(event.getAssunto()));
        variables.put("valorCausa", event.getValorCausa());
        variables.put("comarca", normalize(event.getComarca()));
        variables.put("uf", normalize(event.getUf()));
        variables.put("tribunalCodigo", normalize(event.getTribunalCodigo()));
        variables.put("unidadeJudiciariaCodigo", normalize(event.getUnidadeJudiciariaCodigo()));
        variables.put("criadoEm", event.getCriadoEm() != null ? event.getCriadoEm().toString() : null);
        variables.put("quantidadeAnexos", event.getAnexos() != null ? event.getAnexos().size() : 0);
        return Map.copyOf(variables);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
