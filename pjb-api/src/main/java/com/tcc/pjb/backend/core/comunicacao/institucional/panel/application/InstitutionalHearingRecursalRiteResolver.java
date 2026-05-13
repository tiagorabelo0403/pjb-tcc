package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingRiteGovernance;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class InstitutionalHearingRecursalRiteResolver {

    private final InstitutionalHearingRiteGovernanceFactory factory;

    InstitutionalHearingRecursalRiteResolver(InstitutionalHearingRiteGovernanceFactory factory) {
        this.factory = factory;
    }

    List<InstitutionalHearingRiteGovernance> resolve(InstitutionalHearingRiteGovernanceContext context) {
        ArrayList<InstitutionalHearingRiteGovernance> rites = new ArrayList<>();

        factory.addIfRelevant(rites, factory.buildRite(
                context.profile(),
                context.workspace(),
                context.schedulingScopeKey(),
                "RECURSAL",
                "TRIBUNAIS",
                context.laborScope() ? "TRABALHO" : context.federalScope() ? "FEDERAL" : context.electoralScope() ? "ELEITORAL" : context.militaryScope() ? "MILITAR" : "ESTADUAL",
                "RECURSAL",
                "SUSTENTACAO_ORAL_OU_INSTRUCAO_EXCEPCIONAL",
                context.recursalBroad(),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                context.canConfirmAttendance(),
                false,
                context.canIssueHearingCommunications() && context.recursalBroad(),
                context.canPrepareHearingBundle() && context.recursalBroad(),
                context.requiresUnitIsolation(),
                false,
                true,
                context.requestActors(),
                context.preparatoryActors(),
                context.communicationActors(),
                Set.of(),
                context.trackingActors(),
                context.oversightActors(),
                List.of("registrar_sustentacao_oral", "acompanhar_publicacao_de_pauta_de_julgamento"),
                List.of(),
                factory.mergeSegregationGuards(context.topLevelSegregationGuards(), "RECURSAL", "COLEGIADO")));

        return List.copyOf(rites);
    }
}
