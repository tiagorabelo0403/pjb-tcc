package com.tcc.pjb.backend.core.kernel.recursal;

import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.core.util.EnumText;

public final class RecursalAliasRegistry {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("ED", "EDCL"),
            Map.entry("EMBARGOS_DECLARACAO", "EDCL"),
            Map.entry("AGINT", "AGINT"),
            Map.entry("AGRAVO_INTERNO", "AGINT"),
            Map.entry("APELACAO_CIVEL", "APCIV"),
            Map.entry("APELACAO_PENAL", "APCRIM"),
            Map.entry("AI", "AGINST"),
            Map.entry("AGRAVO_INSTRUMENTO", "AGINST"),
            Map.entry("AGRAVO_DE_INSTRUMENTO", "AGINST"),
            Map.entry("AGRAVO_INSTRUMENTO_TRABALHISTA", "AGITRAB"),
            Map.entry("AGRAVO_REGIMENTAL", "AGREG"),
            Map.entry("AGRAVO_REGIMENTAL_INTERNO", "AGREG"),
            Map.entry("ROC", "ROC"),
            Map.entry("RECURSO_ORDINARIO_CONSTITUCIONAL", "ROC"),
            Map.entry("ROT", "ROT"),
            Map.entry("RECURSO_ORDINARIO_TRABALHISTA", "ROT"),
            Map.entry("RR", "RR"),
            Map.entry("RECURSO_REVISTA", "RR"),
            Map.entry("AIRR", "AIRR"),
            Map.entry("AGRAVO_RECURSO_REVISTA", "AIRR"),
            Map.entry("AGP", "AGPET"),
            Map.entry("AGRAVO_PETICAO", "AGPET"),
            Map.entry("EE", "EEXEC"),
            Map.entry("EMBARGOS_EXECUCAO", "EEXEC"),
            Map.entry("EEF", "EEFISC"),
            Map.entry("EMBARGOS_EXECUCAO_FISCAL", "EEFISC"),
            Map.entry("ET", "ETERC"),
            Map.entry("EMBARGOS_TERCEIRO", "ETERC"),
            Map.entry("RESP", "RESP"),
            Map.entry("RE", "RE"),
            Map.entry("ARESP", "ARESP"),
            Map.entry("ARE", "ARE"),
            Map.entry("EDIV", "EDIV"),
            Map.entry("RCL", "RCL"),
            Map.entry("RECLAMACAO", "RCL"),
            Map.entry("RECLAMACAO_CONSTITUCIONAL", "RCL"),
            Map.entry("CC", "CC"),
            Map.entry("CONFLITO_COMPETENCIA", "CC"),
            Map.entry("CPARCIAL", "CPARCIAL"),
            Map.entry("CORREICAO_PARCIAL", "CPARCIAL"),
            Map.entry("RI", "RINOM"),
            Map.entry("RECURSO_INOMINADO", "RINOM"),
            Map.entry("RINOMINADO", "RINOM"),
            Map.entry("PUIL", "PUILF"),
            Map.entry("PUILF", "PUILF"),
            Map.entry("PEDILEF", "PUILF"),
            Map.entry("PEDIDO_UNIFORMIZACAO", "PUILF"),
            Map.entry("PEDIDO_DE_UNIFORMIZACAO", "PUILF")
    );

    public String resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) {
            return null;
        }
        return ALIASES.getOrDefault(token, token);
    }

    public boolean matches(String raw, String canonicalCode) {
        return Objects.equals(resolve(raw), canonicalCode);
    }
}
