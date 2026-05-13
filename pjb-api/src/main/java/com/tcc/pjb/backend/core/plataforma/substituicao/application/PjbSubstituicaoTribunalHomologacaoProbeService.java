package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoHomologacaoProbeSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoTribunalHomologacaoProbeEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoTribunalHomologacaoProbeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoTribunalHomologacaoProbeService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final PjbSubstituicaoTribunalHomologacaoProbeRepository repository;
    private final PjbSubstituicaoNacionalExecucaoRepository execucaoRepository;
    private final ObjectMapper objectMapper;

    public PjbSubstituicaoTribunalHomologacaoProbeService(PjbSubstituicaoTribunalHomologacaoProbeRepository repository,
                                                          PjbSubstituicaoNacionalExecucaoRepository execucaoRepository,
                                                          ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.execucaoRepository = Objects.requireNonNull(execucaoRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public ProbeExecutionResult executar(Long execucaoId,
                                         String tribunalCodigo,
                                         boolean dryRun,
                                         String payloadJson,
                                         PjbSubstituicaoGateSnapshot gate) {
        Instant now = Instant.now();
        PjbSubstituicaoNacionalExecucaoEntity execucao = execucaoRepository.getReferenceById(execucaoId);
        List<ProbeDefinition> definitions = List.of(
                new ProbeDefinition("AUTH_CONNECTOR", gate.tribunal().connectorPreferido().name(), environment(dryRun), gate.tribunalReady() > 0 && gate.cryptoBlocked() == 0,
                        List.of("Conector preferido materializado", "Gate sem bloqueio criptográfico impeditivo")),
                new ProbeDefinition("PROTOCOLO_EXTERNO", gate.tribunal().sistemaJudicialFallback().name(), environment(dryRun), gate.productionReady() > 0 && gate.blockedSystems() == 0,
                        List.of("Há sistema production-ready para submissão assistida", "Nenhum bloqueio operacional ativo")),
                new ProbeDefinition("CONSULTA_PROCESSUAL", gate.tribunal().connectorPreferido().name(), environment(dryRun), gate.healthySystems() > 0,
                        List.of("Leitura nacional saudável", "Observabilidade de consulta operacional")),
                new ProbeDefinition("CERTIFICADO_ICP", "ICP_BRASIL", environment(dryRun), gate.certificateReady() > 0 && gate.cryptoBlocked() == 0,
                        List.of("Cadeia certificadora pronta", "Sem bloqueio de criptografia")),
                new ProbeDefinition("OBSERVABILIDADE_TRIBUNAL", gate.tribunal().connectorPreferido().name(), environment(dryRun), gate.healthySystems() > gate.blockedSystems(),
                        List.of("Healthy systems supera sistemas bloqueados", "Trilha observável por tribunal"))
        );
        ArrayList<Map<String, Object>> probes = new ArrayList<>();
        int aprovadas = 0;
        int bloqueadas = 0;
        int simuladas = 0;
        for (ProbeDefinition definition : definitions) {
            PjbSubstituicaoHomologacaoProbeSituacao situacao = dryRun
                    ? PjbSubstituicaoHomologacaoProbeSituacao.SIMULADA
                    : definition.aprovada()
                    ? PjbSubstituicaoHomologacaoProbeSituacao.APROVADA
                    : PjbSubstituicaoHomologacaoProbeSituacao.BLOQUEADA;
            if (situacao == PjbSubstituicaoHomologacaoProbeSituacao.APROVADA) {
                aprovadas++;
            } else if (situacao == PjbSubstituicaoHomologacaoProbeSituacao.BLOQUEADA) {
                bloqueadas++;
            } else {
                simuladas++;
            }
            LinkedHashMap<String, Object> evidencias = new LinkedHashMap<>();
            evidencias.put("gateScore", gate.gateScore());
            evidencias.put("gateAprovado", gate.gateAprovado());
            evidencias.put("tribunalReady", gate.tribunalReady());
            evidencias.put("healthySystems", gate.healthySystems());
            evidencias.put("certificateReady", gate.certificateReady());
            evidencias.put("fundamentos", definition.fundamentos());
            LinkedHashMap<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("situacao", situacao.name());
            resultado.put("conector", definition.connectorCodigo());
            resultado.put("ambiente", definition.ambienteCodigo());
            resultado.put("tribunalCodigo", tribunalCodigo);
            resultado.put("bloqueadores", definition.aprovada() ? List.of() : gate.blockers());
            upsert(execucao, tribunalCodigo, definition, situacao, gate.gateScore(), toJson(evidencias), toJson(resultado), now);
            LinkedHashMap<String, Object> probeResumo = new LinkedHashMap<>();
            probeResumo.put("probeCodigo", definition.probeCodigo());
            probeResumo.put("connectorCodigo", definition.connectorCodigo());
            probeResumo.put("ambienteCodigo", definition.ambienteCodigo());
            probeResumo.put("situacao", situacao.name());
            probeResumo.put("fundamentos", definition.fundamentos());
            probes.add(PjbSubstituicaoPayloadSupport.immutableMap(probeResumo));
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("totalProbes", definitions.size());
        result.put("aprovadas", aprovadas);
        result.put("bloqueadas", bloqueadas);
        result.put("simuladas", simuladas);
        result.put("probes", PjbSubstituicaoPayloadSupport.immutableList(probes));
        result.put("metadadosHomologacao", decodeMetadata(payloadJson));
        return new ProbeExecutionResult(aprovadas, bloqueadas, simuladas, PjbSubstituicaoPayloadSupport.immutableMap(result));
    }

    private void upsert(PjbSubstituicaoNacionalExecucaoEntity execucao,
                        String tribunalCodigo,
                        ProbeDefinition definition,
                        PjbSubstituicaoHomologacaoProbeSituacao situacao,
                        int gateScore,
                        String evidenciasJson,
                        String resultadoJson,
                        Instant now) {
        repository.findByExecucaoIdAndProbeCodigo(execucao.getId(), definition.probeCodigo())
                .ifPresentOrElse(existing -> {
                    existing.refresh(situacao, gateScore, evidenciasJson, resultadoJson, now);
                    repository.save(existing);
                }, () -> repository.save(new PjbSubstituicaoTribunalHomologacaoProbeEntity(
                        execucao,
                        tribunalCodigo,
                        definition.probeCodigo(),
                        definition.connectorCodigo(),
                        definition.ambienteCodigo(),
                        situacao,
                        gateScore,
                        evidenciasJson,
                        resultadoJson,
                        now,
                        now
                )));
    }

    private String environment(boolean dryRun) {
        return dryRun ? "DRY_RUN" : "ASSISTIDA";
    }

    private Map<String, Object> decodeMetadata(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) {
                return Map.of();
            }
            Map<String, Object> payload = objectMapper.readValue(payloadJson, MAP_TYPE);
            Object metadados = payload.get("metadados");
            return metadados instanceof Map<?, ?> map ? PjbSubstituicaoPayloadSupport.immutableMap(map) : Map.of();
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private record ProbeDefinition(String probeCodigo,
                                   String connectorCodigo,
                                   String ambienteCodigo,
                                   boolean aprovada,
                                   List<String> fundamentos) {
    }

    public record ProbeExecutionResult(int aprovadas,
                                       int bloqueadas,
                                       int simuladas,
                                       Map<String, Object> details) {
    }
}
