package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.ResultadoAutorizacaoCaixaInstitucional;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.Objects;

final class PjbAuthorizationTrailAssembler {

    PjbAuthorizationEvaluation assembleProcessRead(Processo processo,
                                                   PjbAuthorizationDecisionContext context,
                                                   PolicyRegistry.ActivePolicy activePolicy,
                                                   AuthzDecision decision,
                                                   NivelSigilo effectiveSigilo,
                                                   PjbAuthorizationStepUpAssessment stepUp) {
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        "READ_PROCESSO",
                        "PROCESSO",
                        resolveProcessId(processo),
                        context,
                        activePolicy,
                        decision,
                        effectiveSigilo,
                        stepUp,
                        PjbAuthorizationGovernanceAssessment.notRequired("NONE", "process_read", "processo")
                )
        );
    }

    PjbAuthorizationEvaluation assembleVotosRead(Processo processo,
                                                 PjbAuthorizationDecisionContext context,
                                                 PolicyRegistry.ActivePolicy activePolicy,
                                                 AuthzDecision decision,
                                                 NivelSigilo effectiveSigilo,
                                                 PjbAuthorizationStepUpAssessment stepUp) {
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        "READ_VOTOS",
                        "PROCESSO_VOTOS",
                        resolveProcessId(processo),
                        context,
                        activePolicy,
                        decision,
                        effectiveSigilo,
                        stepUp,
                        PjbAuthorizationGovernanceAssessment.notRequired("NONE", "votos_read", "colegiado")
                )
        );
    }

    PjbAuthorizationEvaluation assembleDocumentRead(Processo processo,
                                                    DocumentoProcessual documento,
                                                    PjbAuthorizationDecisionContext context,
                                                    PolicyRegistry.ActivePolicy activePolicy,
                                                    AuthzDecision decision,
                                                    NivelSigilo effectiveSigilo,
                                                    PjbAuthorizationStepUpAssessment stepUp) {
        String resourceId = documento != null && documento.getId() != null
                ? documento.getId().toString()
                : resolveProcessId(processo);
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        "READ_DOCUMENTO",
                        "DOCUMENTO",
                        resourceId,
                        context,
                        activePolicy,
                        decision,
                        effectiveSigilo,
                        stepUp,
                        PjbAuthorizationGovernanceAssessment.notRequired("NONE", "document_read", "documento")
                )
        );
    }

    PjbAuthorizationEvaluation assembleProcessWrite(Processo processo,
                                                    PjbAuthorizationDecisionContext context,
                                                    PolicyRegistry.ActivePolicy activePolicy,
                                                    AuthzDecision decision,
                                                    NivelSigilo effectiveSigilo,
                                                    PjbAuthorizationStepUpAssessment stepUp) {
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        "WRITE_PROCESSO",
                        "PROCESSO",
                        resolveProcessId(processo),
                        context,
                        activePolicy,
                        decision,
                        effectiveSigilo,
                        stepUp,
                        PjbAuthorizationGovernanceAssessment.notRequired("NONE", "process_write", "processo")
                )
        );
    }

    PjbAuthorizationEvaluation assembleCidadaoParte(Processo processo,
                                                    PjbAuthorizationDecisionContext context,
                                                    AuthzDecision decision) {
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        "CIDADAO_PARTE",
                        "PROCESSO",
                        resolveProcessId(processo),
                        context,
                        null,
                        decision,
                        NivelSigilo.PUBLICO,
                        PjbAuthorizationStepUpAssessment.notRequired("NONE", "cidadao_parte"),
                        PjbAuthorizationGovernanceAssessment.notRequired("NONE", "cidadao_parte", "processo")
                )
        );
    }

    PjbAuthorizationEvaluation assembleInstitutionalCapability(String expedicaoUuid,
                                                               String unidadeCodigo,
                                                               String caixaCodigo,
                                                               CapacidadeCaixaInstitucional capacidade,
                                                               ResultadoAutorizacaoCaixaInstitucional resultado,
                                                               PjbAuthorizationDecisionContext context,
                                                               PolicyRegistry.ActivePolicy activePolicy,
                                                               AuthzDecision decision) {
        String resourceId = normalizeResourceId(
                unidadeCodigo,
                caixaCodigo,
                capacidade == null ? null : capacidade.name(),
                expedicaoUuid
        );
        PjbAuthorizationGovernanceAssessment governance = resultado != null && resultado.autorizado()
                ? PjbAuthorizationGovernanceAssessment.satisfied(
                        "INSTITUTIONAL_MEMBERSHIP",
                        "institutional_capability_satisfied",
                        capacityScope(capacidade)
                )
                : PjbAuthorizationGovernanceAssessment.requiredButMissing(
                        "INSTITUTIONAL_MEMBERSHIP",
                        "institutional_capability_missing",
                        capacityScope(capacidade),
                        "Capacidade institucional ausente para a caixa solicitada."
                );
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        "INSTITUTIONAL_BOX_CAPABILITY",
                        "CAIXA_INSTITUCIONAL",
                        resourceId,
                        context,
                        activePolicy,
                        decision,
                        NivelSigilo.PUBLICO,
                        PjbAuthorizationStepUpAssessment.notRequired("NONE", "institutional_capability"),
                        governance
                )
        );
    }

    PjbAuthorizationEvaluation assembleFuncaoServidorCapability(Processo processo,
                                                                Long unidadeId,
                                                                AcaoProcessualServidor acao,
                                                                boolean autorizado,
                                                                PjbAuthorizationDecisionContext context,
                                                                PolicyRegistry.ActivePolicy activePolicy,
                                                                AuthzDecision decision) {
        String resourceId = normalizeFuncaoServidorResourceId(unidadeId, acao, processo);
        String scope = acao == null ? "funcao_servidor" : acao.name();
        PjbAuthorizationGovernanceAssessment governance = autorizado
                ? PjbAuthorizationGovernanceAssessment.satisfied("FUNCAO_SERVIDOR", "funcao_servidor_capacidade_satisfeita", scope)
                : PjbAuthorizationGovernanceAssessment.requiredButMissing("FUNCAO_SERVIDOR", "funcao_servidor_capacidade_ausente", scope,
                        "Servidor não possui função ativa com a capacidade exigida na unidade do processo.");
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        "FUNCAO_SERVIDOR_CAPABILITY",
                        "FUNCAO_SERVIDOR",
                        resourceId,
                        context,
                        activePolicy,
                        decision,
                        NivelSigilo.PUBLICO,
                        PjbAuthorizationStepUpAssessment.notRequired("NONE", "funcao_servidor_capability"),
                        governance
                )
        );
    }

    private String normalizeFuncaoServidorResourceId(Long unidadeId, AcaoProcessualServidor acao, Processo processo) {
        String unidade = unidadeId == null ? "UNIDADE_DESCONHECIDA" : String.valueOf(unidadeId);
        String acaoStr = acao == null ? "ACAO_DESCONHECIDA" : acao.name();
        return unidade + ':' + acaoStr + ':' + resolveProcessId(processo);
    }

    PjbAuthorizationEvaluation assembleExternalAccess(String action,
                                                      String systemCode,
                                                      Usuario actor,
                                                      PjbAuthorizationDecisionContext context,
                                                      PolicyRegistry.ActivePolicy activePolicy,
                                                      AuthzDecision decision,
                                                      PjbAuthorizationStepUpAssessment stepUp,
                                                      PjbAuthorizationGovernanceAssessment governance,
                                                      int sensitivityScore) {
        PjbAuthorizationDecisionContext effectiveContext = context == null
                ? new PjbAuthorizationDecisionContext(actor, null, null, null)
                : context;
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        action,
                        "INTEGRACAO_EXTERNA",
                        systemCode == null || systemCode.isBlank() ? "UNKNOWN" : systemCode,
                        effectiveContext,
                        activePolicy,
                        decision,
                        NivelSigilo.PUBLICO,
                        stepUp,
                        governance,
                        sensitivityScore
                )
        );
    }

    private PjbAuthorizationDecisionTrail assemble(String action,
                                                   String resourceType,
                                                   String resourceId,
                                                   PjbAuthorizationDecisionContext context,
                                                   PolicyRegistry.ActivePolicy activePolicy,
                                                   AuthzDecision decision,
                                                   NivelSigilo effectiveSigilo,
                                                   PjbAuthorizationStepUpAssessment stepUp,
                                                   PjbAuthorizationGovernanceAssessment governance) {
        return assemble(action, resourceType, resourceId, context, activePolicy, decision, effectiveSigilo, stepUp, governance, 0);
    }

    private PjbAuthorizationDecisionTrail assemble(String action,
                                                   String resourceType,
                                                   String resourceId,
                                                   PjbAuthorizationDecisionContext context,
                                                   PolicyRegistry.ActivePolicy activePolicy,
                                                   AuthzDecision decision,
                                                   NivelSigilo effectiveSigilo,
                                                   PjbAuthorizationStepUpAssessment stepUp,
                                                   PjbAuthorizationGovernanceAssessment governance,
                                                   int sensitivityScore) {
        int riskScore = computeRiskScore(decision, effectiveSigilo, stepUp, governance, sensitivityScore);
        Usuario usuario = context == null ? null : context.usuario();
        return new PjbAuthorizationDecisionTrail(
                action,
                resourceType,
                resourceId == null || resourceId.isBlank() ? "UNKNOWN" : resourceId,
                decision != null && decision.allowed(),
                decision == null ? "decisao_nula" : Objects.requireNonNullElse(decision.reason(), "sem_motivo"),
                decision == null ? null : decision.policyVersion(),
                activePolicy == null ? null : activePolicy.descriptorSha256(),
                usuario == null ? null : usuario.getId(),
                usuario == null || usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name(),
                context == null ? null : context.requestId(),
                context == null ? null : context.justificativa(),
                effectiveSigilo == null ? NivelSigilo.PUBLICO : effectiveSigilo,
                PjbAuthorizationRiskLevel.fromScore(riskScore),
                riskScore,
                stepUp,
                governance
        );
    }

    private int computeRiskScore(AuthzDecision decision,
                                 NivelSigilo effectiveSigilo,
                                 PjbAuthorizationStepUpAssessment stepUp,
                                 PjbAuthorizationGovernanceAssessment governance,
                                 int sensitivityScore) {
        NivelSigilo sigilo = effectiveSigilo == null ? NivelSigilo.PUBLICO : effectiveSigilo;
        int score = Math.max(5, sigilo.getNivel() * 18) + Math.max(0, sensitivityScore);
        if (decision == null || !decision.allowed()) {
            score += 42;
        } else {
            score += 8;
        }
        if (stepUp != null && stepUp.required()) {
            score += stepUp.satisfied() ? 10 : 28;
        }
        if (governance != null && governance.required()) {
            score += governance.satisfied() ? 8 : 24;
        }
        return Math.min(100, score);
    }

    private String resolveProcessId(Processo processo) {
        if (processo == null) {
            return "UNKNOWN";
        }
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        if (processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank()) {
            return processo.getNumeroProcesso();
        }
        return processo.getId() == null ? "UNKNOWN" : String.valueOf(processo.getId());
    }

    private String normalizeResourceId(String unidadeCodigo,
                                       String caixaCodigo,
                                       String capacidade,
                                       String expedicaoUuid) {
        String unidade = unidadeCodigo == null || unidadeCodigo.isBlank() ? "UNIDADE_DESCONHECIDA" : unidadeCodigo.trim().toUpperCase();
        String caixa = caixaCodigo == null || caixaCodigo.isBlank() ? "CAIXA_DESCONHECIDA" : caixaCodigo.trim().toUpperCase();
        String cap = capacidade == null || capacidade.isBlank() ? "CAPACIDADE_DESCONHECIDA" : capacidade.trim().toUpperCase();
        String expedicao = expedicaoUuid == null || expedicaoUuid.isBlank() ? "SEM_EXPEDICAO" : expedicaoUuid.trim().toUpperCase();
        return unidade + ':' + caixa + ':' + cap + ':' + expedicao;
    }

    private String capacityScope(CapacidadeCaixaInstitucional capacidade) {
        return capacidade == null ? "institucional" : capacidade.name();
    }
}
