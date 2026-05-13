package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.AutorizacaoCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.ResultadoAutorizacaoCaixaInstitucional;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;

final class PjbAuthorizationInstitutionalCapabilityFacade {

    private final PolicyRegistry policyRegistry;
    private final PjbAuthorizationDecisionContextResolver contextResolver;
    private final AutorizacaoCaixaInstitucionalService autorizacaoCaixaInstitucionalService;
    private final PjbAuthorizationTrailAssembler trailAssembler;

    PjbAuthorizationInstitutionalCapabilityFacade(PolicyRegistry policyRegistry,
                                                  PjbAuthorizationDecisionContextResolver contextResolver,
                                                  AutorizacaoCaixaInstitucionalService autorizacaoCaixaInstitucionalService,
                                                  PjbAuthorizationTrailAssembler trailAssembler) {
        this.policyRegistry = policyRegistry;
        this.contextResolver = contextResolver;
        this.autorizacaoCaixaInstitucionalService = autorizacaoCaixaInstitucionalService;
        this.trailAssembler = trailAssembler;
    }

    PjbAuthorizationEvaluation evaluate(String expedicaoUuid,
                                        String unidadeCodigo,
                                        String caixaCodigo,
                                        CapacidadeCaixaInstitucional capacidade) {
        PolicyRegistry.ActivePolicy activePolicy = policyRegistry.active();
        PjbAuthorizationDecisionContext context = contextResolver.resolve();
        if (unidadeCodigo == null || unidadeCodigo.isBlank() || caixaCodigo == null || caixaCodigo.isBlank() || capacidade == null) {
            AuthzDecision denied = AuthzDecision.deny("institutional_context_invalid", activePolicy.policy().version());
            return trailAssembler.assembleInstitutionalCapability(
                    expedicaoUuid,
                    unidadeCodigo,
                    caixaCodigo,
                    capacidade,
                    null,
                    context,
                    activePolicy,
                    denied
            );
        }
        ResultadoAutorizacaoCaixaInstitucional resultado = autorizacaoCaixaInstitucionalService.autorizar(
                expedicaoUuid,
                unidadeCodigo,
                caixaCodigo,
                capacidade
        );
        AuthzDecision decision = resultado.autorizado()
                ? AuthzDecision.allow("institutional_capability_granted", activePolicy.policy().version())
                : AuthzDecision.deny("institutional_capability_missing", activePolicy.policy().version());
        return trailAssembler.assembleInstitutionalCapability(
                expedicaoUuid,
                unidadeCodigo,
                caixaCodigo,
                capacidade,
                resultado,
                context,
                activePolicy,
                decision
        );
    }
}
