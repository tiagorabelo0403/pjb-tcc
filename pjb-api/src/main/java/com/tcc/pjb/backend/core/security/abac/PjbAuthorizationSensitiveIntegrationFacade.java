package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

final class PjbAuthorizationSensitiveIntegrationFacade {

    private final PolicyRegistry policyRegistry;
    private final PjbAuthorizationDecisionContextResolver contextResolver;
    private final PjbAuthorizationExternalSystemAccessPolicy externalSystemAccessPolicy;
    private final PjbAuthorizationTrailAssembler trailAssembler;

    PjbAuthorizationSensitiveIntegrationFacade(PolicyRegistry policyRegistry,
                                              PjbAuthorizationDecisionContextResolver contextResolver,
                                              PjbAuthorizationExternalSystemAccessPolicy externalSystemAccessPolicy,
                                              PjbAuthorizationTrailAssembler trailAssembler) {
        this.policyRegistry = policyRegistry;
        this.contextResolver = contextResolver;
        this.externalSystemAccessPolicy = externalSystemAccessPolicy;
        this.trailAssembler = trailAssembler;
    }

    PjbAuthorizationEvaluation evaluateConsultBnmp(Usuario usuario) {
        return evaluateSimple("CONSULT_BNMP", "BNMP", usuario, externalSystemAccessPolicy.canConsultBnmp(usuario), 34);
    }

    PjbAuthorizationEvaluation evaluateConsultCnib(Usuario usuario) {
        return evaluateSimple("CONSULT_CNIB", "CNIB", usuario, externalSystemAccessPolicy.canConsultCnib(usuario), 36);
    }

    PjbAuthorizationEvaluation evaluateConsultRenajud(Usuario usuario) {
        return evaluateSimple("CONSULT_RENAJUD", "RENAJUD", usuario, externalSystemAccessPolicy.canConsultRenajud(usuario), 38);
    }

    PjbAuthorizationEvaluation evaluateRequestInfojud(Usuario usuario, boolean delegatedOperation) {
        return evaluateDelegated(
                "REQUEST_INFOJUD",
                "INFOJUD",
                usuario,
                delegatedOperation,
                externalSystemAccessPolicy.canRequestInfojud(usuario, delegatedOperation),
                48
        );
    }

    PjbAuthorizationEvaluation evaluateRequestSisbajud(Usuario usuario, boolean delegatedOperation) {
        return evaluateDelegated(
                "REQUEST_SISBAJUD",
                "SISBAJUD",
                usuario,
                delegatedOperation,
                externalSystemAccessPolicy.canRequestSisbajud(usuario, delegatedOperation),
                56
        );
    }

    PjbAuthorizationEvaluation evaluateRequestSerasajud(Usuario usuario, boolean delegatedOperation) {
        return evaluateDelegated(
                "REQUEST_SERASAJUD",
                "SERASAJUD",
                usuario,
                delegatedOperation,
                externalSystemAccessPolicy.canRequestSerasajud(usuario, delegatedOperation),
                42
        );
    }

    PjbAuthorizationEvaluation evaluateLocatePessoaByCpf(Usuario usuario) {
        return evaluateSimple(
                "LOCATE_PESSOA_CPF",
                "LOCALIZACAO_CPF",
                usuario,
                externalSystemAccessPolicy.canLocatePessoaByCpf(usuario),
                44
        );
    }

    PjbAuthorizationEvaluation evaluateConsultarLocalizacaoSemProcesso(Usuario usuario) {
        return evaluateSimple(
                "CONSULT_LOCALIZACAO_SEM_PROCESSO",
                "LOCALIZACAO_SEM_PROCESSO",
                usuario,
                externalSystemAccessPolicy.canConsultarLocalizacaoSemProcesso(usuario),
                58
        );
    }

    PjbAuthorizationEvaluation evaluateAccessEnderecoEstrito(Usuario usuario, boolean possuiContextoFormal) {
        boolean allowed = externalSystemAccessPolicy.canAccessEnderecoEstrito(usuario, possuiContextoFormal);
        boolean governanceRequired = externalSystemAccessPolicy.requiresFormalContextForStrictAddress(usuario);
        PjbAuthorizationGovernanceAssessment governance = governanceRequired
                ? possuiContextoFormal
                    ? PjbAuthorizationGovernanceAssessment.satisfied(
                            "FORMAL_CONTEXT",
                            "formal_context_satisfied",
                            "ENDERECO_ESTRITO"
                    )
                    : PjbAuthorizationGovernanceAssessment.requiredButMissing(
                            "FORMAL_CONTEXT",
                            "formal_context_required",
                            "ENDERECO_ESTRITO",
                            "Contexto formal exigido para endereço estrito."
                    )
                : PjbAuthorizationGovernanceAssessment.notRequired("NONE", "formal_context_not_required", "ENDERECO_ESTRITO");
        return assembleExternal(
                "ACCESS_ENDERECO_ESTRITO",
                "ENDERECO_ESTRITO",
                usuario,
                allowed,
                PjbAuthorizationStepUpAssessment.notRequired("NONE", "endereco_estrito"),
                governance,
                64
        );
    }

    PjbAuthorizationEvaluation evaluateConsultarInteligenciaPatrimonial(Usuario usuario) {
        return evaluateSimple(
                "CONSULT_INTELIGENCIA_PATRIMONIAL",
                "INTELIGENCIA_PATRIMONIAL",
                usuario,
                externalSystemAccessPolicy.canConsultarInteligenciaPatrimonial(usuario),
                68
        );
    }

    PjbAuthorizationEvaluation evaluateConsultarMandadosPessoa(Usuario usuario) {
        return evaluateSimple(
                "CONSULT_MANDADOS_PESSOA",
                "MANDADOS_PESSOA",
                usuario,
                externalSystemAccessPolicy.canConsultarMandadosPessoa(usuario),
                52
        );
    }

    private PjbAuthorizationEvaluation evaluateSimple(String action,
                                                      String systemCode,
                                                      Usuario usuario,
                                                      boolean allowed,
                                                      int sensitivityScore) {
        return assembleExternal(
                action,
                systemCode,
                usuario,
                allowed,
                PjbAuthorizationStepUpAssessment.notRequired("NONE", systemCode),
                PjbAuthorizationGovernanceAssessment.notRequired("NONE", systemCode, systemCode),
                sensitivityScore
        );
    }

    private PjbAuthorizationEvaluation evaluateDelegated(String action,
                                                         String systemCode,
                                                         Usuario usuario,
                                                         boolean delegatedOperation,
                                                         boolean allowed,
                                                         int sensitivityScore) {
        PjbAuthorizationGovernanceAssessment governance = delegatedOperation && isAssessor(usuario)
                ? allowed
                    ? PjbAuthorizationGovernanceAssessment.satisfied(
                            "DELEGATION",
                            "delegation_credential_satisfied",
                            systemCode
                    )
                    : PjbAuthorizationGovernanceAssessment.requiredButMissing(
                            "DELEGATION",
                            "delegation_credential_required",
                            systemCode,
                            "Credencial de delegação institucional exigida para a operação solicitada."
                    )
                : PjbAuthorizationGovernanceAssessment.notRequired("NONE", "delegation_not_required", systemCode);
        return assembleExternal(
                action,
                systemCode,
                usuario,
                allowed,
                PjbAuthorizationStepUpAssessment.notRequired("NONE", systemCode),
                governance,
                sensitivityScore
        );
    }

    private PjbAuthorizationEvaluation assembleExternal(String action,
                                                        String systemCode,
                                                        Usuario usuario,
                                                        boolean allowed,
                                                        PjbAuthorizationStepUpAssessment stepUp,
                                                        PjbAuthorizationGovernanceAssessment governance,
                                                        int sensitivityScore) {
        PolicyRegistry.ActivePolicy activePolicy = policyRegistry.active();
        AuthzDecision decision = new AuthzDecision(
                allowed && (governance == null || !governance.deniedByGovernance()),
                resolveReason(systemCode, allowed, governance),
                activePolicy.policy().version()
        );
        return trailAssembler.assembleExternalAccess(
                action,
                systemCode,
                usuario,
                contextResolver.resolve(usuario),
                activePolicy,
                decision,
                stepUp,
                governance,
                sensitivityScore
        );
    }

    private String resolveReason(String systemCode,
                                 boolean allowed,
                                 PjbAuthorizationGovernanceAssessment governance) {
        if (governance != null && governance.deniedByGovernance()) {
            return governance.code();
        }
        return allowed
                ? systemCode.toLowerCase() + "_access_granted"
                : systemCode.toLowerCase() + "_access_denied";
    }

    private boolean isAssessor(Usuario usuario) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        return tipo != null && tipo.isAssessor();
    }
}
