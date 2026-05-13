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

public final class SuperiorTribunalJusticaRuleProfile extends AbstractRecursalRuleProfile {

    public SuperiorTribunalJusticaRuleProfile() {
        super(RecursalTribunal.STJ);
    }

    @Override
    public String name() {
        return "STJ_RULE_PROFILE";
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
            case AgravoInterno ignored -> {
                requireMonocraticDecision(context);
                require(context.instanciaAtual() == InstanceLevel.SUPERIOR || context.instanciaAtual() == InstanceLevel.EXTRAORDINARY, "Agravo interno exige decisão monocrática no STJ");
                yield sameCourtPlan(
                        perfil.perfilNome(),
                        context,
                        RecursalAuthority.TURMA,
                        PreparoDisposition.dispensado(),
                        new AdmissibilityDisposition(false, null, false, null, false, false, false, false),
                        PreventionDisposition.strictSameRelator());
            }
            case EmbargosDivergencia embargosDivergencia -> {
                requireColegiateDecision(context);
                require(embargosDivergencia.meritoDoParadigmaConhecido(), "Embargos de divergência exigem mérito conhecido no acórdão paradigma");
                yield sameCourtPlan(
                        perfil.perfilNome(),
                        context,
                        perfil.autoridadeEmbargosDivergencia(),
                        PreparoDisposition.dispensado(),
                        new AdmissibilityDisposition(true, RecursalAuthority.PRESIDENCIA, false, null, false, false, true, false),
                        new PreventionDisposition(true, false, true, true));
            }
            case RecursoExtraordinario ignored -> {
                require(context.instanciaAtual() == InstanceLevel.SUPERIOR, "Recurso extraordinário no STJ exige acórdão do STJ");
                requireColegiateDecision(context);
                require(context.materiaConstitucional(), "Recurso extraordinário exige questão constitucional direta");
                yield externalPlan(
                        perfil.perfilNome(),
                        context,
                        RecursalTribunal.STF,
                        InstanceLevel.EXTRAORDINARY,
                        RecursalAuthority.PLENARIO,
                        PreparoDisposition.obrigatorio(true),
                        new AdmissibilityDisposition(true, RecursalAuthority.PRESIDENCIA, true, RecursalAuthority.RELATOR, true, true, true, true),
                        new PreventionDisposition(true, false, true, true));
            }
            case AgravoRecursoExtraordinario ignored -> externalPlan(
                    perfil.perfilNome(),
                    context,
                    RecursalTribunal.STF,
                    InstanceLevel.EXTRAORDINARY,
                    RecursalAuthority.PLENARIO,
                    PreparoDisposition.obrigatorio(true),
                    new AdmissibilityDisposition(false, null, true, RecursalAuthority.RELATOR, false, true, false, true),
                    new PreventionDisposition(true, false, true, true));
            case RecursoEspecial ignored -> throw new IllegalArgumentException("Espécie incompatível com STJ como origem nesta malha");
            case AgravoRecursoEspecial ignored -> throw new IllegalArgumentException("Espécie incompatível com STJ como origem nesta malha");
            case ApelacaoCivel ignored -> throw new IllegalArgumentException("Espécie incompatível com STJ como origem nesta malha");
            case ApelacaoPenal ignored -> throw new IllegalArgumentException("Espécie incompatível com STJ como origem nesta malha");
            default -> routeExtendedSpecies(context, species);
        };
    }
}
