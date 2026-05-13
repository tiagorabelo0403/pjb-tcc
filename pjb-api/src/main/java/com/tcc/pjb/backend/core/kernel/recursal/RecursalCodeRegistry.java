package com.tcc.pjb.backend.core.kernel.recursal;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RecursalCodeRegistry {

    private static final Map<String, LegalAppealType> TYPES = Map.ofEntries(
            Map.entry("EDCL", LegalAppealType.EMBARGOS_DECLARACAO),
            Map.entry("AGINT", LegalAppealType.AGRAVO_INTERNO),
            Map.entry("APCIV", LegalAppealType.APELACAO),
            Map.entry("APCRIM", LegalAppealType.APELACAO_PENAL),
            Map.entry("AGINST", LegalAppealType.AGRAVO_INSTRUMENTO),
            Map.entry("AGITRAB", LegalAppealType.AGRAVO_INSTRUMENTO),
            Map.entry("AGREG", LegalAppealType.AGRAVO_REGIMENTAL),
            Map.entry("ROC", LegalAppealType.RECURSO_ORDINARIO_CONSTITUCIONAL),
            Map.entry("ROT", LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA),
            Map.entry("RR", LegalAppealType.RECURSO_REVISTA),
            Map.entry("AIRR", LegalAppealType.AGRAVO_RECURSO_REVISTA),
            Map.entry("AGPET", LegalAppealType.AGRAVO_PETICAO),
            Map.entry("EEXEC", LegalAppealType.EMBARGOS_EXECUCAO),
            Map.entry("EEFISC", LegalAppealType.EMBARGOS_EXECUCAO_FISCAL),
            Map.entry("ETERC", LegalAppealType.EMBARGOS_TERCEIRO),
            Map.entry("RESP", LegalAppealType.RESP),
            Map.entry("RE", LegalAppealType.RE),
            Map.entry("ARESP", LegalAppealType.AGRAVO_RESP_RE),
            Map.entry("ARE", LegalAppealType.AGRAVO_RESP_RE),
            Map.entry("EDIV", LegalAppealType.OUTRO),
            Map.entry("RCL", LegalAppealType.RECLAMACAO_CONSTITUCIONAL),
            Map.entry("CC", LegalAppealType.CONFLITO_COMPETENCIA),
            Map.entry("CPARCIAL", LegalAppealType.CORREICAO_PARCIAL),
            Map.entry("RINOM", LegalAppealType.RECURSO_INOMINADO),
            Map.entry("PUILF", LegalAppealType.PEDIDO_UNIFORMIZACAO)
    );

    public LegalAppealType typeOf(String canonicalCode) {
        return TYPES.getOrDefault(canonicalCode, LegalAppealType.OUTRO);
    }

    public String canonicalCodeFor(LegalAppealType type) {
        if (type == null) {
            return null;
        }
        return TYPES.entrySet().stream()
                .filter(entry -> entry.getValue() == type)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public boolean supports(LegalAppealType type) {
        return canonicalCodeFor(type) != null;
    }

    public Set<LegalAppealType> supportedTypes() {
        return Arrays.stream(LegalAppealType.values())
                .filter(this::supports)
                .collect(Collectors.toUnmodifiableSet());
    }
}

