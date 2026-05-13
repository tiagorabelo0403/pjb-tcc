package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosTerceiro;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucaoFiscal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDivergencia;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;

public final class RecursalRouteKindResolver {

    private RecursalRouteKindResolver() {
    }

    public static RecursalRouteKind resolve(RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan plan) {
        if (species instanceof EmbargosExecucao || species instanceof EmbargosExecucaoFiscal || species instanceof EmbargosTerceiro) {
            return RecursalRouteKind.EXECUTION_INCIDENT_INTERNAL;
        }
        if (plan.mesmaCorte() && plan.remessa().mesmosAutos()) {
            if (species instanceof AgravoInterno || species instanceof AgravoRegimental || species instanceof CorrecaoParcial) {
                return RecursalRouteKind.INTERNAL_REGIMENTAL;
            }
            return RecursalRouteKind.INTERNAL_SAME_AUTOS;
        }
        if (species instanceof RecursoInominadoJuizado) {
            return RecursalRouteKind.JUIZADO_TURMA_RECURSAL;
        }
        if (species instanceof PedidoUniformizacaoFederal) {
            return RecursalRouteKind.JUIZADO_UNIFORMIZACAO;
        }
        if (species instanceof ReclamacaoConstitucional || species instanceof ConflitoCompetencia) {
            return plan.instanciaDestino() == InstanceLevel.EXTRAORDINARY || plan.tribunalDestino().constitutionalCourt()
                    ? RecursalRouteKind.ORIGINARY_CONSTITUTIONAL
                    : RecursalRouteKind.ORIGINARY_SUPERIOR;
        }
        if (species instanceof RecursoExtraordinario || species instanceof AgravoRecursoExtraordinario) {
            return RecursalRouteKind.EXTRAORDINARY_EXCEPTIONAL;
        }
        if (species instanceof RecursoEspecial
                || species instanceof AgravoRecursoEspecial
                || species instanceof RecursoRevista
                || species instanceof AgravoRecursoRevista
                || species instanceof EmbargosDivergencia
                || species instanceof RecursoOrdinarioConstitucional && (plan.tribunalDestino().superiorCourt() || plan.instanciaDestino() == InstanceLevel.SUPERIOR)) {
            return RecursalRouteKind.SUPERIOR_EXCEPTIONAL;
        }
        if (species instanceof RecursoOrdinarioConstitucional && (plan.tribunalDestino().constitutionalCourt() || plan.instanciaDestino() == InstanceLevel.EXTRAORDINARY)) {
            return RecursalRouteKind.EXTRAORDINARY_EXCEPTIONAL;
        }
        if (context.classFamily() == RecursalClassFamily.JUIZADO_ESPECIAL || context.rito().isJuizado()) {
            return RecursalRouteKind.JUIZADO_TURMA_RECURSAL;
        }
        return RecursalRouteKind.SECOND_INSTANCE_EXTERNAL;
    }
}
