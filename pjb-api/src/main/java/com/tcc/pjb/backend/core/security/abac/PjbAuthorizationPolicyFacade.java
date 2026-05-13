package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.security.GovBrAssurancePolicy;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.core.security.professional.ProfessionalDocumentScopePolicyService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

final class PjbAuthorizationPolicyFacade {

    private final PolicyRegistry policyRegistry;
    private final PjbAuthorizationDecisionContextResolver contextResolver;
    private final PjbAuthorizationTrailAssembler trailAssembler;
    private final GovBrAssurancePolicy govBrAssurancePolicy;
    private final ProfessionalDocumentScopePolicyService documentScopePolicyService;

    PjbAuthorizationPolicyFacade(PolicyRegistry policyRegistry,
                                 PjbAuthorizationDecisionContextResolver contextResolver,
                                 GovBrAssurancePolicy govBrAssurancePolicy,
                                 ProfessionalDocumentScopePolicyService documentScopePolicyService) {
        this.policyRegistry = policyRegistry;
        this.contextResolver = contextResolver;
        this.trailAssembler = new PjbAuthorizationTrailAssembler();
        this.govBrAssurancePolicy = govBrAssurancePolicy;
        this.documentScopePolicyService = documentScopePolicyService;
    }

    AuthzDecision canReadProcesso(Processo processo, PjbAuthorizationSigiloResolver sigiloResolver) {
        return evaluateReadProcesso(processo, sigiloResolver).decision();
    }

    AuthzDecision canReadVotosColegiados(Processo processo, PjbAuthorizationSigiloResolver sigiloResolver) {
        return evaluateReadVotosColegiados(processo, sigiloResolver).decision();
    }

    AuthzDecision canReadDocumento(Processo processo,
                                   DocumentoProcessual documento,
                                   PjbAuthorizationSigiloResolver sigiloResolver) {
        return evaluateReadDocumento(processo, documento, sigiloResolver).decision();
    }

    PjbAuthorizationEvaluation evaluateReadProcesso(Processo processo,
                                                    PjbAuthorizationSigiloResolver sigiloResolver) {
        PolicyRegistry.ActivePolicy activePolicy = policyRegistry.active();
        PjbAuthorizationDecisionContext context = contextResolver.resolve();
        NivelSigilo effectiveSigilo = sigiloResolver.computeProcessoSigiloEfetivo(processo);
        AuthzDecision baseDecision = activePolicy.policy().canReadProcesso(context.usuario(), processo, context.justificativa());
        PjbAuthorizationStepUpAssessment stepUp = buildStepUp(baseDecision, processo, effectiveSigilo, sigiloResolver);
        AuthzDecision finalDecision = finalizeDecision(baseDecision, stepUp);
        return trailAssembler.assembleProcessRead(processo, context, activePolicy, finalDecision, effectiveSigilo, stepUp);
    }

    PjbAuthorizationEvaluation evaluateReadVotosColegiados(Processo processo,
                                                           PjbAuthorizationSigiloResolver sigiloResolver) {
        PolicyRegistry.ActivePolicy activePolicy = policyRegistry.active();
        PjbAuthorizationDecisionContext context = contextResolver.resolve();
        NivelSigilo effectiveSigilo = sigiloResolver.computeProcessoSigiloEfetivo(processo);
        AuthzDecision baseDecision = activePolicy.policy().canReadVotosColegiados(context.usuario(), processo, context.justificativa());
        PjbAuthorizationStepUpAssessment stepUp = buildStepUp(baseDecision, processo, effectiveSigilo, sigiloResolver);
        AuthzDecision finalDecision = finalizeDecision(baseDecision, stepUp);
        return trailAssembler.assembleVotosRead(processo, context, activePolicy, finalDecision, effectiveSigilo, stepUp);
    }

    PjbAuthorizationEvaluation evaluateReadDocumento(Processo processo,
                                                     DocumentoProcessual documento,
                                                     PjbAuthorizationSigiloResolver sigiloResolver) {
        PolicyRegistry.ActivePolicy activePolicy = policyRegistry.active();
        PjbAuthorizationDecisionContext context = contextResolver.resolve();
        if (documento == null) {
            AuthzDecision denied = AuthzDecision.deny("documento_nulo", activePolicy.policy().version());
            return trailAssembler.assembleDocumentRead(
                    processo,
                    null,
                    context,
                    activePolicy,
                    denied,
                    NivelSigilo.PUBLICO,
                    PjbAuthorizationStepUpAssessment.notRequired("NONE", "documento_nulo")
            );
        }
        NivelSigilo effectiveSigilo = sigiloResolver.computeDocumentoSigiloEfetivo(processo, documento);
        AuthzDecision baseDecision = activePolicy.policy().canReadProcesso(
                context.usuario(),
                sigiloResolver.shadowProcesso(processo, effectiveSigilo),
                context.justificativa()
        );
        PjbAuthorizationStepUpAssessment stepUp = buildStepUp(baseDecision, processo, effectiveSigilo, sigiloResolver);
        AuthzDecision finalDecision = finalizeDecision(baseDecision, stepUp);
        AuthzDecision scopedDecision = documentScopePolicyService.refineDocumentReadDecision(context.usuario(), processo, documento, finalDecision);
        return trailAssembler.assembleDocumentRead(processo, documento, context, activePolicy, scopedDecision, effectiveSigilo, stepUp);
    }

    PjbAuthorizationEvaluation evaluateWriteProcesso(Processo processo,
                                                     PjbAuthorizationSigiloResolver sigiloResolver) {
        PjbAuthorizationDecisionContext context = contextResolver.resolve();
        PjbAuthorizationEvaluation readEvaluation = evaluateReadProcesso(processo, sigiloResolver);
        AuthzDecision readDecision = readEvaluation.decision();
        AuthzDecision finalDecision = readDecision;
        if (readDecision != null && readDecision.allowed() && "advogado_credencial_temporaria".equals(readDecision.reason())) {
            finalDecision = AuthzDecision.deny("advogado_credencial_temporaria_sem_escrita", readDecision.policyVersion());
        }
        PjbAuthorizationDecisionTrail readTrail = readEvaluation.trail();
        PjbAuthorizationStepUpAssessment effectiveStepUp = readTrail == null
                ? PjbAuthorizationStepUpAssessment.notRequired("NONE", "sem_trilha")
                : readTrail.stepUp();
        if (finalDecision != null && finalDecision.allowed()) {
            PjbAuthorizationStepUpAssessment assuranceStepUp = buildGovBrWriteAssuranceStepUp(context, processo);
            effectiveStepUp = mergeStepUp(effectiveStepUp, assuranceStepUp);
            finalDecision = finalizeDecision(finalDecision, assuranceStepUp);
        }
        return trailAssembler.assembleProcessWrite(
                processo,
                context,
                policyRegistry.active(),
                finalDecision,
                readTrail == null ? NivelSigilo.PUBLICO : readTrail.effectiveSigilo(),
                effectiveStepUp
        );
    }


    private PjbAuthorizationStepUpAssessment buildGovBrWriteAssuranceStepUp(PjbAuthorizationDecisionContext context,
                                                                             Processo processo) {
        if (context == null || context.usuario() == null || context.usuario().getTipoUsuario() == null) {
            return PjbAuthorizationStepUpAssessment.notRequired("NONE", "govbr_actor_missing");
        }
        if (context.usuario().getTipoUsuario() != com.tcc.pjb.backend.model.entity.enums.TipoUsuario.CIDADAO) {
            return PjbAuthorizationStepUpAssessment.notRequired("NONE", "govbr_not_cidadao");
        }
        boolean atoSensivel = govBrAssurancePolicy.isSensitiveCitizenAct(processo, context.justificativa());
        if (!atoSensivel) {
            return PjbAuthorizationStepUpAssessment.notRequired("NONE", "govbr_sensitive_write_not_required");
        }
        if (govBrAssurancePolicy.atoNormatizadoAtendido(context.govBrAssuranceLevel(), true)) {
            return PjbAuthorizationStepUpAssessment.satisfied("GOVBR_ASSURANCE", "govbr_ouro_satisfied");
        }
        return PjbAuthorizationStepUpAssessment.requiredButMissing(
                "GOVBR_ASSURANCE",
                "govbr_ouro_required",
                "Atos sensíveis do cidadão exigem nível ouro Gov.br antes da escrita processual."
        );
    }

    private PjbAuthorizationStepUpAssessment mergeStepUp(PjbAuthorizationStepUpAssessment primary,
                                                         PjbAuthorizationStepUpAssessment secondary) {
        if (primary == null) {
            return secondary;
        }
        if (secondary == null) {
            return primary;
        }
        if (primary.deniedByStepUp()) {
            return primary;
        }
        if (secondary.deniedByStepUp()) {
            return secondary;
        }
        if (secondary.required()) {
            return secondary;
        }
        return primary;
    }
    private PjbAuthorizationStepUpAssessment buildStepUp(AuthzDecision decision,
                                                         Processo processo,
                                                         NivelSigilo effectiveSigilo,
                                                         PjbAuthorizationSigiloResolver sigiloResolver) {
        if (decision == null || !decision.allowed()) {
            return PjbAuthorizationStepUpAssessment.notRequired("NONE", "decisao_base_negada");
        }
        return sigiloResolver.assessStepUpForSigiloEfetivo(processo, effectiveSigilo);
    }

    private AuthzDecision finalizeDecision(AuthzDecision baseDecision,
                                           PjbAuthorizationStepUpAssessment stepUp) {
        if (baseDecision == null) {
            return AuthzDecision.deny("decisao_nula", policyRegistry.active().policy().version());
        }
        if (stepUp != null && stepUp.deniedByStepUp()) {
            return AuthzDecision.deny(stepUp.code(), baseDecision.policyVersion());
        }
        return baseDecision;
    }
}
