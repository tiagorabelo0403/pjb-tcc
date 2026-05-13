package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RecursalSpeciesCatalog {

    private static final Map<String, String> FORMAL_NAMES = Map.ofEntries(
            Map.entry("EDCL", "Embargos de Declaração"),
            Map.entry("AGINT", "Agravo Interno"),
            Map.entry("APCIV", "Apelação Cível"),
            Map.entry("APCRIM", "Apelação Penal"),
            Map.entry("AGINST", "Agravo de Instrumento"),
            Map.entry("AGITRAB", "Agravo de Instrumento Trabalhista"),
            Map.entry("AGREG", "Agravo Regimental"),
            Map.entry("ROC", "Recurso Ordinário Constitucional"),
            Map.entry("ROT", "Recurso Ordinário Trabalhista"),
            Map.entry("RR", "Recurso de Revista"),
            Map.entry("AIRR", "Agravo em Recurso de Revista"),
            Map.entry("AGPET", "Agravo de Petição"),
            Map.entry("EEXEC", "Embargos à Execução"),
            Map.entry("EEFISC", "Embargos à Execução Fiscal"),
            Map.entry("ETERC", "Embargos de Terceiro"),
            Map.entry("RESP", "Recurso Especial"),
            Map.entry("RE", "Recurso Extraordinário"),
            Map.entry("ARESP", "Agravo em Recurso Especial"),
            Map.entry("ARE", "Agravo em Recurso Extraordinário"),
            Map.entry("EDIV", "Embargos de Divergência"),
            Map.entry("RCL", "Reclamação"),
            Map.entry("CC", "Conflito de Competência"),
            Map.entry("CPARCIAL", "Correição Parcial"),
            Map.entry("RINOM", "Recurso Inominado"),
            Map.entry("PUILF", "Pedido de Uniformização de Interpretação de Lei Federal")
    );

    public Set<String> supportedCodes() {
        return FORMAL_NAMES.keySet();
    }

    public boolean supports(String code) {
        return FORMAL_NAMES.containsKey(code);
    }

    public String formalNameOf(String code) {
        return FORMAL_NAMES.get(code);
    }

    public Map<String, String> formalNamesOf(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (String code : codes) {
            if (code != null && FORMAL_NAMES.containsKey(code)) {
                out.put(code, FORMAL_NAMES.get(code));
            }
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }
}

