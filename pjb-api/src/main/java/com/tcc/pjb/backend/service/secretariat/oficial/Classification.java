package com.tcc.pjb.backend.service.secretariat.oficial;

import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import java.util.Optional;

enum Classification {
    POSITIVO("CUMPRIMENTO_POSITIVO", 3, "RECEBIMENTO_CARTORARIO", "Cumprimento positivo", "Juntada cartorária do resultado do cumprimento", WorkItemType.JUNTADA, false, MaterializationAct.JUNTADA_FINAL_PROCESSUAL, "5D", 2, "CUMPRIMENTO_COMPLEMENTAR_CONFIRMATORIO"),
    PARCIAL("DILIGENCIA_PARCIAL", 2, "SANEAMENTO_CARTORARIO", "Diligência parcial", "Saneamento cartorário e complementação do cumprimento", WorkItemType.CERTIDAO, true, MaterializationAct.NOVA_EXPEDICAO_AO_OFICIAL, "10D", 1, "CUMPRIMENTO_COMPLEMENTAR"),
    FRUSTRADO("CUMPRIMENTO_FRUSTRADO", 1, "REANALISE_EXECUTIVA", "Cumprimento frustrado", "Reanálise cartorária e nova providência de expedição", WorkItemType.EXPEDICAO, true, MaterializationAct.CONCLUSAO_AUTOMATICA_AO_GABINETE, "5D", 0, "REITERACAO_JUDICIAL_DE_CUMPRIMENTO");

    private final String outcomeName;
    private final int priority;
    private final String bucket;
    private final String bucketLabel;
    private final String nextProvidenceLabel;
    private final WorkItemType nextType;
    private final boolean blocking;
    private final MaterializationAct primaryAct;
    private final String defaultExpeditionPrazo;
    private final int defaultJudicialPriority;
    private final String defaultJudicialOrderType;

    Classification(String outcomeName,
                   int priority,
                   String bucket,
                   String bucketLabel,
                   String nextProvidenceLabel,
                   WorkItemType nextType,
                   boolean blocking,
                   MaterializationAct primaryAct,
                   String defaultExpeditionPrazo,
                   int defaultJudicialPriority,
                   String defaultJudicialOrderType) {
        this.outcomeName = outcomeName;
        this.priority = priority;
        this.bucket = bucket;
        this.bucketLabel = bucketLabel;
        this.nextProvidenceLabel = nextProvidenceLabel;
        this.nextType = nextType;
        this.blocking = blocking;
        this.primaryAct = primaryAct;
        this.defaultExpeditionPrazo = defaultExpeditionPrazo;
        this.defaultJudicialPriority = defaultJudicialPriority;
        this.defaultJudicialOrderType = defaultJudicialOrderType;
    }

    static Classification fromOutcome(com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo outcome) {
        if (outcome == null) {
            return POSITIVO;
        }
        return switch (outcome) {
            case CUMPRIMENTO_POSITIVO -> POSITIVO;
            case DILIGENCIA_PARCIAL -> PARCIAL;
            case CUMPRIMENTO_FRUSTRADO -> FRUSTRADO;
        };
    }

    static Optional<Classification> fromTemplate(String template) {
        if (template == null || template.isBlank()) {
            return Optional.empty();
        }
        for (Classification value : values()) {
            if (template.endsWith(':' + value.name())) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    static Optional<Classification> fromExternal(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase();
        for (Classification classification : values()) {
            if (classification.name().equals(normalized) || classification.outcomeName.equals(normalized)) {
                return Optional.of(classification);
            }
        }
        return Optional.empty();
    }

    int priority() { return priority; }
    String bucket() { return bucket; }
    String bucketLabel() { return bucketLabel; }
    String nextProvidenceLabel() { return nextProvidenceLabel; }
    WorkItemType nextType() { return nextType; }
    boolean blocking() { return blocking; }
    MaterializationAct primaryAct() { return primaryAct; }
    String defaultExpeditionPrazo() { return defaultExpeditionPrazo; }
    int defaultJudicialPriority() { return defaultJudicialPriority; }
    String defaultJudicialOrderType() { return defaultJudicialOrderType; }
}
