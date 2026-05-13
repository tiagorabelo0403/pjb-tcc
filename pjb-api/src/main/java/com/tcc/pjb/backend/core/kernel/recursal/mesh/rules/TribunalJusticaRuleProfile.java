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

public final class TribunalJusticaRuleProfile extends AbstractRecursalRuleProfile {

    public TribunalJusticaRuleProfile() {
        super(RecursalTribunal.TJ);
    }

    @Override
    public String name() {
        return "TJ_RULE_PROFILE";
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
            case EmbargosDivergencia ignored -> throw new IllegalArgumentException("Embargos de divergência não integram o fluxo ordinário do TJ");
            default -> routeExtendedSpecies(context, species);
        };
    }

    private RecursalRoutePlan routeEmbargos(RecursalCaseContext context, EmbargosDeclaracao embargos) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        RecursalAuthority merit = embargos.contraDecisaoMonocratica() ? RecursalAuthority.RELATOR : context.autoridadeAtual();
        return sameCourtPlan(
                perfil.perfilNome(),
                context,
                merit,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                PreventionDisposition.strictSameRelator()
        );
    }

    private RecursalRoutePlan routeAgravoInterno(RecursalCaseContext context, AgravoInterno agravoInterno) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        requireMonocraticDecision(context);
        require(context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE, "Agravo interno exige decisão monocrática no tribunal");
        RecursalAuthority merit = agravoInterno.contraFiltroPresidencial() ? perfil.autoridadeAgravoInternoFiltro() : RecursalAuthority.CAMARA;
        return sameCourtPlan(
                perfil.perfilNome(),
                context,
                merit,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                PreventionDisposition.strictSameRelator()
        );
    }

    private RecursalRoutePlan routeApelacaoCivel(RecursalCaseContext context, ApelacaoCivel ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        requireFirstInstance(context);
        require(context.classFamily() != RecursalClassFamily.JUIZADO_ESPECIAL, "Juizado especial não admite apelação cível clássica");
        require(!context.rito().isPenal(), "Apelação cível não incide em rito penal");
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.TJ,
                InstanceLevel.SECOND_INSTANCE,
                RecursalAuthority.CAMARA,
                ordinaryCivilPreparo(context),
                new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeApelacaoPenal(RecursalCaseContext context, ApelacaoPenal apelacaoPenal) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        requireFirstInstance(context);
        require(context.rito().isPenal() || context.classFamily() == RecursalClassFamily.CRIMINAL_ACAO || apelacaoPenal.tribunalDoJuri(), "Apelação penal exige base criminal");
        return externalPlan(
                perfil.perfilNome(),
                context,
                RecursalTribunal.TJ,
                InstanceLevel.SECOND_INSTANCE,
                RecursalAuthority.CAMARA,
                PreparoDisposition.dispensado(),
                new AdmissibilityDisposition(true, RecursalAuthority.JUIZO_SINGULAR, false, null, true, false, false, false),
                new PreventionDisposition(true, false, true, true)
        );
    }

    private RecursalRoutePlan routeRecursoEspecial(RecursalCaseContext context, RecursoEspecial ignored) {
        RecursalTribunalPerfilReal perfil = realProfile(context);
        requireSecondInstance(context);
        requireColegiateDecision(context);
        require(context.materiaFederalInfraconstitucional(), "Recurso especial exige controvérsia federal infraconstitucional");
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
        require(context.materiaConstitucional(), "Recurso extraordinário exige questão constitucional direta");
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
        require(context.autoridadeAtual().presidencia(), "Agravo em recurso especial exige decisão da presidência ou vice-presidência do tribunal de origem");
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
        require(context.autoridadeAtual().presidencia(), "Agravo em recurso extraordinário exige decisão da presidência ou vice-presidência do tribunal de origem");
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
