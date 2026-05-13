package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.RecursalRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.SuperiorTribunalJusticaRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.SupremoTribunalFederalRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalJusticaRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalRegionalEleitoralRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalRegionalFederalRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalSuperiorEleitoralRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.SuperiorTribunalMilitarRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TurmaNacionalUniformizacaoRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalRegionalTrabalhoRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalSuperiorTrabalhoRuleProfile;

public final class RecursalRuleCatalog {

    private final List<RecursalRuleProfile> profiles;
    private final RecursalLocalRegimentalPolicyCatalog localPolicies;

    public RecursalRuleCatalog(List<RecursalRuleProfile> profiles) {
        this(profiles, RecursalLocalRegimentalPolicyCatalog.defaultCatalog());
    }

    public RecursalRuleCatalog(List<RecursalRuleProfile> profiles, RecursalLocalRegimentalPolicyCatalog localPolicies) {
        this.profiles = List.copyOf(profiles);
        this.localPolicies = Objects.requireNonNull(localPolicies, "localPolicies");
    }

    public static RecursalRuleCatalog defaultCatalog() {
        return new RecursalRuleCatalog(List.of(
                new TribunalJusticaRuleProfile(),
                new TribunalRegionalFederalRuleProfile(),
                new TribunalRegionalTrabalhoRuleProfile(),
                new TribunalRegionalEleitoralRuleProfile(),
                new SuperiorTribunalJusticaRuleProfile(),
                new TribunalSuperiorTrabalhoRuleProfile(),
                new TribunalSuperiorEleitoralRuleProfile(),
                new SuperiorTribunalMilitarRuleProfile(),
                new TurmaNacionalUniformizacaoRuleProfile(),
                new SupremoTribunalFederalRuleProfile()
        ));
    }

    public RecursalRoutePlan route(RecursalCaseContext context, RecursalSpecies species) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(species, "species");
        RecursalRoutePlan base = profiles.stream()
                .filter(profile -> profile.supports(context.tribunalOrigem()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Perfil recursal não encontrado para o tribunal: " + context.tribunalOrigem()))
                .route(context, species);
        RecursalRoutePlan adjusted = applyLocalPolicy(context, species, base, localPolicies.policyOf(base.tribunalDetalhadoOrigem()));
        return new RecursalRoutePlan(
                adjusted.profileName(),
                adjusted.tribunalOrigem(),
                adjusted.tribunalDetalhadoOrigem(),
                adjusted.autoridadeOrigemAdmissibilidade(),
                adjusted.tribunalDestino(),
                adjusted.tribunalDetalhadoDestino(),
                adjusted.instanciaDestino(),
                adjusted.autoridadeDestinoAdmissibilidade(),
                adjusted.autoridadeJulgamentoMerito(),
                adjusted.preparo(),
                adjusted.admissibilidade(),
                adjusted.prevencao(),
                adjusted.remessa(),
                RecursalRouteKindResolver.resolve(context, species, adjusted)
        );
    }

    private RecursalRoutePlan applyLocalPolicy(RecursalCaseContext context, RecursalSpecies species, RecursalRoutePlan base, RecursalLocalRegimentalPolicy policy) {
        AdmissibilityDisposition admissibilidade = base.admissibilidade();
        RecursalAuthority autoridadeMerito = base.autoridadeJulgamentoMerito();
        if (isExceptionalSpecies(species) && admissibilidade.juizoOrigem()) {
            RecursalAuthority origem = policy.exigeVicePresidenciaNosExcepcionais()
                    ? RecursalAuthority.VICE_PRESIDENCIA
                    : policy.autoridadeAdmissibilidadeExcepcional();
            admissibilidade = new AdmissibilityDisposition(
                    true,
                    origem,
                    admissibilidade.juizoDestino(),
                    admissibilidade.autoridadeDestino(),
                    admissibilidade.admiteRetratacao(),
                    admissibilidade.admiteSobrestamento(),
                    admissibilidade.exigePrequestionamento(),
                    admissibilidade.exigeDemonstracaoRepercussaoGeral()
            );
            autoridadeMerito = policy.autoridadeMeritoExcepcional();
        }
        if (species instanceof AgravoInterno && context.autoridadeAtual().presidencia()) {
            autoridadeMerito = policy.autoridadeAgravoInternoContraPresidencia();
        }
        if (species instanceof EmbargosDeclaracao && context.autoridadeAtual().colegiado()) {
            autoridadeMerito = policy.autoridadeEmbargosColegiados();
        }
        if ((species instanceof RecursoEspecial || species instanceof RecursoExtraordinario) && policy.admiteJuizoRetratacaoPosSobrestamento()) {
            admissibilidade = new AdmissibilityDisposition(
                    admissibilidade.juizoOrigem(),
                    admissibilidade.autoridadeOrigem(),
                    admissibilidade.juizoDestino(),
                    admissibilidade.autoridadeDestino(),
                    true,
                    admissibilidade.admiteSobrestamento(),
                    admissibilidade.exigePrequestionamento(),
                    admissibilidade.exigeDemonstracaoRepercussaoGeral()
            );
        }
        return new RecursalRoutePlan(
                base.profileName(),
                base.tribunalOrigem(),
                base.tribunalDetalhadoOrigem(),
                admissibilidade.autoridadeOrigem(),
                base.tribunalDestino(),
                base.tribunalDetalhadoDestino(),
                base.instanciaDestino(),
                admissibilidade.autoridadeDestino(),
                autoridadeMerito,
                base.preparo(),
                admissibilidade,
                base.prevencao(),
                base.remessa(),
                base.routeKind()
        );
    }

    private boolean isExceptionalSpecies(RecursalSpecies species) {
        return species instanceof RecursoEspecial
                || species instanceof RecursoExtraordinario
                || species instanceof AgravoRecursoEspecial
                || species instanceof AgravoRecursoExtraordinario;
    }
}
