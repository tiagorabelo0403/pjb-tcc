package com.tcc.pjb.backend.core.kernel.recursal;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum LegalAppealType {


    APELACAO,
    AGRAVO_INSTRUMENTO,
    AGRAVO_INTERNO,
    EMBARGOS_DECLARACAO,
    EMBARGOS_INFRINGENTES,


    RESE,
    APELACAO_PENAL,
    HABEAS_CORPUS,


    RESP,
    RE,
    AGRAVO_RESP_RE,


    RECURSO_INOMINADO,
    PEDIDO_UNIFORMIZACAO,
    AGRAVO_REGIMENTAL,
    RECURSO_ORDINARIO_CONSTITUCIONAL,
    RECURSO_ORDINARIO_TRABALHISTA,
    RECURSO_REVISTA,
    AGRAVO_RECURSO_REVISTA,
    AGRAVO_PETICAO,
    EMBARGOS_EXECUCAO,
    EMBARGOS_EXECUCAO_FISCAL,
    EMBARGOS_TERCEIRO,
    RECLAMACAO_CONSTITUCIONAL,
    CONFLITO_COMPETENCIA,
    CORREICAO_PARCIAL,


    OUTRO;

    private static final Map<String, LegalAppealType> ALIASES = Map.ofEntries(
            Map.entry("ED", EMBARGOS_DECLARACAO),
            Map.entry("EMBARGOS_DE_DECLARACAO", EMBARGOS_DECLARACAO),
            Map.entry("AI", AGRAVO_INSTRUMENTO),
            Map.entry("AGINT", AGRAVO_INTERNO),
            Map.entry("REsp", RESP),
            Map.entry("RESP", RESP),
            Map.entry("RE", RE),
            Map.entry("RI", RECURSO_INOMINADO),
            Map.entry("RINOMINADO", RECURSO_INOMINADO),
            Map.entry("PUIL", PEDIDO_UNIFORMIZACAO),
            Map.entry("PUILF", PEDIDO_UNIFORMIZACAO),
            Map.entry("PEDILEF", PEDIDO_UNIFORMIZACAO),
            Map.entry("PEDIDO_UNIFORMIZACAO", PEDIDO_UNIFORMIZACAO),
            Map.entry("PEDIDO_DE_UNIFORMIZACAO", PEDIDO_UNIFORMIZACAO),
            Map.entry("AGRAVO_REGIMENTAL", AGRAVO_REGIMENTAL),
            Map.entry("AGRREG", AGRAVO_REGIMENTAL),
            Map.entry("ROC", RECURSO_ORDINARIO_CONSTITUCIONAL),
            Map.entry("ROT", RECURSO_ORDINARIO_TRABALHISTA),
            Map.entry("RR", RECURSO_REVISTA),
            Map.entry("AIRR", AGRAVO_RECURSO_REVISTA),
            Map.entry("AGP", AGRAVO_PETICAO),
            Map.entry("EE", EMBARGOS_EXECUCAO),
            Map.entry("EEF", EMBARGOS_EXECUCAO_FISCAL),
            Map.entry("ET", EMBARGOS_TERCEIRO),
            Map.entry("RCL", RECLAMACAO_CONSTITUCIONAL),
            Map.entry("CC", CONFLITO_COMPETENCIA),
            Map.entry("CPARCIAL", CORREICAO_PARCIAL)
    );

    public static LegalAppealType fromString(String raw) {
        if (raw == null || raw.isBlank()) return OUTRO;
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) return OUTRO;
        LegalAppealType alias = ALIASES.get(token);
        if (alias != null) return alias;
        try {
            return LegalAppealType.valueOf(token);
        } catch (Exception ignored) {
            return OUTRO;
        }
    }



    public boolean isExceptional() {
        return this == RESP || this == RE || this == AGRAVO_RESP_RE || this == RECLAMACAO_CONSTITUCIONAL;
    }

    public boolean isExecutoryIncident() {
        return this == EMBARGOS_EXECUCAO || this == EMBARGOS_EXECUCAO_FISCAL || this == EMBARGOS_TERCEIRO || this == AGRAVO_PETICAO;
    }

    public boolean isInternalReview() {
        return this == AGRAVO_INTERNO || this == AGRAVO_REGIMENTAL || this == EMBARGOS_DECLARACAO;
    }

    public boolean isOutsideCurrentMeshCatalog() {
        return this == HABEAS_CORPUS || this == RESE || this == EMBARGOS_INFRINGENTES || this == OUTRO;
    }

    @JsonCreator
    public static LegalAppealType jsonCreator(String raw) {
        return fromString(raw);
    }
}
