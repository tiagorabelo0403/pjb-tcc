package com.tcc.pjb.backend.core.kernel.recursal.mesh.rules;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AdmissibilityDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInterno;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRecursoEspecial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRecursoExtraordinario;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ApelacaoCivel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ApelacaoPenal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDivergencia;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PreparoDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PreventionDisposition;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoEspecial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoExtraordinario;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalPerfilReal;

public final class TribunalSuperiorTrabalhoRuleProfile extends AbstractRecursalRuleProfile {

    public TribunalSuperiorTrabalhoRuleProfile() {
        super(RecursalTribunal.TST);
    }

    @Override
    public String name() {
        return "TST_RULE_PROFILE";
    }

    @Override
    public RecursalRoutePlan route(RecursalCaseContext context, RecursalSpecies species) {
        requireContext(context, species);
        RecursalTribunalPerfilReal perfil = realProfile(context);
        return switch (species) {
            case EmbargosDeclaracao embargos -> sameCourtPlan(
                    perfil.perfilNome(),
                    context,
                    embargos.contraDecisaoMonocratica() ? RecursalAuthority.RELATOR : RecursalAuthority.TURMA,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                    PreventionDisposition.strictSameRelator());
            case AgravoInterno ignored -> sameCourtPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalAuthority.TURMA,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                    PreventionDisposition.strictSameRelator());
            case RecursoExtraordinario ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.STF,
                    InstanceLevel.EXTRAORDINARY,
                    RecursalAuthority.PLENARIO,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(true, RecursalAuthority.PRESIDENCIA, true, RecursalAuthority.RELATOR, true, true, true, true),
                    new PreventionDisposition(true, false, true, true));
            case AgravoRecursoExtraordinario ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.STF,
                    InstanceLevel.EXTRAORDINARY,
                    RecursalAuthority.PLENARIO,
                    PreparoDisposition.dispensado(),
                    new AdmissibilityDisposition(false, null, true, RecursalAuthority.RELATOR, false, true, false, true),
                    new PreventionDisposition(true, false, true, true));
            case ApelacaoCivel ignored -> throw new IllegalArgumentException("Espécie incompatível com o TST nesta malha");
            case ApelacaoPenal ignored -> throw new IllegalArgumentException("Espécie incompatível com o TST nesta malha");
            case RecursoEspecial ignored -> throw new IllegalArgumentException("Espécie incompatível com o TST nesta malha");
            case AgravoRecursoEspecial ignored -> throw new IllegalArgumentException("Espécie incompatível com o TST nesta malha");
            case EmbargosDivergencia ignored -> throw new IllegalArgumentException("Espécie incompatível com o TST nesta malha");
            default -> routeExtendedSpecies(context, species);
        };
    }
}
