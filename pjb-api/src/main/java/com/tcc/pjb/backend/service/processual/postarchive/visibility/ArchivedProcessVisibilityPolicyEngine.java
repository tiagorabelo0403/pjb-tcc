package com.tcc.pjb.backend.service.processual.postarchive.visibility;

import com.tcc.pjb.backend.model.dto.transito.PostArchiveLifecycleRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ArchivedProcessVisibilityPolicyEngine {

    private ArchivedProcessVisibilityPolicyEngine() {
    }

    public static ArchivedProcessVisibilityPolicyReport analyze(Processo processo,
                                                                PostArchiveLifecycleRequest request,
                                                                Map<String, Object> snapshot) {
        boolean archived = processo != null && processo.getStatusProcesso() == StatusProcesso.ARQUIVADO;
        NivelSigilo sigilo = processo != null && processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;
        RamoDireito ramo = processo != null ? processo.getRamoDireito() : null;
        int suggestedHideAfterDays = resolveHideAfterDays(sigilo, ramo, processo);
        ArchivedProcessVisibilityMode mode = resolveMode(archived, sigilo, ramo, snapshot);
        boolean partyAuthorizationPreferred = mode == ArchivedProcessVisibilityMode.CONCEALED_PARTY_GATE;
        LinkedHashSet<String> requesterProfiles = new LinkedHashSet<>();
        if (mode == ArchivedProcessVisibilityMode.VISIBLE) {
            requesterProfiles.add("PUBLICO_OU_PARTES_CONFORME_ABAC");
        } else if (partyAuthorizationPreferred) {
            requesterProfiles.add("CIDADAO_PARTE");
            requesterProfiles.add("ADVOGADO_HABILITADO");
            requesterProfiles.add("MAGISTRADO");
            requesterProfiles.add("SERVIDOR_AUTORIZADO");
        } else {
            requesterProfiles.add("MAGISTRADO");
            requesterProfiles.add("SERVIDOR_AUTORIZADO");
            requesterProfiles.add("PARTE_COM_JUSTIFICATIVA_QUALIFICADA");
        }
        List<String> alerts = new ArrayList<>();
        if (archived && mode.requiresControlledAccess()) {
            alerts.add("Arquivamento maduro recomenda ocultação operacional, sem supressão do acervo digital ou probatório.");
            alerts.add("A reexposição deve passar por trilha autenticada, justificativa e auditoria." );
        }
        if (sigilo.exigeCredencial()) {
            alerts.add("Sigilo processual eleva o nível de controle para consulta posterior ao arquivamento.");
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.INFANCIA_JUVENTUDE || ramo == RamoDireito.FAMILIA) {
            alerts.add("Ramo sensível recomenda limitação reforçada de indexação e de recuperação ampla após arquivamento.");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("archived", archived);
        metadata.put("sigilo", sigilo.name());
        metadata.put("ramoDireito", ramo != null ? ramo.name() : null);
        metadata.put("suggestedHideAfterDays", suggestedHideAfterDays);
        metadata.put("archiveAgeDays", archiveAgeDays(processo));
        metadata.put("snapshotTerminalDisposition", snapshot == null ? null : Objects.toString(snapshot.get("terminalDisposition"), null));
        metadata.put("requestReativar", request != null && request.reativar());
        metadata.put("policyNature", "OPERACIONAL_CONSERVADORA");
        metadata.put("destructiveDeletion", false);
        metadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return new ArchivedProcessVisibilityPolicyReport(
                archived,
                mode,
                suggestedHideAfterDays,
                mode.requiresControlledAccess(),
                partyAuthorizationPreferred,
                List.copyOf(requesterProfiles),
                List.copyOf(alerts),
                Collections.unmodifiableMap(metadata)
        );
    }

    private static ArchivedProcessVisibilityMode resolveMode(boolean archived,
                                                             NivelSigilo sigilo,
                                                             RamoDireito ramo,
                                                             Map<String, Object> snapshot) {
        if (!archived) {
            return ArchivedProcessVisibilityMode.VISIBLE;
        }
        if (sigilo.exigeCredencial()) {
            return ArchivedProcessVisibilityMode.CONCEALED_SENSITIVE_GATE;
        }
        String closureMode = snapshot == null ? null : Objects.toString(snapshot.get("currentClosureMode"), null);
        if (containsAny(closureMode, "SIGILO", "RESTRITO")) {
            return ArchivedProcessVisibilityMode.CONCEALED_SENSITIVE_GATE;
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.INFANCIA_JUVENTUDE || ramo == RamoDireito.FAMILIA) {
            return ArchivedProcessVisibilityMode.CONCEALED_INSTITUTIONAL_GATE;
        }
        return ArchivedProcessVisibilityMode.CONCEALED_PARTY_GATE;
    }

    private static int resolveHideAfterDays(NivelSigilo sigilo,
                                            RamoDireito ramo,
                                            Processo processo) {
        int base = 45;
        if (sigilo.exigeCredencial()) {
            base = 0;
        } else if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            base = 15;
        } else if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR || ramo == RamoDireito.ELEITORAL) {
            base = 30;
        } else if (ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.PREVIDENCIARIO) {
            base = 60;
        }
        if (processo != null && processo.getResultadoFinal() != null && containsAny(processo.getResultadoFinal(), "ACORDO", "DESISTENCIA", "HOMOLOGACAO")) {
            base = Math.min(base, 30);
        }
        return base;
    }

    private static long archiveAgeDays(Processo processo) {
        LocalDateTime ref = processo != null ? firstNonNull(processo.getDataAtualizacao(), processo.getDataDistribuicao(), processo.getDataCriacao()) : null;
        if (ref == null) {
            return 0L;
        }
        return Math.max(0L, ChronoUnit.DAYS.between(ref, LocalDateTime.now()));
    }

    private static LocalDateTime firstNonNull(LocalDateTime... values) {
        if (values == null) {
            return null;
        }
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static boolean containsAny(String source, String... needles) {
        if (source == null || source.isBlank() || needles == null) {
            return false;
        }
        String normalized = source.toUpperCase();
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalized.contains(needle.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
