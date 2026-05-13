package com.tcc.pjb.backend.core.processo.evidencia.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaAggregate;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaConsulta;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ProcessoEvidenciaApplicationService {

    private final ProcessoEvidenciaMeshEngine engine;
    private final DecisionTraceService decisionTraceService;
    private final ObjectMapper objectMapper;

    public ProcessoEvidenciaApplicationService(ProcessoEvidenciaMeshEngine engine,
                                               ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                               ObjectMapper objectMapper) {
        this.engine = Objects.requireNonNull(engine);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public ProcessoEvidenciaAggregate analisar(ProcessoEvidenciaConsulta consulta) {
        ProcessoEvidenciaAggregate aggregate = engine.analisar(consulta);
        registrarTrace(consulta, aggregate);
        return aggregate;
    }

    private void registrarTrace(ProcessoEvidenciaConsulta consulta, ProcessoEvidenciaAggregate aggregate) {
        if (decisionTraceService == null) {
            return;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoIdRaiz", aggregate.processoIdRaiz());
        metadata.put("documentoIdRaiz", aggregate.documentoIdRaiz());
        metadata.put("processosCorrelatos", aggregate.processosCorrelatos());
        metadata.put("haCompartilhamentoInterfeitos", aggregate.haCompartilhamentoInterfeitos());
        metadata.put("origemSolicitacao", consulta.origemSolicitacao());
        metadata.put("solicitante", consulta.solicitante());
        String outputDigest = Hashes.sha256Hex(aggregate.sha256Raiz() + "#" + aggregate.processosCorrelatos() + "#" + aggregate.itens().size());
        decisionTraceService.record(
                "MALHA_EVIDENCIA_NACIONAL",
                "DOCUMENTO_PROCESSUAL",
                aggregate.documentoIdRaiz() == null ? null : aggregate.documentoIdRaiz().toString(),
                BigDecimal.valueOf(aggregate.haCompartilhamentoInterfeitos() ? 0.95d : 0.73d),
                toJson(aggregate.fundamentos()),
                toJson(aggregate.itens()),
                Hashes.sha256Hex(String.valueOf(aggregate.processoIdRaiz()) + "#" + Objects.toString(consulta.solicitante(), "")),
                outputDigest,
                "PJB_EVIDENCIA_V1",
                toJson(metadata)
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
