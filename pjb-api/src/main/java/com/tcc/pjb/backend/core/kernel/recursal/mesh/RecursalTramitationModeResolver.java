package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosTerceiro;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucaoFiscal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;
public final class RecursalTramitationModeResolver {

    private RecursalTramitationModeResolver() {
    }

    public static RecursalTramitationMode resolve(RecursalRoutePlan plan, RecursalSpecies species) {
        if (plan == null) {
            return RecursalTramitationMode.SAME_AUTOS_SAME_GRADE;
        }
        if (species instanceof EmbargosExecucao || species instanceof EmbargosExecucaoFiscal || species instanceof EmbargosTerceiro) {
            return RecursalTramitationMode.APARTADO_DEPENDENCIA_SAME_GRADE;
        }
        if (plan.remessa().autosApartadosDependencia()) {
            return RecursalTramitationMode.APARTADO_DEPENDENCIA_SAME_GRADE;
        }
        if (plan.remessa().mesmosAutos()) {
            return RecursalTramitationMode.SAME_AUTOS_SAME_GRADE;
        }
        if (plan.remessa().externa() && plan.remessa().autuacaoDestino()) {
            return RecursalTramitationMode.HIGHER_GRADE_AUTONOMOUS;
        }
        if (plan.remessa().externa()) {
            return RecursalTramitationMode.HIGHER_GRADE_SAME_NUMBERING;
        }
        return RecursalTramitationMode.SAME_AUTOS_SAME_GRADE;
    }
}
