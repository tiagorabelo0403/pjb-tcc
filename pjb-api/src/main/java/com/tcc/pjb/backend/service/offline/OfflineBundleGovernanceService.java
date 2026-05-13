package com.tcc.pjb.backend.service.offline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.offline.PwaOfflineBundle;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.service.offline.domain.SyncGovernance;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OfflineBundleGovernanceService {

    private final ObjectMapper objectMapper;

    public OfflineBundleGovernanceService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public Map<String, Object> buildOfflineCapability(Processo processo,
                                                      Usuario usuario,
                                                      String escopo,
                                                      List<DocumentoProcessual> documentos,
                                                      List<MovimentacaoProcessual> movimentacoes) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        boolean magistratura = usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura();
        map.put("profile", magistratura ? "MAGISTRATURA_OFFLINE_SOBERANO" : "OFFLINE_PROCESSUAL_OPERACIONAL");
        map.put("escopo", escopo);
        map.put("magistratura", magistratura);
        map.put("decisionDraftEligible", magistratura);
        map.put("signatureReviewRequired", magistratura);
        map.put("documentSnapshotCount", documentos == null ? 0 : documentos.size());
        map.put("movementSnapshotCount", movimentacoes == null ? 0 : movimentacoes.size());
        map.put("conflictMode", magistratura ? "REVIEW_BEFORE_SIGNATURE" : "REPLAY_WITH_GOVERNANCE");
        map.put("latestMovementAt", latestMovement(movimentacoes));
        map.put("bundleWindowHours", 72);
        return Map.copyOf(map);
    }

    public SyncGovernance governSync(PwaOfflineBundle bundle,
                                     Processo processo,
                                     com.tcc.pjb.backend.service.offline.domain.SincronizarBundleRequest request) {
        List<Map<String, Object>> acoes = request.acoes() == null ? List.of() : request.acoes();
        long decisionActions = acoes.stream().filter(this::isDecisionAction).count();
        long signatureActions = acoes.stream().filter(this::isSignatureAction).count();
        boolean staleProcess = isProcessStaleAgainstBundle(bundle, processo);
        boolean deviceMismatch = request.deviceClock() != null && bundle.getDeviceFingerprint() != null && request.deviceClock().isBlank();
        boolean explicitConflict = request.conflitoResumo() != null && !request.conflitoResumo().isBlank();
        boolean requiredReview = decisionActions > 0L || signatureActions > 0L || staleProcess;
        String status = explicitConflict || requiredReview || deviceMismatch ? "PENDENTE_CONFLITO" : "SINCRONIZADO";
        String conflictSummary = explicitConflict
                ? request.conflitoResumo().trim()
                : requiredReview
                ? "Sincronização exige revisão governada antes de consolidar atos offline no processo." : null;
        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("profile", requiredReview ? "SYNC_REVIEW_GOVERNED" : "SYNC_FAST_FORWARD");
        envelope.put("actionCount", acoes.size());
        envelope.put("decisionActions", decisionActions);
        envelope.put("signatureActions", signatureActions);
        envelope.put("requiredReview", requiredReview);
        envelope.put("staleProcess", staleProcess);
        envelope.put("replayDigest", Hashes.sha256Hex(writeJson(acoes)));
        envelope.put("bundleAgeMinutes", bundle.getAbertoEm() == null ? 0L : Duration.between(bundle.getAbertoEm(), Instant.now()).toMinutes());
        if (processo != null && processo.getDataUltimaMovimentacao() != null) {
            envelope.put("latestProcessMovement", processo.getDataUltimaMovimentacao().atZone(ZoneId.systemDefault()).toInstant().toString());
        }
        return new SyncGovernance(status, conflictSummary, Map.copyOf(envelope));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseManifest(String manifestJson) {
        if (manifestJson == null || manifestJson.isBlank()) {
            return Map.of();
        }
        try {
            Object value = objectMapper.readValue(manifestJson, Object.class);
            if (value instanceof Map<?, ?> map) {
                LinkedHashMap<String, Object> out = new LinkedHashMap<>();
                map.forEach((key, item) -> out.put(String.valueOf(key), item));
                return Collections.unmodifiableMap(out);
            }
            return Map.of("raw", value);
        } catch (Exception ignored) {
            return Map.of("raw", manifestJson);
        }
    }

    private boolean isDecisionAction(Map<String, Object> action) {
        String token = actionToken(action);
        return token.contains("DESPACHO") || token.contains("DECISAO") || token.contains("SENTENCA");
    }

    private boolean isSignatureAction(Map<String, Object> action) {
        return actionToken(action).contains("ASSINAT");
    }

    private String actionToken(Map<String, Object> action) {
        if (action == null || action.isEmpty()) {
            return "";
        }
        String value = String.valueOf(action.getOrDefault("tipo", action.getOrDefault("type", action.getOrDefault("acao", ""))));
        return value.trim().toUpperCase();
    }

    private boolean isProcessStaleAgainstBundle(PwaOfflineBundle bundle, Processo processo) {
        if (bundle == null || processo == null || bundle.getAbertoEm() == null || processo.getDataUltimaMovimentacao() == null) {
            return false;
        }
        LocalDateTime lastMovement = processo.getDataUltimaMovimentacao();
        Instant processInstant = lastMovement.atZone(ZoneId.systemDefault()).toInstant();
        return processInstant.isAfter(bundle.getAbertoEm());
    }

    private String latestMovement(List<MovimentacaoProcessual> movimentacoes) {
        if (movimentacoes == null || movimentacoes.isEmpty()) {
            return null;
        }
        for (MovimentacaoProcessual movimentacao : movimentacoes) {
            if (movimentacao != null && movimentacao.getDataMovimentacao() != null) {
                return movimentacao.getDataMovimentacao().toString();
            }
        }
        return null;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar envelope offline.", e);
        }
    }
}
