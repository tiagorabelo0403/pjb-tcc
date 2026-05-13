package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoComunicacaoSyncSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoComunicacaoSyncCursorEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoComunicacaoSyncItemEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoComunicacaoSyncCursorRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoComunicacaoSyncItemRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoComunicacaoNacionalSyncService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final PjbSubstituicaoComunicacaoSyncCursorRepository cursorRepository;
    private final PjbSubstituicaoComunicacaoSyncItemRepository itemRepository;
    private final PjbSubstituicaoNacionalExecucaoRepository execucaoRepository;
    private final CanonicalJsonHasher canonicalJsonHasher;
    private final ObjectMapper objectMapper;

    public PjbSubstituicaoComunicacaoNacionalSyncService(PjbSubstituicaoComunicacaoSyncCursorRepository cursorRepository,
                                                         PjbSubstituicaoComunicacaoSyncItemRepository itemRepository,
                                                         PjbSubstituicaoNacionalExecucaoRepository execucaoRepository,
                                                         CanonicalJsonHasher canonicalJsonHasher,
                                                         ObjectMapper objectMapper) {
        this.cursorRepository = Objects.requireNonNull(cursorRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.execucaoRepository = Objects.requireNonNull(execucaoRepository);
        this.canonicalJsonHasher = Objects.requireNonNull(canonicalJsonHasher);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public CommunicationSyncExecutionResult executar(Long execucaoId,
                                                     String tribunalCodigo,
                                                     boolean dryRun,
                                                     String payloadJson,
                                                     PjbSubstituicaoGateSnapshot gate) {
        PjbSubstituicaoNacionalExecucaoEntity execucao = execucaoRepository.getReferenceById(execucaoId);
        SyncPlan plan = resolvePlan(payloadJson);
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ArrayList<Map<String, Object>> cursores = new ArrayList<>();
        int totalRecebido = 0;
        int totalDeduplicado = 0;
        int totalCorrelacionado = 0;
        int totalReprocessavel = 0;
        for (String canal : plan.canais()) {
            Instant janelaInicio = now.minus(plan.janelaHoras(), ChronoUnit.HOURS);
            Instant janelaFim = now;
            List<Envelope> envelopes = plan.envelopesPorCanal().getOrDefault(canal, List.of());
            CursorMetrics metrics = persistCursor(execucao, tribunalCodigo, dryRun, canal, janelaInicio, janelaFim, envelopes, gate);
            totalRecebido += metrics.totalRecebido();
            totalDeduplicado += metrics.totalDeduplicado();
            totalCorrelacionado += metrics.totalCorrelacionado();
            totalReprocessavel += metrics.totalReprocessavel();
            cursores.add(Map.of(
                    "canal", canal,
                    "janelaInicio", janelaInicio,
                    "janelaFim", janelaFim,
                    "situacao", metrics.situacao().name(),
                    "totalRecebido", metrics.totalRecebido(),
                    "totalDeduplicado", metrics.totalDeduplicado(),
                    "totalCorrelacionado", metrics.totalCorrelacionado(),
                    "totalReprocessavel", metrics.totalReprocessavel()
            ));
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("janelaHoras", plan.janelaHoras());
        result.put("canais", plan.canais());
        result.put("totalRecebido", totalRecebido);
        result.put("totalDeduplicado", totalDeduplicado);
        result.put("totalCorrelacionado", totalCorrelacionado);
        result.put("totalReprocessavel", totalReprocessavel);
        result.put("cursores", PjbSubstituicaoPayloadSupport.immutableList(cursores));
        return new CommunicationSyncExecutionResult(totalRecebido, totalDeduplicado, totalCorrelacionado, totalReprocessavel, PjbSubstituicaoPayloadSupport.immutableMap(result));
    }

    private CursorMetrics persistCursor(PjbSubstituicaoNacionalExecucaoEntity execucao,
                                        String tribunalCodigo,
                                        boolean dryRun,
                                        String canal,
                                        Instant janelaInicio,
                                        Instant janelaFim,
                                        List<Envelope> envelopes,
                                        PjbSubstituicaoGateSnapshot gate) {
        Instant now = Instant.now();
        String correlationNamespace = tribunalCodigo + ':' + canal + ":CORR";
        String dedupeNamespace = tribunalCodigo + ':' + canal + ":DEDUP";
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int correlacionados = 0;
        int reprocessaveis = 0;
        int deduplicados = 0;
        PjbSubstituicaoComunicacaoSyncSituacao cursorSituacao = resolveCursorSituacao(dryRun, gate, envelopes);
        PjbSubstituicaoComunicacaoSyncCursorEntity cursor = cursorRepository.findByExecucaoIdAndCanalOrigemAndJanelaInicioAndJanelaFim(execucao.getId(), canal, janelaInicio, janelaFim)
                .orElseGet(() -> cursorRepository.save(new PjbSubstituicaoComunicacaoSyncCursorEntity(
                        execucao,
                        tribunalCodigo,
                        canal,
                        janelaInicio,
                        janelaFim,
                        correlationNamespace,
                        dedupeNamespace,
                        cursorSituacao,
                        0,
                        0,
                        0,
                        0,
                        "{}",
                        now,
                        now
                )));
        for (Envelope envelope : envelopes) {
            LinkedHashMap<String, Object> fingerprintEnvelope = new LinkedHashMap<>();
            fingerprintEnvelope.put("canal", canal);
            fingerprintEnvelope.put("tribunalCodigo", tribunalCodigo);
            fingerprintEnvelope.put("externalMessageId", envelope.externalMessageId());
            fingerprintEnvelope.put("processoNumero", envelope.processoNumero());
            fingerprintEnvelope.put("payload", envelope.payload());
            String dedupeHash = canonicalJsonHasher.fingerprint(fingerprintEnvelope).sha256();
            boolean duplicated = !seen.add(dedupeHash);
            PjbSubstituicaoComunicacaoSyncSituacao itemSituacao = duplicated
                    ? PjbSubstituicaoComunicacaoSyncSituacao.DEDUPLICADO
                    : envelope.processoNumero() != null && !envelope.processoNumero().isBlank()
                    ? PjbSubstituicaoComunicacaoSyncSituacao.CORRELACIONADO
                    : PjbSubstituicaoComunicacaoSyncSituacao.REPROCESSAVEL;
            if (duplicated) {
                deduplicados++;
            }
            if (itemSituacao == PjbSubstituicaoComunicacaoSyncSituacao.CORRELACIONADO) {
                correlacionados++;
            }
            if (itemSituacao == PjbSubstituicaoComunicacaoSyncSituacao.REPROCESSAVEL) {
                reprocessaveis++;
            }
            boolean reprocessavel = itemSituacao == PjbSubstituicaoComunicacaoSyncSituacao.REPROCESSAVEL;
            LinkedHashMap<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("situacao", itemSituacao.name());
            resultado.put("duplicated", duplicated);
            resultado.put("reprocessavel", reprocessavel);
            resultado.put("canal", canal);
            String correlationKey = envelope.processoNumero() != null && !envelope.processoNumero().isBlank()
                    ? tribunalCodigo + ':' + envelope.processoNumero()
                    : tribunalCodigo + ':' + dedupeHash.substring(0, 24);
            upsertItem(cursor, dedupeHash, envelope.externalMessageId(), correlationKey, envelope.processoNumero(), itemSituacao, reprocessavel, toJson(envelope.payload()), toJson(resultado), now);
        }
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("canal", canal);
        snapshot.put("janelaInicio", janelaInicio);
        snapshot.put("janelaFim", janelaFim);
        snapshot.put("situacao", cursorSituacao.name());
        snapshot.put("totalRecebido", envelopes.size());
        snapshot.put("totalDeduplicado", deduplicados);
        snapshot.put("totalCorrelacionado", correlacionados);
        snapshot.put("totalReprocessavel", reprocessaveis);
        cursor.refresh(cursorSituacao, envelopes.size(), deduplicados, correlacionados, reprocessaveis, toJson(snapshot), now);
        cursorRepository.save(cursor);
        return new CursorMetrics(cursorSituacao, envelopes.size(), deduplicados, correlacionados, reprocessaveis);
    }

    private void upsertItem(PjbSubstituicaoComunicacaoSyncCursorEntity cursor,
                            String dedupeHash,
                            String externalMessageId,
                            String correlationKey,
                            String processoNumero,
                            PjbSubstituicaoComunicacaoSyncSituacao situacao,
                            boolean reprocessavel,
                            String payloadJson,
                            String resultadoJson,
                            Instant now) {
        itemRepository.findByCursorIdAndDedupeHash(cursor.getId(), dedupeHash)
                .ifPresentOrElse(existing -> {
                    existing.refresh(situacao, reprocessavel, resultadoJson, now);
                    itemRepository.save(existing);
                }, () -> itemRepository.save(new PjbSubstituicaoComunicacaoSyncItemEntity(
                        cursor,
                        dedupeHash,
                        externalMessageId,
                        correlationKey,
                        processoNumero,
                        situacao,
                        reprocessavel,
                        payloadJson,
                        resultadoJson,
                        now,
                        now
                )));
    }

    private PjbSubstituicaoComunicacaoSyncSituacao resolveCursorSituacao(boolean dryRun,
                                                                         PjbSubstituicaoGateSnapshot gate,
                                                                         List<Envelope> envelopes) {
        if (dryRun) {
            return PjbSubstituicaoComunicacaoSyncSituacao.SIMULADO;
        }
        if (gate.blockedFor(com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao.SINCRONIZAR_COMUNICACOES_NACIONAIS)) {
            return PjbSubstituicaoComunicacaoSyncSituacao.BLOQUEADO;
        }
        if (envelopes.isEmpty()) {
            return PjbSubstituicaoComunicacaoSyncSituacao.PLANEJADO;
        }
        return PjbSubstituicaoComunicacaoSyncSituacao.CORRELACIONADO;
    }

    private SyncPlan resolvePlan(String payloadJson) {
        try {
            Map<String, Object> payload = payloadJson == null || payloadJson.isBlank()
                    ? Map.of()
                    : objectMapper.readValue(payloadJson, MAP_TYPE);
            Map<String, Object> metadados = payload.get("metadados") instanceof Map<?, ?> raw ? safeMap(raw) : Map.of();
            int janelaHoras = positiveInt(metadados.get("janelaHoras"), 12);
            List<String> canais = resolveCanais(metadados.get("canais"));
            List<Envelope> envelopes = resolveEnvelopes(metadados.get("envelopes"));
            LinkedHashMap<String, List<Envelope>> porCanal = new LinkedHashMap<>();
            for (String canal : canais) {
                porCanal.put(canal, envelopes.stream().filter(item -> canal.equals(item.canal())).toList());
            }
            return new SyncPlan(Math.max(1, janelaHoras), List.copyOf(canais), java.util.Collections.unmodifiableMap(porCanal));
        } catch (Exception ex) {
            List<String> canais = List.of("DJEN", "DOMICILIO", "MNI");
            return new SyncPlan(12, canais, Map.of("DJEN", List.of(), "DOMICILIO", List.of(), "MNI", List.of()));
        }
    }

    private List<String> resolveCanais(Object raw) {
        if (raw instanceof List<?> list) {
            ArrayList<String> canais = new ArrayList<>();
            for (Object value : list) {
                if (value != null && !String.valueOf(value).isBlank()) {
                    canais.add(String.valueOf(value).trim().toUpperCase());
                }
            }
            return canais.isEmpty() ? List.of("DJEN", "DOMICILIO", "MNI") : List.copyOf(new LinkedHashSet<>(canais));
        }
        return List.of("DJEN", "DOMICILIO", "MNI");
    }

    private List<Envelope> resolveEnvelopes(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<Envelope> envelopes = new ArrayList<>();
        for (Object value : list) {
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> item = safeMap(map);
                String canal = text(item.get("canal"));
                if (canal == null) {
                    continue;
                }
                envelopes.add(new Envelope(
                        canal.toUpperCase(),
                        text(item.get("externalMessageId")),
                        text(item.get("processoNumero")),
                        item
                ));
            }
        }
        return List.copyOf(envelopes);
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

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private record SyncPlan(int janelaHoras,
                            List<String> canais,
                            Map<String, List<Envelope>> envelopesPorCanal) {
    }

    private record Envelope(String canal,
                            String externalMessageId,
                            String processoNumero,
                            Map<String, Object> payload) {
    }

    private record CursorMetrics(PjbSubstituicaoComunicacaoSyncSituacao situacao,
                                 int totalRecebido,
                                 int totalDeduplicado,
                                 int totalCorrelacionado,
                                 int totalReprocessavel) {
    }

    public record CommunicationSyncExecutionResult(int totalRecebido,
                                                   int totalDeduplicado,
                                                   int totalCorrelacionado,
                                                   int totalReprocessavel,
                                                   Map<String, Object> details) {
    }
}
