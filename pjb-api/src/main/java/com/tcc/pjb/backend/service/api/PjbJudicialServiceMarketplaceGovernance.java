package com.tcc.pjb.backend.service.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;

public final class PjbJudicialServiceMarketplaceGovernance {

    public PjbJudicialServiceMarketplaceDecision evaluate(PjbJudicialServiceOffering offering, String tribunalCode) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> requirements = new LinkedHashSet<>();
        if (offering == null || Objects.toString(offering.providerCode(), "").isBlank()) {
            blockers.add("PROVEDOR_NAO_IDENTIFICADO");
        }
        if (offering == null || offering.category() == null) {
            blockers.add("CATEGORIA_NAO_CLASSIFICADA");
        }
        if (offering == null || !offering.homologated()) {
            blockers.add("SERVICO_NAO_HOMOLOGADO");
        }
        if (offering == null || !offering.audited()) {
            blockers.add("AUDITORIA_NAO_CONCLUIDA");
        }
        if (offering == null || !offering.lgpdReady()) {
            blockers.add("LGPD_NAO_VALIDADA");
        }
        if (offering != null && offering.supportedTribunals() != null && !offering.supportedTribunals().isEmpty() && !offering.supportedTribunals().contains(tribunalCode)) {
            blockers.add("TRIBUNAL_FORA_DA_COBERTURA");
        }
        if (offering != null && offering.humanSupervisionRequired()) {
            requirements.add("exigir revisão humana antes de materializar resultado no processo");
        }
        requirements.add("registrar contratação, execução e evidências em trilha auditável");
        String status = blockers.isEmpty() ? "SERVICE_AVAILABLE" : "SERVICE_BLOCKED";
        return new PjbJudicialServiceMarketplaceDecision(status, blockers.isEmpty(), new ArrayList<>(blockers), new ArrayList<>(requirements));
    }
}
