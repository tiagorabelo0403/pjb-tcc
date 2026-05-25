package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Objects;

public final class RecursalRouteIntegrityValidator {

    public void validate(RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan routePlan) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(species, "species");
        Objects.requireNonNull(routePlan, "routePlan");
        require(routePlan.preparo() != null, "Plano recursal sem regra de preparo");
        require(routePlan.admissibilidade() != null, "Plano recursal sem regra de admissibilidade");
        require(routePlan.prevencao() != null, "Plano recursal sem regra de prevenção");
        require(routePlan.remessa() != null, "Plano recursal sem regra de remessa");
        require(routePlan.tribunalDetalhadoOrigem().familia() == routePlan.tribunalOrigem(), "Tribunal detalhado de origem incompatível com a família recursal");
        require(routePlan.tribunalDetalhadoDestino().familia() == routePlan.tribunalDestino(), "Tribunal detalhado de destino incompatível com a família recursal");
        boolean mesmaCorteInterna = routePlan.mesmaCorte() && routePlan.instanciaDestino() == context.instanciaAtual();
        if (mesmaCorteInterna) {
            require(!routePlan.remessa().externa(), "Mesma corte não admite remessa externa");
            require(routePlan.instanciaDestino() == context.instanciaAtual(), "Mesma corte deve preservar a instância do julgamento");
            require(routePlan.tribunalDetalhadoOrigem() == routePlan.tribunalDetalhadoDestino(), "Mesma corte deve preservar o tribunal detalhado");
        } else {
            require(routePlan.remessa().externa(), "Mudança de corte exige remessa externa");
            require(routePlan.tribunalDestino().instanceLevel().ordinal() >= context.tribunalOrigem().instanceLevel().ordinal(), "Destino recursal não pode degradar a hierarquia do tribunal");
        }
        if (routePlan.preparo().exigido()) {
            require(species.potentiallyRequiresPreparo(), "Espécie incompatível com preparo obrigatório");
        }
        if (species.requiresCollegiateMerit()) {
            require(routePlan.autoridadeJulgamentoMerito().colegiado(), "Espécie exige julgamento colegiado");
        }
        if (routePlan.admissibilidade().juizoOrigem()) {
            require(routePlan.admissibilidade().autoridadeOrigem() != null, "Juízo de admissibilidade de origem exige autoridade definida");
        }
        if (routePlan.admissibilidade().juizoDestino()) {
            require(routePlan.admissibilidade().autoridadeDestino() != null, "Juízo de admissibilidade de destino exige autoridade definida");
        }
    }

    private void require(boolean expression, String message) {
        if (!expression) {
            throw new RecursalConstraintViolationException(message);
        }
    }
}
