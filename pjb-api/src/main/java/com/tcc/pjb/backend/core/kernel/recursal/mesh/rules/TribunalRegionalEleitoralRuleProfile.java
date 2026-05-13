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

public final class TribunalRegionalEleitoralRuleProfile extends AbstractRecursalRuleProfile {

    public TribunalRegionalEleitoralRuleProfile() {
        super(RecursalTribunal.TRE);
    }

    @Override
    public String name() {
        return "TRE_RULE_PROFILE";
    }

    @Override
    public RecursalRoutePlan route(RecursalCaseContext context, RecursalSpecies species) {
        requireContext(context, species);
        return switch (species) {
            case EmbargosDeclaracao embargos -> routeEmbargos(context, embargos);
            case AgravoInterno agravoInterno -> routeAgravoInterno(context, agravoInterno);
            case RecursoEspecial recursoEspecial -> routeRecursoEspecial(context, recursoEspecial);
            case AgravoRecursoEspecial agravoRecursoEspecial -> routeAgravoRecursoEspecial(context, agravoRecursoEspecial);
            case RecursoExtraordinario recursoExtraordinario -> routeRecursoExtraordinario(context, recursoExtraordinario);
            case AgravoRecursoExtraordinario agravoRecursoExtraordinario -> routeAgravoRecursoExtraordinario(context, agravoRecursoExtraordinario);
            case ApelacaoCivel ignored -> throw new IllegalArgumentException("Espécie incompatível com o fluxo central do TRE nesta malha");
            case ApelacaoPenal ignored2 -> throw new IllegalArgumentException("Espécie incompatível com o fluxo central do TRE nesta malha");
            case EmbargosDivergencia ignored5 -> throw new IllegalArgumentException("Espécie incompatível com o fluxo central do TRE nesta malha");
            default -> routeExtendedSpecies(context, species);
        };
    }

    private RecursalRoutePlan routeEmbargos(RecursalCaseContext context, EmbargosDeclaracao embargos) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        return sameCourtPlan(
                perfil.perfilNome(),
                context,
                embargos.contraDecisaoMonocratica() ? RecursalAuthority.RELATOR : context.autoridadeAtual(),
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                PreventionDisposition.strictSameRelator()
        );
    }

    private RecursalRoutePlan routeAgravoInterno(RecursalCaseContext context, AgravoInterno agravoInterno) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        return sameCourtPlan(
                perfil.perfilNome(),
                context,
                agravoInterno.contraFiltroPresidencial() ? perfil.autoridadeAgravoInternoFiltro() : RecursalAuthority.PLENARIO,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                PreventionDisposition.strictSameRelator()
        );
    }


    private RecursalRoutePlan routeRecursoEspecial(RecursalCaseContext context, RecursoEspecial ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE, "Recurso especial eleitoral exige decisão de TRE");
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.TSE,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.PLENARIO,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(true, perfil.autoridadeAdmissibilidadeExcepcional(), true, RecursalAuthority.RELATOR, true, true, true, false),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeAgravoRecursoEspecial(RecursalCaseContext context, AgravoRecursoEspecial ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.TSE,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.PLENARIO,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, true, RecursalAuthority.RELATOR, false, true, false, false),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeRecursoExtraordinario(RecursalCaseContext context, RecursoExtraordinario ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        require(context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE, "Recurso extraordinário eleitoral exige decisão de TRE");
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.STF,
                InstanceLevel.EXTRAORDINARY,
                RecursalAuthority.PLENARIO,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(true, perfil.autoridadeAdmissibilidadeExcepcional(), true, RecursalAuthority.RELATOR, true, true, true, true),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeAgravoRecursoExtraordinario(RecursalCaseContext context, AgravoRecursoExtraordinario ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.STF,
                InstanceLevel.EXTRAORDINARY,
                RecursalAuthority.PLENARIO,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, true, RecursalAuthority.RELATOR, false, true, false, true),
                new PreventionDisposition(true, false, true, true)
        );
    }
}
