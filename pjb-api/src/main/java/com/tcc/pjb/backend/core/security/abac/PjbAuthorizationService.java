package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.AutorizacaoCaixaInstitucionalService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.GovBrAssuranceExtractor;
import com.tcc.pjb.backend.core.security.GovBrAssurancePolicy;
import com.tcc.pjb.backend.core.security.ProcessoPartyCpfLinkPolicy;
import com.tcc.pjb.backend.core.security.professional.ProfessionalDocumentScopePolicyService;
import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.platform.security.rbac.RbacGrantedRoleResolver;
import com.tcc.pjb.backend.service.processo.ProcessoObservabilidadeAcessoService;
import org.springframework.stereotype.Service;

@Service
public class PjbAuthorizationService {

    private final CurrentUserService currentUserService;
    private final PjbAuthorizationPolicyFacade policyFacade;
    private final PjbAuthorizationSigiloResolver sigiloResolver;
    private final PjbAuthorizationAuditFacade auditFacade;
    private final PjbAuthorizationExternalSystemAccessPolicy externalSystemAccessPolicy;
    private final PjbAuthorizationInstitutionalCapabilityFacade institutionalCapabilityFacade;
    private final PjbAuthorizationSensitiveIntegrationFacade sensitiveIntegrationFacade;
    private final PjbAuthorizationFuncaoServidorFacade funcaoServidorFacade;
    private final UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;
    private final RbacGrantedRoleResolver rbacGrantedRoleResolver;

    public PjbAuthorizationService(CurrentUserService currentUserService,
                                   PolicyRegistry policyRegistry,
                                   AuditLedgerService auditLedgerService,
                                   SigiloAccessService sigiloAccessService,
                                   ProcessoObservabilidadeAcessoService processoObservabilidadeAcessoService,
                                   RbacGrantedRoleResolver rbacGrantedRoleResolver,
                                   AutorizacaoCaixaInstitucionalService autorizacaoCaixaInstitucionalService,
                                   PjbAuthorizationTrailRegistry authorizationTrailRegistry,
                                   PjbAuthorizationTrailReadModelService authorizationTrailReadModelService,
                                   PjbAuthorizationTrailAnalyticsRefreshQueueService authorizationTrailAnalyticsRefreshQueueService,
                                   GovBrAssuranceExtractor govBrAssuranceExtractor,
                                   GovBrAssurancePolicy govBrAssurancePolicy,
                                   ProfessionalDocumentScopePolicyService professionalDocumentScopePolicyService,
                                   FuncaoServidorJudiciarioRepository funcaoServidorJudiciarioRepository,
                                   UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository) {
        this.currentUserService = currentUserService;
        PjbAuthorizationDecisionContextResolver contextResolver = new PjbAuthorizationDecisionContextResolver(currentUserService, govBrAssuranceExtractor);
        PjbAuthorizationTrailAssembler trailAssembler = new PjbAuthorizationTrailAssembler();
        this.sigiloResolver = new PjbAuthorizationSigiloResolver(currentUserService, sigiloAccessService);
        this.auditFacade = new PjbAuthorizationAuditFacade(
                auditLedgerService,
                processoObservabilidadeAcessoService,
                authorizationTrailRegistry,
                authorizationTrailReadModelService,
                authorizationTrailAnalyticsRefreshQueueService
        );
        this.externalSystemAccessPolicy = new PjbAuthorizationExternalSystemAccessPolicy();
        this.policyFacade = new PjbAuthorizationPolicyFacade(
                policyRegistry,
                contextResolver,
                govBrAssurancePolicy,
                professionalDocumentScopePolicyService
        );
        this.institutionalCapabilityFacade = new PjbAuthorizationInstitutionalCapabilityFacade(
                policyRegistry,
                contextResolver,
                autorizacaoCaixaInstitucionalService,
                trailAssembler
        );
        this.sensitiveIntegrationFacade = new PjbAuthorizationSensitiveIntegrationFacade(
                policyRegistry,
                contextResolver,
                externalSystemAccessPolicy,
                trailAssembler
        );
        this.unidadeJudiciariaCompetenciaRepository = unidadeJudiciariaCompetenciaRepository;
        this.funcaoServidorFacade = new PjbAuthorizationFuncaoServidorFacade(
                policyRegistry,
                contextResolver,
                funcaoServidorJudiciarioRepository,
                trailAssembler
        );
        this.rbacGrantedRoleResolver = rbacGrantedRoleResolver;
    }

    public AuthzDecision canReadProcesso(Processo processo) {
        return evaluateReadProcesso(processo).decision();
    }

    public void requireRole(Usuario usuario, String... allowedRoles) {
        if (!hasAnyRole(usuario, allowedRoles)) {
            throw new AccessDeniedPjbException("Acesso negado ao perfil informado.");
        }
    }

    public void requireRoleAny(Usuario usuario, String... allowedRoles) {
        requireRole(usuario, allowedRoles);
    }

    public void requireInstitutionalBoxCapability(String unidadeCodigo,
                                                  String caixaCodigo,
                                                  CapacidadeCaixaInstitucional capacidade) {
        requireInstitutionalBoxCapability(null, unidadeCodigo, caixaCodigo, capacidade);
    }

    public void requireInstitutionalBoxCapability(String expedicaoUuid,
                                                  String unidadeCodigo,
                                                  String caixaCodigo,
                                                  CapacidadeCaixaInstitucional capacidade) {
        PjbAuthorizationEvaluation evaluation = institutionalCapabilityFacade.evaluate(
                expedicaoUuid,
                unidadeCodigo,
                caixaCodigo,
                capacidade
        );
        auditFacade.registerDecision(evaluation);
        if (!evaluation.allowed()) {
            throw accessDenied("Acesso negado à capacidade institucional", evaluation);
        }
    }

    public void requireFuncaoServidorCapability(Processo processo, AcaoProcessualServidor acao) {
        Long unidadeId = resolveUnidadeId(processo);
        PjbAuthorizationEvaluation evaluation = funcaoServidorFacade.evaluate(unidadeId, acao, processo);
        auditFacade.registerDecision(evaluation);
        if (!evaluation.allowed()) {
            throw accessDenied("Acesso negado à ação processual do servidor", evaluation);
        }
    }

    private Long resolveUnidadeId(Processo processo) {
        if (processo == null || processo.getUnidadeJudiciariaCodigo() == null
                || processo.getUnidadeJudiciariaCodigo().isBlank()) {
            return null;
        }
        return unidadeJudiciariaCompetenciaRepository.findByCodigo(processo.getUnidadeJudiciariaCodigo())
                .map(UnidadeJudiciariaCompetencia::getId)
                .orElse(null);
    }

    public AuthzDecision canReadVotosColegiados(Processo processo) {
        return evaluateReadVotosColegiados(processo).decision();
    }

    public void requireReadVotosColegiados(Processo processo) {
        PjbAuthorizationEvaluation evaluation = evaluateReadVotosColegiados(processo);
        auditFacade.registerDecision(evaluation);
        if (!evaluation.allowed()) {
            throw accessDenied("Acesso negado aos votos do julgamento", evaluation);
        }
        auditFacade.registerGrantedReadVotos(processo);
    }

    public void requireReadProcesso(Processo processo) {
        PjbAuthorizationEvaluation evaluation = evaluateReadProcesso(processo);
        auditFacade.registerDecision(evaluation);
        if (!evaluation.allowed()) {
            throw accessDenied("Acesso negado ao processo", evaluation);
        }
        auditFacade.registerGrantedReadProcesso(processo);
    }

    public void requireReadProcessoAsCidadaoParte(Processo processo) {
        requireReadProcesso(processo);
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getTipoUsuario() != TipoUsuario.CIDADAO) {
            return;
        }
        String cpf = usuario.getCpf();
        if (cpf == null || cpf.isBlank()) {
            throw new AccessDeniedPjbException("CPF não encontrado no perfil");
        }
        if (!ProcessoPartyCpfLinkPolicy.vinculado(cpf, processo)) {
            throw new AccessDeniedPjbException("Cidadão não é parte do processo.");
        }
    }

    public void requireReadProcessoAtSecrecy(Processo processo, NivelSigilo sigiloEfetivo) {
        if (processo == null) {
            throw new AccessDeniedPjbException("Acesso negado ao processo: processo_nulo");
        }
        requireReadProcesso(sigiloResolver.shadowProcessoAtSecrecy(processo, sigiloEfetivo));
    }

    public AuthzDecision canReadDocumento(Processo processo, DocumentoProcessual documento) {
        return evaluateReadDocumento(processo, documento).decision();
    }

    public AuthzDecision canReadDocumentoAtSecrecy(Processo processo,
                                                   DocumentoProcessual documento,
                                                   NivelSigilo sigiloEfetivo) {
        return canReadDocumento(sigiloResolver.shadowProcessoAtSecrecy(processo, sigiloEfetivo), documento);
    }

    public void requireReadDocumento(Processo processo, DocumentoProcessual documento) {
        PjbAuthorizationEvaluation evaluation = evaluateReadDocumento(processo, documento);
        auditFacade.registerDecision(evaluation);
        if (!evaluation.allowed()) {
            throw accessDenied("Acesso negado ao documento", evaluation);
        }
        auditFacade.registerGrantedReadDocumento(documento);
    }

    public void requireReadDocumentoAtSecrecy(Processo processo,
                                              DocumentoProcessual documento,
                                              NivelSigilo sigiloEfetivo) {
        requireReadDocumento(sigiloResolver.shadowProcessoAtSecrecy(processo, sigiloEfetivo), documento);
    }

    public void requireWriteProcesso(Processo processo) {
        PjbAuthorizationEvaluation evaluation = policyFacade.evaluateWriteProcesso(processo, sigiloResolver);
        auditFacade.registerDecision(evaluation);
        if (!evaluation.allowed()) {
            throw accessDenied("Acesso negado ao processo", evaluation);
        }
        auditFacade.registerGrantedWriteProcesso(processo);
    }

    public boolean canConsultBnmp(Usuario usuario) {
        return sensitiveIntegrationFacade.evaluateConsultBnmp(usuario).allowed();
    }

    public void requireConsultBnmp(Usuario usuario) {
        requireSensitiveIntegration("Acesso negado ao BNMP", sensitiveIntegrationFacade.evaluateConsultBnmp(usuario));
    }

    public boolean canConsultCnib(Usuario usuario) {
        return sensitiveIntegrationFacade.evaluateConsultCnib(usuario).allowed();
    }

    public void requireConsultCnib(Usuario usuario) {
        requireSensitiveIntegration("Acesso negado à CNIB", sensitiveIntegrationFacade.evaluateConsultCnib(usuario));
    }

    public boolean canConsultRenajud(Usuario usuario) {
        return sensitiveIntegrationFacade.evaluateConsultRenajud(usuario).allowed();
    }

    public void requireConsultRenajud(Usuario usuario) {
        requireSensitiveIntegration("Acesso negado ao RENAJUD", sensitiveIntegrationFacade.evaluateConsultRenajud(usuario));
    }

    public boolean canRequestInfojud(Usuario usuario, boolean delegatedOperation) {
        return sensitiveIntegrationFacade.evaluateRequestInfojud(usuario, delegatedOperation).allowed();
    }

    public void requireRequestInfojud(Usuario usuario, boolean delegatedOperation) {
        requireSensitiveIntegration("Acesso negado ao INFOJUD", sensitiveIntegrationFacade.evaluateRequestInfojud(usuario, delegatedOperation));
    }

    public boolean canRequestSisbajud(Usuario usuario, boolean delegatedOperation) {
        return sensitiveIntegrationFacade.evaluateRequestSisbajud(usuario, delegatedOperation).allowed();
    }

    public void requireRequestSisbajud(Usuario usuario, boolean delegatedOperation) {
        requireSensitiveIntegration("Acesso negado ao SISBAJUD", sensitiveIntegrationFacade.evaluateRequestSisbajud(usuario, delegatedOperation));
    }

    public boolean canRequestSerasajud(Usuario usuario, boolean delegatedOperation) {
        return sensitiveIntegrationFacade.evaluateRequestSerasajud(usuario, delegatedOperation).allowed();
    }

    public void requireRequestSerasajud(Usuario usuario, boolean delegatedOperation) {
        requireSensitiveIntegration("Acesso negado ao SERASAJUD", sensitiveIntegrationFacade.evaluateRequestSerasajud(usuario, delegatedOperation));
    }

    public boolean canSolicitarBloqueioAtivos(Usuario usuario, boolean delegatedOperation) {
        return canRequestSisbajud(usuario, delegatedOperation);
    }

    public boolean canLocatePessoaByCpf(Usuario usuario) {
        return sensitiveIntegrationFacade.evaluateLocatePessoaByCpf(usuario).allowed();
    }

    public void requireLocatePessoaByCpf(Usuario usuario) {
        requireSensitiveIntegration("Acesso negado à localização por CPF", sensitiveIntegrationFacade.evaluateLocatePessoaByCpf(usuario));
    }

    public boolean canConsultarLocalizacaoSemProcesso(Usuario usuario) {
        return sensitiveIntegrationFacade.evaluateConsultarLocalizacaoSemProcesso(usuario).allowed();
    }

    public void requireConsultarLocalizacaoSemProcesso(Usuario usuario) {
        requireSensitiveIntegration("Acesso negado à localização sem processo", sensitiveIntegrationFacade.evaluateConsultarLocalizacaoSemProcesso(usuario));
    }

    public boolean requiresFormalContextForStrictAddress(Usuario usuario) {
        return externalSystemAccessPolicy.requiresFormalContextForStrictAddress(usuario);
    }

    public boolean canAccessEnderecoEstrito(Usuario usuario, boolean possuiContextoFormal) {
        return sensitiveIntegrationFacade.evaluateAccessEnderecoEstrito(usuario, possuiContextoFormal).allowed();
    }

    public void requireAccessEnderecoEstrito(Usuario usuario, boolean possuiContextoFormal) {
        requireSensitiveIntegration("Acesso negado ao endereço estrito", sensitiveIntegrationFacade.evaluateAccessEnderecoEstrito(usuario, possuiContextoFormal));
    }

    public boolean canConsultarInteligenciaPatrimonial(Usuario usuario) {
        return sensitiveIntegrationFacade.evaluateConsultarInteligenciaPatrimonial(usuario).allowed();
    }

    public void requireConsultarInteligenciaPatrimonial(Usuario usuario) {
        requireSensitiveIntegration("Acesso negado à inteligência patrimonial", sensitiveIntegrationFacade.evaluateConsultarInteligenciaPatrimonial(usuario));
    }

    public boolean canConsultarMandadosPessoa(Usuario usuario) {
        return sensitiveIntegrationFacade.evaluateConsultarMandadosPessoa(usuario).allowed();
    }

    public void requireConsultarMandadosPessoa(Usuario usuario) {
        requireSensitiveIntegration("Acesso negado à consulta de mandados", sensitiveIntegrationFacade.evaluateConsultarMandadosPessoa(usuario));
    }

    private PjbAuthorizationEvaluation evaluateReadProcesso(Processo processo) {
        return policyFacade.evaluateReadProcesso(processo, sigiloResolver);
    }

    private PjbAuthorizationEvaluation evaluateReadVotosColegiados(Processo processo) {
        return policyFacade.evaluateReadVotosColegiados(processo, sigiloResolver);
    }

    private PjbAuthorizationEvaluation evaluateReadDocumento(Processo processo,
                                                             DocumentoProcessual documento) {
        return policyFacade.evaluateReadDocumento(processo, documento, sigiloResolver);
    }

    private AccessDeniedPjbException accessDenied(String prefix,
                                                  PjbAuthorizationEvaluation evaluation) {
        PjbAuthorizationDecisionTrail trail = evaluation == null ? null : evaluation.trail();
        PjbAuthorizationStepUpAssessment stepUp = trail == null ? null : trail.stepUp();
        if (stepUp != null && stepUp.deniedByStepUp() && stepUp.userMessage() != null && !stepUp.userMessage().isBlank()) {
            return new AccessDeniedPjbException(prefix + ": " + stepUp.userMessage());
        }
        PjbAuthorizationGovernanceAssessment governance = trail == null ? null : trail.governance();
        if (governance != null && governance.deniedByGovernance() && governance.userMessage() != null && !governance.userMessage().isBlank()) {
            return new AccessDeniedPjbException(prefix + ": " + governance.userMessage());
        }
        AuthzDecision decision = evaluation == null ? null : evaluation.decision();
        return new AccessDeniedPjbException(prefix + ": " + (decision == null ? "decisao_nula" : decision.reason()));
    }


    private void requireSensitiveIntegration(String prefix,
                                             PjbAuthorizationEvaluation evaluation) {
        auditFacade.registerDecision(evaluation);
        if (!evaluation.allowed()) {
            throw accessDenied(prefix, evaluation);
        }
    }

    private boolean hasAnyRole(Usuario usuario, String... allowedRoles) {
        if (usuario == null || usuario.getTipoUsuario() == null || allowedRoles == null || allowedRoles.length == 0) {
            return false;
        }
        java.util.Set<String> granted = rbacGrantedRoleResolver.resolveGrantedRoles(usuario.getTipoUsuario());
        for (String allowedRole : allowedRoles) {
            if (allowedRole != null && granted.contains(allowedRole.trim().toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
