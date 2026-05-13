package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoMigracaoLoteSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoMigracaoLoteEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoMigracaoLoteRepository;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
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
public class PjbSubstituicaoMigracaoIndustrialBatchService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final PjbSubstituicaoMigracaoLoteRepository repository;
    private final PjbSubstituicaoNacionalExecucaoRepository execucaoRepository;
    private final CanonicalJsonHasher canonicalJsonHasher;
    private final ObjectMapper objectMapper;

    public PjbSubstituicaoMigracaoIndustrialBatchService(PjbSubstituicaoMigracaoLoteRepository repository,
                                                         PjbSubstituicaoNacionalExecucaoRepository execucaoRepository,
                                                         CanonicalJsonHasher canonicalJsonHasher,
                                                         ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.execucaoRepository = Objects.requireNonNull(execucaoRepository);
        this.canonicalJsonHasher = Objects.requireNonNull(canonicalJsonHasher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public MigrationExecutionResult executar(Long execucaoId,
                                             String tribunalCodigo,
                                             boolean dryRun,
                                             String requestHash,
                                             String payloadJson,
                                             PjbSubstituicaoGateSnapshot gate) {
        PjbSubstituicaoNacionalExecucaoEntity execucao = execucaoRepository.getReferenceById(execucaoId);
        MigrationPlan plan = resolvePlan(payloadJson);
        Instant now = Instant.now();
        ArrayList<Map<String, Object>> lotes = new ArrayList<>();
        int reconciliados = 0;
        int bloqueados = 0;
        int simulados = 0;
        for (int index = 0; index < plan.totalLotes(); index++) {
            int ordem = index + 1;
            int totalItens = plan.itemsNoLote(index);
            String loteCodigo = "LOTE-" + tribunalCodigo + '-' + String.format("%04d", ordem);
            String faixa = plan.faixaReferencia(index);
            LinkedHashMap<String, Object> fingerprintEnvelope = new LinkedHashMap<>();
            fingerprintEnvelope.put("requestHash", requestHash);
            fingerprintEnvelope.put("loteCodigo", loteCodigo);
            fingerprintEnvelope.put("faixa", faixa);
            fingerprintEnvelope.put("totalItens", totalItens);
            String checksumEsperado = canonicalJsonHasher.fingerprint(fingerprintEnvelope).sha256();
            PjbSubstituicaoMigracaoLoteSituacao situacao = resolveSituacao(dryRun, gate, plan.usouLoteControle());
            String checksumApurado = situacao == PjbSubstituicaoMigracaoLoteSituacao.BLOQUEADO ? null : checksumEsperado;
            int divergencias = plan.divergencias(index, situacao == PjbSubstituicaoMigracaoLoteSituacao.BLOQUEADO);
            LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("loteCodigo", loteCodigo);
            snapshot.put("ordem", ordem);
            snapshot.put("faixaReferencia", faixa);
            snapshot.put("totalItens", totalItens);
            snapshot.put("checksumEsperado", checksumEsperado);
            snapshot.put("checksumApurado", checksumApurado);
            snapshot.put("divergencias", divergencias);
            snapshot.put("situacao", situacao.name());
            snapshot.put("controlBatch", totalItens == 0);
            upsert(execucao, tribunalCodigo, loteCodigo, ordem, faixa, totalItens, situacao, checksumEsperado, checksumApurado, divergencias, toJson(snapshot), now);
            if (situacao == PjbSubstituicaoMigracaoLoteSituacao.RECONCILIADO) {
                reconciliados++;
            } else if (situacao == PjbSubstituicaoMigracaoLoteSituacao.BLOQUEADO) {
                bloqueados++;
            } else if (situacao == PjbSubstituicaoMigracaoLoteSituacao.SIMULADO) {
                simulados++;
            }
            LinkedHashMap<String, Object> loteResumo = new LinkedHashMap<>();
            loteResumo.put("loteCodigo", loteCodigo);
            loteResumo.put("ordem", ordem);
            loteResumo.put("totalItens", totalItens);
            loteResumo.put("faixaReferencia", faixa);
            loteResumo.put("situacao", situacao.name());
            loteResumo.put("divergencias", divergencias);
            loteResumo.put("checksumEsperado", checksumEsperado);
            loteResumo.put("checksumApurado", checksumApurado);
            lotes.add(PjbSubstituicaoPayloadSupport.immutableMap(loteResumo));
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("totalLotes", plan.totalLotes());
        result.put("totalItensPlanejados", plan.totalItensPlanejados());
        result.put("reconciliados", reconciliados);
        result.put("bloqueados", bloqueados);
        result.put("simulados", simulados);
        result.put("loteControleTecnico", plan.usouLoteControle());
        result.put("lotes", PjbSubstituicaoPayloadSupport.immutableList(lotes));
        return new MigrationExecutionResult(reconciliados, bloqueados, simulados, PjbSubstituicaoPayloadSupport.immutableMap(result));
    }

    private PjbSubstituicaoMigracaoLoteSituacao resolveSituacao(boolean dryRun,
                                                                PjbSubstituicaoGateSnapshot gate,
                                                                boolean loteControleTecnico) {
        if (dryRun || loteControleTecnico) {
            return PjbSubstituicaoMigracaoLoteSituacao.SIMULADO;
        }
        if (gate.blockedFor(com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao.INICIAR_MIGRACAO_SOMBRA)) {
            return PjbSubstituicaoMigracaoLoteSituacao.BLOQUEADO;
        }
        return PjbSubstituicaoMigracaoLoteSituacao.RECONCILIADO;
    }

    private void upsert(PjbSubstituicaoNacionalExecucaoEntity execucao,
                        String tribunalCodigo,
                        String loteCodigo,
                        int loteOrdem,
                        String faixaReferencia,
                        int totalItens,
                        PjbSubstituicaoMigracaoLoteSituacao situacao,
                        String checksumEsperado,
                        String checksumApurado,
                        int divergencias,
                        String snapshotJson,
                        Instant now) {
        repository.findByExecucaoIdAndLoteCodigo(execucao.getId(), loteCodigo)
                .ifPresentOrElse(existing -> {
                    existing.refresh(situacao, checksumApurado, divergencias, snapshotJson, now);
                    repository.save(existing);
                }, () -> repository.save(new PjbSubstituicaoMigracaoLoteEntity(
                        execucao,
                        tribunalCodigo,
                        loteCodigo,
                        loteOrdem,
                        faixaReferencia,
                        totalItens,
                        situacao,
                        checksumEsperado,
                        checksumApurado,
                        divergencias,
                        snapshotJson,
                        now,
                        now
                )));
    }

    private MigrationPlan resolvePlan(String payloadJson) {
        try {
            Map<String, Object> payload = payloadJson == null || payloadJson.isBlank()
                    ? Map.of()
                    : objectMapper.readValue(payloadJson, MAP_TYPE);
            Map<String, Object> metadados = payload.get("metadados") instanceof Map<?, ?> raw ? safeMap(raw) : Map.of();
            int totalItens = positiveInt(metadados.get("processosEstimados"), 0);
            int tamanhoLote = positiveInt(metadados.get("tamanhoLote"), 250);
            int lotesDesejados = positiveInt(metadados.get("lotesDesejados"), 0);
            int totalLotes = lotesDesejados > 0
                    ? Math.max(1, lotesDesejados)
                    : totalItens > 0
                    ? Math.max(1, (int) Math.ceil(totalItens / (double) tamanhoLote))
                    : 1;
            return new MigrationPlan(totalItens, tamanhoLote, Math.min(totalLotes, 64), totalItens == 0);
        } catch (Exception ex) {
            return new MigrationPlan(0, 250, 1, true);
        }
    }

    private int positiveInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value instanceof String text) {
            try {
                return Math.max(0, Integer.parseInt(text.trim()));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private Map<String, Object> safeMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                out.put(String.valueOf(key), value);
            }
        });
        return PjbSubstituicaoPayloadSupport.immutableMap(out);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private record MigrationPlan(int totalItensPlanejados,
                                 int tamanhoLote,
                                 int totalLotes,
                                 boolean usouLoteControle) {

        int itemsNoLote(int index) {
            if (totalItensPlanejados == 0) {
                return 0;
            }
            int remaining = totalItensPlanejados - (index * tamanhoLote);
            return Math.max(0, Math.min(tamanhoLote, remaining));
        }

        String faixaReferencia(int index) {
            int inicio = (index * tamanhoLote) + 1;
            int fim = totalItensPlanejados == 0 ? 0 : Math.min(totalItensPlanejados, (index + 1) * tamanhoLote);
            return totalItensPlanejados == 0 ? "CONTROLE_TECNICO" : inicio + "-" + fim;
        }

        int divergencias(int index, boolean bloqueado) {
            if (bloqueado) {
                return Math.max(1, index == 0 ? 1 : 0);
            }
            return 0;
        }
    }

    public record MigrationExecutionResult(int reconciliados,
                                           int bloqueados,
                                           int simulados,
                                           Map<String, Object> details) {
    }
}
