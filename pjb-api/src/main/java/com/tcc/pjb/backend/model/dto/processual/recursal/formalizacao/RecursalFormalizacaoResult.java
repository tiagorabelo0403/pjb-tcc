package com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao;

import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import java.util.LinkedHashMap;
import java.util.Map;

public record RecursalFormalizacaoResult(
        boolean disponivel,
        String status,
        Map<String, Object> pecaFormalPrincipal,
        RecursalPdfArtifact pecaFormalPrincipalPdf,
        Map<String, Object> contrarrazoesAtoAutonomo,
        Map<String, Object> embargosAtoAutonomo,
        Map<String, Object> assinaturaVinculada,
        Map<String, Object> protocoloConectorJudicial,
        Map<String, Object> representacaoProcessual,
        Map<String, Object> sigiloRecursal) {

    public RecursalFormalizacaoResult {
        pecaFormalPrincipal = copy(pecaFormalPrincipal);
        contrarrazoesAtoAutonomo = copy(contrarrazoesAtoAutonomo);
        embargosAtoAutonomo = copy(embargosAtoAutonomo);
        pecaFormalPrincipalPdf = pecaFormalPrincipalPdf == null ? RecursalPdfArtifact.unavailable() : pecaFormalPrincipalPdf;
        assinaturaVinculada = copy(assinaturaVinculada);
        protocoloConectorJudicial = copy(protocoloConectorJudicial);
        representacaoProcessual = copy(representacaoProcessual);
        sigiloRecursal = copy(sigiloRecursal);
    }

    public static RecursalFormalizacaoResult unavailable() {
        return new RecursalFormalizacaoResult(false, null, Map.of(), RecursalPdfArtifact.unavailable(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    public boolean empty() {
        return !disponivel;
    }

    public Map<String, Object> toMap() {
        if (!disponivel) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        put(out, "status", status);
        put(out, "pecaFormalPrincipal", pecaFormalPrincipal);
        put(out, "pecaFormalPrincipalPdf", pecaFormalPrincipalPdf.toMap());
        put(out, "contrarrazoesAtoAutonomo", contrarrazoesAtoAutonomo);
        put(out, "embargosAtoAutonomo", embargosAtoAutonomo);
        put(out, "assinaturaVinculada", assinaturaVinculada);
        put(out, "protocoloConectorJudicial", protocoloConectorJudicial);
        put(out, "representacaoProcessual", representacaoProcessual);
        put(out, "sigiloRecursal", sigiloRecursal);
        return Map.copyOf(out);
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        return value == null || value.isEmpty() ? Map.of() : Map.copyOf(value);
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        target.put(key, value);
    }
}
