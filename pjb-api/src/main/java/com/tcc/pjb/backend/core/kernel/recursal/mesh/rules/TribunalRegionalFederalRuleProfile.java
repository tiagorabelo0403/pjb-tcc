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
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalPerfilReal;

public final class TribunalRegionalFederalRuleProfile extends AbstractRecursalRuleProfile {

    public TribunalRegionalFederalRuleProfile() {
        super(RecursalTribunal.TRF);
    }

    @Override
    public String name() {
        return "TRF_RULE_PROFILE";
    }

    @Override
    public RecursalRoutePlan route(RecursalCaseContext context, RecursalSpecies species) {
        requireContext(context, species);
        return switch (species) {
            case EmbargosDeclaracao embargos -> routeEmbargos(context, embargos);
            case AgravoInterno agravoInterno -> routeAgravoInterno(context, agravoInterno);
            case ApelacaoCivel apelacaoCivel -> routeApelacaoCivel(context, apelacaoCivel);
            case ApelacaoPenal apelacaoPenal -> routeApelacaoPenal(context, apelacaoPenal);
            case RecursoEspecial recursoEspecial -> routeRecursoEspecial(context, recursoEspecial);
            case RecursoExtraordinario recursoExtraordinario -> routeRecursoExtraordinario(context, recursoExtraordinario);
            case AgravoRecursoEspecial agravoRecursoEspecial -> routeAgravoRecursoEspecial(context, agravoRecursoEspecial);
            case AgravoRecursoExtraordinario agravoRecursoExtraordinario -> routeAgravoRecursoExtraordinario(context, agravoRecursoExtraordinario);
            case EmbargosDivergencia ignored -> throw new IllegalArgumentException("Embargos de divergência não integram o fluxo ordinário do TRF");
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
        requireMonocraticDecision(context);
        require(context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE, "Agravo interno exige decisão monocrática no tribunal");
        return sameCourtPlan(
                perfil.perfilNome(),
                context,
                agravoInterno.contraFiltroPresidencial() ? perfil.autoridadeAgravoInternoFiltro() : RecursalAuthority.TURMA,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                PreventionDisposition.strictSameRelator()
        );
    }

    private RecursalRoutePlan routeApelacaoCivel(RecursalCaseContext context, ApelacaoCivel ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        requireFirstInstance(context);
        require(context.tipoJustica() == com.tcc.pjb.backend.domain.enums.TipoJustica.FEDERAL || context.classFamily() == RecursalClassFamily.FAZENDA_PUBLICA, "Apelação no TRF exige causa federal");
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.TRF,
                InstanceLevel.SECOND_INSTANCE,
                RecursalAuthority.TURMA,
                ordinaryCivilPreparo(context),
                new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeApelacaoPenal(RecursalCaseContext context, ApelacaoPenal ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        requireFirstInstance(context);
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.TRF,
                InstanceLevel.SECOND_INSTANCE,
                RecursalAuthority.TURMA,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeRecursoEspecial(RecursalCaseContext context, RecursoEspecial ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        requireSecondInstance(context);
        requireColegiateDecision(context);
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.STJ,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.TURMA,
                ordinaryCivilPreparo(context),
                new AdmissibilityDisposition(true, perfil.autoridadeAdmissibilidadeExcepcional(), true, RecursalAuthority.RELATOR, true, true, true, false),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeRecursoExtraordinario(RecursalCaseContext context, RecursoExtraordinario ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        requireSecondInstance(context);
        requireColegiateDecision(context);
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.STF,
                InstanceLevel.EXTRAORDINARY,
                RecursalAuthority.TURMA,
                ordinaryCivilPreparo(context),
                new AdmissibilityDisposition(true, perfil.autoridadeAdmissibilidadeExcepcional(), true, RecursalAuthority.RELATOR, true, true, true, true),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeAgravoRecursoEspecial(RecursalCaseContext context, AgravoRecursoEspecial ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.STJ,
                InstanceLevel.SUPERIOR,
                RecursalAuthority.TURMA,
                ordinaryCivilPreparo(context),
                new AdmissibilityDisposition(false, null, true, RecursalAuthority.RELATOR, false, true, false, false),
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
                RecursalAuthority.TURMA,
                ordinaryCivilPreparo(context),
                new AdmissibilityDisposition(false, null, true, RecursalAuthority.RELATOR, false, true, false, true),
                new PreventionDisposition(true, false, true, true)
        );
    }
}
