package com.tcc.pjb.backend.core.processo.migracao.intelligence;

import java.util.LinkedHashSet;
import java.util.List;

public final class PjbLegacyMigrationIntelligenceService {

    public PjbLegacyMigrationIntelligenceReport assess(List<PjbLegacyMigrationDivergence> divergences) {
        List<PjbLegacyMigrationDivergence> normalized = divergences == null ? List.of() : List.copyOf(divergences);
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        boolean blocking = false;
        for (PjbLegacyMigrationDivergence divergence : normalized) {
            blocking = blocking || divergence.blocking();
            actions.add(action(divergence));
        }
        if (actions.isEmpty()) {
            actions.add("executar migração com trilha de auditoria e reconciliação pós-carga");
        }
        String status = blocking ? "BLOCKED_BY_DIVERGENCE" : normalized.isEmpty() ? "READY" : "READY_WITH_RECONCILIATION";
        return new PjbLegacyMigrationIntelligenceReport(status, !blocking, normalized, List.copyOf(actions));
    }

    private String action(PjbLegacyMigrationDivergence divergence) {
        if (divergence == null) {
            return "revisar divergência sem classificação";
        }
        return switch (divergence.type()) {
            case PARTY_DUPLICATION -> "reconciliar identidade de partes antes de ativar capa processual";
            case DOCUMENT_ORPHAN -> "vincular documento órfão a protocolo, movimento ou evento histórico";
            case MOVEMENT_UNMAPPED -> "mapear movimento legado para tabela processual nacional";
            case SECRECY_MISMATCH -> "reaplicar sigilo e validar versão pública antes de consulta externa";
            case PROTOCOL_GAP -> "preservar protocolo original ou emitir ressalva de migração";
            case CLASS_SUBJECT_INCONSISTENCY -> "normalizar classe e assunto com evidência da origem";
            case HISTORICAL_EVENT_LOSS -> "bloquear execução até recompor histórico mínimo verificável";
        };
    }
}
