package com.tcc.pjb.backend.service.processual.postarchive.tombstone;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.processual.postarchive.visibility.ArchivedProcessVisibilityPolicyEngine;
import com.tcc.pjb.backend.service.processual.postarchive.visibility.ArchivedProcessVisibilityPolicyReport;
import com.tcc.pjb.backend.model.entity.cidadao.ProcessoVisibilidadePessoalOverride;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.identity.ProcessoVinculoNacional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoTombstonePolicyEngine {

    private static final int ARCHIVE_HIDE_DAYS_SENSITIVE = 365;
    private static final int ARCHIVE_HIDE_DAYS_STANDARD = 730;

    public ProcessoTombstonePolicyReport evaluate(Processo processoLocal,
                                                  ProcessoVinculoNacional vinculo,
                                                  ProcessoVisibilidadePessoalOverride override,
                                                  Instant reference) {
        Instant now = reference == null ? Instant.now() : reference;
        boolean archived = isArchived(processoLocal, vinculo);
        boolean reexposed = override != null && override.isVisivel() && override.ativa(now);
        int hideAfterDays = archiveHideAfterDays(processoLocal, vinculo);
        long ageDays = archiveAgeDays(processoLocal, vinculo);
        ArchivedProcessVisibilityPolicyReport visibilityReport = processoLocal == null
                ? null
                : ArchivedProcessVisibilityPolicyEngine.analyze(processoLocal, null, Map.of());
        boolean controlledAccessRequired = visibilityReport != null && visibilityReport.controlledAccessRequired();
        boolean hiddenByPolicy = archived && ageDays >= hideAfterDays;
        if (controlledAccessRequired && archived && ageDays >= Math.min(hideAfterDays, ARCHIVE_HIDE_DAYS_SENSITIVE)) {
            hiddenByPolicy = true;
        }
        ProcessoTombstoneStatus status;
        boolean visiblePanel;
        if (!archived) {
            status = reexposed ? ProcessoTombstoneStatus.ACTIVE_REEXPOSED_BY_SECRETARIAT : ProcessoTombstoneStatus.ACTIVE_VISIBLE;
            visiblePanel = true;
        } else if (reexposed) {
            status = ProcessoTombstoneStatus.ACTIVE_REEXPOSED_BY_SECRETARIAT;
            visiblePanel = true;
            hiddenByPolicy = false;
        } else if (!hiddenByPolicy) {
            status = ProcessoTombstoneStatus.ARCHIVED_VISIBLE_GRACE_PERIOD;
            visiblePanel = true;
        } else {
            status = resolveArchivedHiddenStatus(visibilityReport);
            visiblePanel = false;
        }
        ArrayList<String> alerts = new ArrayList<>();
        if (archived) {
            alerts.add("Processo arquivado sob política de tombstone lógico, sem supressão física do acervo.");
        }
        if (reexposed) {
            alerts.add("Reexposição controlada por secretaria ativa para o painel pessoal.");
        }
        if (controlledAccessRequired) {
            alerts.add("Consulta integral depende de trilha controlada e autenticação reforçada.");
        }
        if (visibilityReport != null && visibilityReport.alerts() != null) {
            alerts.addAll(visibilityReport.alerts());
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("archived", archived);
        metadata.put("reexposedBySecretariat", reexposed);
        metadata.put("hideAfterDays", hideAfterDays);
        metadata.put("archiveAgeDays", ageDays);
        metadata.put("controlledAccessRequired", controlledAccessRequired);
        metadata.put("sigilo", firstNonNull(processoLocal != null ? processoLocal.getNivelSigilo() : null, vinculo != null ? vinculo.getNivelSigilo() : null, NivelSigilo.PUBLICO).name());
        metadata.put("ramoDireito", firstNonNull(processoLocal != null ? processoLocal.getRamoDireito() : null, vinculo != null ? vinculo.getRamoDireito() : null));
        metadata.put("visibilityMode", visibilityReport != null && visibilityReport.mode() != null ? visibilityReport.mode().name() : null);
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new ProcessoTombstonePolicyReport(
                status,
                archived,
                visiblePanel,
                hiddenByPolicy,
                reexposed,
                controlledAccessRequired,
                hideAfterDays,
                ageDays,
                List.copyOf(alerts),
                Collections.unmodifiableMap(metadata)
        );
    }

    private ProcessoTombstoneStatus resolveArchivedHiddenStatus(ArchivedProcessVisibilityPolicyReport report) {
        if (report == null || report.mode() == null) {
            return ProcessoTombstoneStatus.ARCHIVED_HIDDEN_BY_POLICY;
        }
        return switch (report.mode()) {
            case CONCEALED_SENSITIVE_GATE -> ProcessoTombstoneStatus.ARCHIVED_HIDDEN_SENSITIVE_GATE;
            case CONCEALED_INSTITUTIONAL_GATE -> ProcessoTombstoneStatus.ARCHIVED_HIDDEN_INSTITUTIONAL_GATE;
            case CONCEALED_PARTY_GATE -> ProcessoTombstoneStatus.ARCHIVED_HIDDEN_PARTY_GATE;
            default -> ProcessoTombstoneStatus.ARCHIVED_HIDDEN_BY_POLICY;
        };
    }

    private int archiveHideAfterDays(Processo processoLocal, ProcessoVinculoNacional vinculo) {
        NivelSigilo sigilo = firstNonNull(processoLocal != null ? processoLocal.getNivelSigilo() : null, vinculo != null ? vinculo.getNivelSigilo() : null, NivelSigilo.PUBLICO);
        RamoDireito ramo = firstNonNull(processoLocal != null ? processoLocal.getRamoDireito() : null, vinculo != null ? vinculo.getRamoDireito() : null);
        if (sigilo.exigeCredencial() || ramo == RamoDireito.PENAL || ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE || ramo == RamoDireito.MILITAR) {
            return ARCHIVE_HIDE_DAYS_SENSITIVE;
        }
        return ARCHIVE_HIDE_DAYS_STANDARD;
    }

    private long archiveAgeDays(Processo processoLocal, ProcessoVinculoNacional vinculo) {
        LocalDateTime reference = processoLocal == null ? null : firstNonNull(
                processoLocal.getDataUltimaMovimentacao(),
                processoLocal.getDataAtualizacao(),
                processoLocal.getDataDistribuicao(),
                processoLocal.getDataCriacao()
        );
        if (reference != null) {
            return Math.max(0L, ChronoUnit.DAYS.between(reference, LocalDateTime.now()));
        }
        if (vinculo != null && vinculo.getOcorridoEm() != null) {
            return Math.max(0L, ChronoUnit.DAYS.between(LocalDateTime.ofInstant(vinculo.getOcorridoEm(), ZoneOffset.UTC), LocalDateTime.now()));
        }
        return 0L;
    }

    private boolean isArchived(Processo processoLocal, ProcessoVinculoNacional vinculo) {
        StatusProcesso status = firstNonNull(processoLocal != null ? processoLocal.getStatusProcesso() : null, vinculo != null ? vinculo.getStatusProcesso() : null);
        return status != null && (status == StatusProcesso.ARQUIVADO || status.isEncerrado());
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
