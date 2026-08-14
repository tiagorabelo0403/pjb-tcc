package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class TransitoJulgadoArquivamentoEngine {
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PainelServiceCommons commons;
    private final PerfilDashboardContextFactory contextFactory;
    private final PjbAuthorizationService authorizationService;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final PostJudgmentOperationalResolver operationalResolver;
    private final ExecutionIncidentResolver executionIncidentResolver;
    private final ExecutionEnforcementResolver executionEnforcementResolver;
    private final TransitoJulgadoPatrimonialWorkflowSupport patrimonialWorkflowSupport;
    private final TransitoJulgadoExpropriationWorkflowSupport expropriationWorkflowSupport;
    private final TransitoJulgadoTerminalWorkflowSupport terminalWorkflowSupport;
    private final TransitoJulgadoExecutionDiagnosticSupport executionDiagnosticSupport;
    private final TransitoJulgadoNarrativeSupport narrativeSupport;
    private final ExecutionMeshStateService executionMeshStateService;

    public TransitoJulgadoArquivamentoEngine(
            ProcessoRepository processoRepository,
            WorkItemRepository workItemRepository,
            PainelServiceCommons commons,
            PerfilDashboardContextFactory contextFactory,
            PjbAuthorizationService authorizationService,
            ProcessoLifecycleMachine lifecycleMachine,
            PostJudgmentOperationalResolver operationalResolver,
            ExecutionIncidentResolver executionIncidentResolver,
            ExecutionEnforcementResolver executionEnforcementResolver,
            TransitoJulgadoPatrimonialWorkflowSupport patrimonialWorkflowSupport,
            TransitoJulgadoExpropriationWorkflowSupport expropriationWorkflowSupport,
            TransitoJulgadoTerminalWorkflowSupport terminalWorkflowSupport,
            TransitoJulgadoExecutionDiagnosticSupport executionDiagnosticSupport,
            TransitoJulgadoNarrativeSupport narrativeSupport,
            ExecutionMeshStateService executionMeshStateService
    ) {
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.commons = commons;
        this.contextFactory = contextFactory;
        this.authorizationService = authorizationService;
        this.lifecycleMachine = lifecycleMachine;
        this.operationalResolver = operationalResolver;
        this.executionIncidentResolver = executionIncidentResolver;
        this.executionEnforcementResolver = executionEnforcementResolver;
        this.patrimonialWorkflowSupport = patrimonialWorkflowSupport;
        this.expropriationWorkflowSupport = expropriationWorkflowSupport;
        this.terminalWorkflowSupport = terminalWorkflowSupport;
        this.executionDiagnosticSupport = executionDiagnosticSupport;
        this.narrativeSupport = narrativeSupport;
        this.executionMeshStateService = executionMeshStateService;
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.certificar-transito", maxMillis = 1500, critical = true)
    public Map<String, Object> certificarTransitoEmJulgado(Long processoId, String fundamentacao) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        ProcessoLifecycleDecision decision = enforceLifecycle(processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO);
        PostJudgmentOperationalProfile profile = operationalResolver.resolve(processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO, fundamentacao, 0D);
        String dedupKey = UUID.nameUUIDFromBytes(("TRANSITO:" + processoId).getBytes(StandardCharsets.UTF_8)).toString();
        WorkItem transitoItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.CERTIDAO)
                .titulo("Certidão de Trânsito em Julgado — " + processo.getNumeroProcesso())
                .descricao(joinDescription(fundamentacao, profile))
                .queueCode(profile.queueCode())
                .inboxKey(profile.inboxKey())
                .assignedRole(profile.assignedRole())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(profile.priority())
                .blocking(profile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(profile.baseLegal())
                .dueAt(profile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(transitoItem);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO);
        processoRepository.save(processo);
        commons.publishUserHistory(ctx.usuario(), "SERVIDOR", "TRANSITO_JULGADO_CERTIFICADO", "Trânsito em julgado certificado.", processo, processoId);
        LinkedHashMap<String, Object> out = baseProfilePayload(processo, profile, decision);
        out.put("status", "TRANSITO_JULGADO_CERTIFICADO");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", transitoItem.getId());
        out.put("dedupKey", dedupKey);
        out.put("proximoPasso", profile.executionTrack());
        out.put("inboxKey", profile.inboxKey());
        out.put("queueCode", profile.queueCode());
        return out;
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.iniciar-cumprimento-sentenca", maxMillis = 2500, critical = true)
    public Map<String, Object> iniciarCumprimentoSentenca(Long processoId, String tipoCumprimento, double valorExequendo) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_PROCURADOR");
        ProcessoLifecycleDecision decision = enforceLifecycle(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);
        PostJudgmentOperationalProfile profile = operationalResolver.resolve(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, tipoCumprimento, valorExequendo);
        String dedupKey = UUID.nameUUIDFromBytes(("CUMPRIMENTO:" + processoId + ':' + tipoCumprimento).getBytes(StandardCharsets.UTF_8)).toString();
        WorkItem cumprimentoItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(FaseProcessual.CUMPRIMENTO_SENTENCA)
                .templateCode(dedupKey)
                .type(WorkItemType.CUMPRIMENTO_SENTENCA)
                .titulo("Cumprimento de Sentença — " + tipoCumprimento + " — R$ " + formatDecimal(valorExequendo))
                .descricao("Tipo: " + tipoCumprimento + " | Valor: R$ " + formatDecimal(valorExequendo) + " | Perfil: " + profile.descriptor())
                .queueCode(profile.queueCode())
                .inboxKey(profile.inboxKey())
                .assignedRole(profile.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(profile.priority())
                .blocking(profile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(profile.baseLegal())
                .dueAt(profile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(cumprimentoItem);
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO);
        processoRepository.save(processo);
        executionMeshStateService.recordExecutionStart(processo, profile, valorExequendo);
        commons.publishUserHistory(ctx.usuario(), "ADVOCACIA", "CUMPRIMENTO_SENTENCA_INICIADO", "Cumprimento de sentença iniciado.", processo, processoId);
        LinkedHashMap<String, Object> out = baseProfilePayload(processo, profile, decision);
        out.put("status", "CUMPRIMENTO_SENTENCA_INICIADO");
        out.put("tipo", tipoCumprimento);
        out.put("valorExequendo", valorExequendo);
        out.put("processoId", processoId);
        out.put("workItemId", cumprimentoItem.getId());
        out.put("dedupKey", dedupKey);
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("assignedRole", profile.assignedRole().name());
        out.put("blocking", profile.blocking());
        out.put("reviewDesk", profile.reviewDesk());
        return out;
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.instaurar-incidente-executivo", maxMillis = 2500, critical = true)
    public Map<String, Object> instaurarIncidenteExecutivo(Long processoId,
                                                           String incidente,
                                                           String fundamentacao,
                                                           double valorGarantia) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_PROCURADOR", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        PostJudgmentOperationalProfile operationalProfile = operationalResolver.resolve(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, incidente, valorGarantia);
        ExecutionIncidentProfile incidentProfile = executionIncidentResolver.resolve(processo, incidente, fundamentacao, valorGarantia);
        String dedupKey = UUID.nameUUIDFromBytes(("INCIDENTE_EXECUTIVO:" + processoId + ':' + incidentProfile.incidentType() + ':' + firstNonBlank(fundamentacao, "SEM_FUNDAMENTACAO")).getBytes(StandardCharsets.UTF_8)).toString();
        Optional<WorkItem> existente = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, dedupKey, WorkItemStatus.CANCELADO);
        if (existente.isPresent()) {
            WorkItem item = existente.get();
            return Map.of(
                    "status", "DUPLICATA_IGNORADA",
                    "processoId", processoId,
                    "workItemId", item.getId(),
                    "dedupKey", dedupKey,
                    "incidentDescriptor", incidentProfile.descriptor()
            );
        }
        WorkItem incidenteItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.PETICAO)
                .titulo("Incidente executivo — " + narrativeSupport.incidentLabel(incidentProfile.incidentType()) + " — " + processo.getNumeroProcesso())
                .descricao(narrativeSupport.buildIncidentDescription(fundamentacao, operationalProfile, incidentProfile))
                .queueCode(incidentProfile.queueCode())
                .inboxKey(incidentProfile.inboxKey())
                .assignedRole(incidentProfile.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(incidentProfile.priority())
                .blocking(incidentProfile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(incidentProfile.baseLegal())
                .dueAt(incidentProfile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(incidenteItem);
        executionMeshStateService.recordIncident(processo, incidentProfile, incidenteItem);
        commons.publishUserHistory(ctx.usuario(), "ADVOCACIA", "INCIDENTE_EXECUTIVO_AUTUADO", "Incidente executivo autuado: " + incidentProfile.incidentType(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "INCIDENTE_EXECUTIVO_AUTUADO");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", incidenteItem.getId());
        out.put("dedupKey", dedupKey);
        out.put("incidentDescriptor", incidentProfile.descriptor());
        out.put("incidentType", incidentProfile.incidentType());
        out.put("queueCode", incidentProfile.queueCode());
        out.put("inboxKey", incidentProfile.inboxKey());
        out.put("assignedRole", incidentProfile.assignedRole().name());
        out.put("executionImpact", incidentProfile.executionImpact());
        out.put("preventionMode", incidentProfile.preventionMode());
        out.put("warnings", incidentProfile.warnings());
        out.put("reviewChecklist", incidentProfile.reviewChecklist());
        out.put("operationalProfile", operationalProfile.toMap());
        out.put("incidentProfile", incidentProfile.toMap());
        return out;
    }

    public Map<String, Object> diagnosticarMalhaExecutiva(Long processoId) {
        Processo processo = loadProcesso(processoId);
        return executionDiagnosticSupport.diagnosticarMalhaExecutiva(processoId, processo, isExecutionEligible(processo));
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.praticar-ato-executivo", maxMillis = 2500, critical = true)
    public Map<String, Object> praticarAtoExecutivo(Long processoId,
                                                    String ato,
                                                    String detalhe,
                                                    double valorOperacao) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_PROCURADOR", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        PostJudgmentOperationalProfile operationalProfile = operationalResolver.resolve(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, ato, valorOperacao);
        ExecutionEnforcementProfile enforcementProfile = executionEnforcementResolver.resolve(processo, ato, detalhe, valorOperacao);
        String dedupKey = UUID.nameUUIDFromBytes(("ATO_EXECUTIVO:" + processoId + ':' + enforcementProfile.actType() + ':' + firstNonBlank(detalhe, "SEM_DETALHE")).getBytes(StandardCharsets.UTF_8)).toString();
        Optional<WorkItem> existente = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, dedupKey, WorkItemStatus.CANCELADO);
        if (existente.isPresent()) {
            WorkItem item = existente.get();
            return Map.of(
                    "status", "DUPLICATA_IGNORADA",
                    "processoId", processoId,
                    "workItemId", item.getId(),
                    "dedupKey", dedupKey,
                    "actDescriptor", enforcementProfile.descriptor()
            );
        }
        WorkItem atoItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(narrativeSupport.resolveExecutionPhaseForAct(processo, enforcementProfile))
                .templateCode(dedupKey)
                .type(narrativeSupport.resolveWorkItemTypeForAct(enforcementProfile))
                .titulo(narrativeSupport.buildActTitle(enforcementProfile, processo))
                .descricao(narrativeSupport.buildEnforcementDescription(detalhe, operationalProfile, enforcementProfile, valorOperacao))
                .queueCode(enforcementProfile.queueCode())
                .inboxKey(enforcementProfile.inboxKey())
                .assignedRole(enforcementProfile.assignedRole())
                .status(narrativeSupport.resolveWorkItemStatusForAct(enforcementProfile))
                .prioridade(enforcementProfile.priority())
                .blocking(enforcementProfile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(enforcementProfile.baseLegal())
                .dueAt(enforcementProfile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(atoItem);
        executionMeshStateService.recordEnforcementAct(processo, enforcementProfile, atoItem, detalhe, valorOperacao);
        commons.publishUserHistory(ctx.usuario(), "EXECUCAO", "ATO_EXECUTIVO_PRATICADO", "Ato executivo praticado: " + enforcementProfile.actType(), processo, processoId);

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "ATO_EXECUTIVO_REGISTRADO");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", atoItem.getId());
        out.put("dedupKey", dedupKey);
        out.put("actDescriptor", enforcementProfile.descriptor());
        out.put("actType", enforcementProfile.actType());
        out.put("speciesCode", enforcementProfile.speciesCode());
        out.put("queueCode", enforcementProfile.queueCode());
        out.put("inboxKey", enforcementProfile.inboxKey());
        out.put("assignedRole", enforcementProfile.assignedRole().name());
        out.put("executionImpact", enforcementProfile.executionImpact());
        out.put("constrictionMode", enforcementProfile.constrictionMode());
        out.put("expropriationMode", enforcementProfile.expropriationMode());
        out.put("satisfactionMode", enforcementProfile.satisfactionMode());
        out.put("warnings", enforcementProfile.warnings());
        out.put("fundamentos", enforcementProfile.fundamentos());
        out.put("reviewChecklist", enforcementProfile.reviewChecklist());
        out.put("operationalProfile", operationalProfile.toMap());
        out.put("enforcementProfile", enforcementProfile.toMap());
        return out;
    }


    @Transactional
    @PjbTransactionalBudget(operation = "transito.praticar-constricao-patrimonial", maxMillis = 2500, critical = true)
    public Map<String, Object> praticarConstricaoPatrimonial(Long processoId,
                                                             String ato,
                                                             String bem,
                                                             String detalhe,
                                                             String convenio,
                                                             double valorOperacao) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_PROCURADOR", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        PostJudgmentOperationalProfile operationalProfile = operationalResolver.resolve(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO, ato, valorOperacao);
        return patrimonialWorkflowSupport.praticarConstricaoPatrimonial(
                processo,
                processoId,
                ctx.usuario(),
                operationalProfile,
                ato,
                bem,
                detalhe,
                convenio,
                valorOperacao);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.integrar-constricao-externa", maxMillis = 3000, critical = true)
    public Map<String, Object> integrarConstricaoExterna(Long processoId,
                                                         String ato,
                                                         String bem,
                                                         String convenio,
                                                         String referenciaExterna,
                                                         double valorOperacao) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_PROCURADOR", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        return patrimonialWorkflowSupport.integrarConstricaoExterna(
                processo,
                processoId,
                ctx.usuario(),
                ato,
                bem,
                convenio,
                referenciaExterna,
                valorOperacao);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.homologar-expropriacao-final", maxMillis = 2500, critical = true)
    public Map<String, Object> homologarExpropriacaoFinal(Long processoId,
                                                          String ato,
                                                          String bem,
                                                          String modalidade,
                                                          String adquirente,
                                                          double valorArrematacao) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        return expropriationWorkflowSupport.homologarExpropriacaoFinal(
                processo,
                processoId,
                ctx.usuario(),
                ato,
                bem,
                modalidade,
                adquirente,
                valorArrematacao);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.liquidar-produto-expropriacao", maxMillis = 2500, critical = true)
    public Map<String, Object> liquidarProdutoExpropriacao(Long processoId,
                                                           String bem,
                                                           String modoProduto,
                                                           String preferencia,
                                                           String subrogacao,
                                                           double valorProduto,
                                                           double saldoExecutado,
                                                           double saldoCredor) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_CONTADOR_JUDICIAL", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        return expropriationWorkflowSupport.liquidarProdutoExpropriacao(
                processo,
                processoId,
                ctx.usuario(),
                bem,
                modoProduto,
                preferencia,
                subrogacao,
                valorProduto,
                saldoExecutado,
                saldoCredor);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.consolidar-fechamento-executivo", maxMillis = 2500, critical = true)
    public Map<String, Object> consolidarFechamentoExecutivo(Long processoId,
                                                             String modoFechamento,
                                                             String preferencia,
                                                             String subrogacao,
                                                             double percentualSatisfeito,
                                                             double saldoRemanescente,
                                                             String motivo) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_CONTADOR_JUDICIAL", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        return terminalWorkflowSupport.consolidarFechamentoExecutivo(
                processo,
                processoId,
                ctx.usuario(),
                modoFechamento,
                preferencia,
                subrogacao,
                percentualSatisfeito,
                saldoRemanescente,
                motivo);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.registrar-satisfacao-terminal", maxMillis = 2500, critical = true)
    public Map<String, Object> registrarSatisfacaoTerminal(Long processoId,
                                                           String modo,
                                                           double percentualSatisfeito,
                                                           double saldoRemanescente,
                                                           String fundamento) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_PROCURADOR", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertTerminalEligible(processo);
        return terminalWorkflowSupport.registrarSatisfacaoTerminal(
                processo,
                processoId,
                ctx.usuario(),
                modo,
                percentualSatisfeito,
                saldoRemanescente,
                fundamento);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.governar-expropriacao", maxMillis = 2500, critical = true)
    public Map<String, Object> governarExpropriacao(Long processoId,
                                                    String ato,
                                                    String bem,
                                                    String modalidade,
                                                    double valorReferencia) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_PROCURADOR", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        return expropriationWorkflowSupport.governarExpropriacao(
                processo,
                processoId,
                ctx.usuario(),
                ato,
                bem,
                modalidade,
                valorReferencia);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.reconciliar-constricao-externa", maxMillis = 2500, critical = true)
    public Map<String, Object> reconciliarConstricaoExterna(Long processoId,
                                                            String bem,
                                                            String convenio,
                                                            String statusExterno,
                                                            String referenciaExterna,
                                                            double valorOperacao) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_PROCURADOR");
        assertIncidentEligible(processo);
        return patrimonialWorkflowSupport.reconciliarConstricaoExterna(
                processo,
                processoId,
                ctx.usuario(),
                bem,
                convenio,
                statusExterno,
                referenciaExterna,
                valorOperacao);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.vincular-arquivamento-terminal", maxMillis = 2500, critical = true)
    public Map<String, Object> vincularArquivamentoTerminal(Long processoId,
                                                            String operacao,
                                                            String disposicaoTerminal,
                                                            String motivo,
                                                            double percentualSatisfeito,
                                                            double saldoRemanescente) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        return terminalWorkflowSupport.vincularArquivamentoTerminal(
                processo,
                processoId,
                ctx.usuario(),
                operacao,
                disposicaoTerminal,
                motivo,
                percentualSatisfeito,
                saldoRemanescente);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.planejar-ciclo-leilao-expropriatorio", maxMillis = 2500, critical = true)
    public Map<String, Object> planejarCicloLeilaoExpropriatorio(Long processoId,
                                                                 String ato,
                                                                 String bem,
                                                                 String modalidade,
                                                                 int tentativa,
                                                                 double valorReferencia) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_PROCURADOR", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO");
        assertIncidentEligible(processo);
        return expropriationWorkflowSupport.planejarCicloLeilaoExpropriatorio(
                processo,
                processoId,
                ctx.usuario(),
                ato,
                bem,
                modalidade,
                tentativa,
                valorReferencia);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.deflagrar-contingencia-constricao-externa", maxMillis = 2500, critical = true)
    public Map<String, Object> deflagrarContingenciaConstricaoExterna(Long processoId,
                                                                      String bem,
                                                                      String convenio,
                                                                      String statusExterno,
                                                                      String referenciaExterna,
                                                                      double valorOperacao) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM", "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_PROCURADOR");
        assertIncidentEligible(processo);
        return patrimonialWorkflowSupport.deflagrarContingenciaConstricaoExterna(
                processo,
                processoId,
                ctx.usuario(),
                bem,
                convenio,
                statusExterno,
                referenciaExterna,
                valorOperacao);
    }

    public Map<String, Object> consultarSnapshotExecutivo(Long processoId) {
        Processo processo = loadProcesso(processoId);
        return executionDiagnosticSupport.consultarSnapshotExecutivo(processo);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.determinar-baixa-arquivamento", maxMillis = 1500, critical = true)
    public Map<String, Object> determinarBaixaArquivamento(Long processoId, String motivoArquivamento) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_SERVIDOR", "ROLE_SERVIDOR_FORUM");
        if (ctx.usuario().getTipoUsuario() != null && ctx.usuario().getTipoUsuario().isServidorJudiciario()) {
            authorizationService.requireFuncaoServidorCapability(processo, AcaoProcessualServidor.ARQUIVAR);
        }
        ProcessoLifecycleDecision decision = enforceLifecycle(processo, ProcessoLifecycleAction.ARQUIVAR);
        PostJudgmentOperationalProfile profile = operationalResolver.resolve(processo, ProcessoLifecycleAction.ARQUIVAR, motivoArquivamento, 0D);
        String dedupKey = UUID.nameUUIDFromBytes(("ARQUIVO:" + processoId).getBytes(StandardCharsets.UTF_8)).toString();
        WorkItem arquivamento = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.CERTIDAO)
                .titulo("Baixa e Arquivamento — " + processo.getNumeroProcesso())
                .descricao("Motivo: " + motivoArquivamento + " | Perfil: " + profile.descriptor())
                .queueCode(profile.queueCode())
                .inboxKey(profile.inboxKey())
                .assignedRole(profile.assignedRole())
                .status(WorkItemStatus.CONCLUIDO)
                .prioridade(profile.priority())
                .blocking(profile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(profile.baseLegal())
                .dueAt(profile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(arquivamento);
        Map<String, Object> terminalReference = executionMeshStateService.currentTerminalReference(processo);
        double percentualSatisfeito = decimalAsDouble(terminalReference.get("satisfactionPercent"));
        double saldoRemanescente = decimalAsDouble(terminalReference.get("residualAmount"));
        TerminalArchiveLinkProfile archiveLinkProfile = terminalWorkflowSupport.resolveArchiveLinkProfile(
                processo,
                "ARQUIVAR",
                stringValue(terminalReference.get("terminalDisposition"), profile.satisfactionMode()),
                motivoArquivamento,
                percentualSatisfeito,
                saldoRemanescente);
        if ("INELEGIVEL".equals(archiveLinkProfile.archiveEligibility())) {
            throw new IllegalStateException("Arquivamento bloqueado pela malha terminal: " + archiveLinkProfile.archiveLinkMode());
        }
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.ARQUIVAR);
        processoRepository.save(processo);
        executionMeshStateService.recordArchiveLinkage(processo, archiveLinkProfile, arquivamento, motivoArquivamento);
        commons.publishUserHistory(ctx.usuario(), "SERVIDOR", "PROCESSO_ARQUIVADO", "Processo baixado e arquivado. Motivo: " + motivoArquivamento, processo, processoId);
        LinkedHashMap<String, Object> out = baseProfilePayload(processo, profile, decision);
        out.put("status", "PROCESSO_ARQUIVADO");
        out.put("motivo", motivoArquivamento);
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", arquivamento.getId());
        out.put("arquivadoEm", Instant.now());
        out.put("dedupKey", dedupKey);
        out.put("retentionMode", profile.retentionMode());
        out.put("archiveLinkProfile", archiveLinkProfile.toMap());
        return out;
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.abrir-desarquivamento", maxMillis = 2000, critical = true)
    public Map<String, Object> abrirDesarquivamento(Long processoId, String motivoReativacao) {
        Processo processo = loadProcesso(processoId);
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_SERVIDOR_FORUM");
        ProcessoLifecycleDecision decision = enforceLifecycle(processo, ProcessoLifecycleAction.DESARQUIVAR);
        PostJudgmentOperationalProfile profile = operationalResolver.resolve(processo, ProcessoLifecycleAction.DESARQUIVAR, motivoReativacao, 0D);
        WorkItem reativacao = WorkItem.builder()
                .processo(processo)
                .faseOrigem(FaseProcessual.CONHECIMENTO)
                .templateCode("DESARQUIVAMENTO:" + processoId + ':' + Instant.now().toEpochMilli())
                .type(WorkItemType.DISTRIBUICAO)
                .titulo("Desarquivamento — " + processo.getNumeroProcesso())
                .descricao("Motivo de reativação: " + motivoReativacao + " | Perfil: " + profile.descriptor())
                .queueCode(profile.queueCode())
                .inboxKey(profile.inboxKey())
                .assignedRole(profile.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(profile.priority())
                .blocking(profile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(profile.baseLegal())
                .dueAt(profile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(reativacao);
        Map<String, Object> terminalReference = executionMeshStateService.currentTerminalReference(processo);
        TerminalArchiveLinkProfile archiveLinkProfile = terminalWorkflowSupport.resolveArchiveLinkProfile(
                processo,
                "DESARQUIVAR",
                stringValue(terminalReference.get("terminalDisposition"), profile.satisfactionMode()),
                motivoReativacao,
                decimalAsDouble(terminalReference.get("satisfactionPercent")),
                decimalAsDouble(terminalReference.get("residualAmount")));
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.DESARQUIVAR);
        processoRepository.save(processo);
        executionMeshStateService.recordArchiveLinkage(processo, archiveLinkProfile, reativacao, motivoReativacao);
        LinkedHashMap<String, Object> out = baseProfilePayload(processo, profile, decision);
        out.put("status", "DESARQUIVAMENTO_SOLICITADO");
        out.put("processoId", processoId);
        out.put("motivo", motivoReativacao);
        out.put("workItemId", reativacao.getId());
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("archiveLinkProfile", archiveLinkProfile.toMap());
        return out;
    }

    public Map<String, Object> calcularEfeitosTransito(Long processoId) {
        Processo processo = loadProcesso(processoId);
        PostJudgmentOperationalProfile profile = operationalResolver.resolve(processo, ProcessoLifecycleAction.CERTIFICAR_TRANSITO, processo.getResultadoFinal(), 0D);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("coisaJulgadaFormal", true);
        out.put("coisaJulgadaMaterial", !isExcluirCoisaJulgadaMaterial(processo));
        out.put("resJudicataScope", profile.resJudicataScope());
        out.put("executionTrack", profile.executionTrack());
        out.put("archiveTrack", profile.archiveTrack());
        out.put("reviewDesk", profile.reviewDesk());
        out.put("retentionMode", profile.retentionMode());
        out.put("satisfactionMode", profile.satisfactionMode());
        out.put("prazoPrescricaoCumprimento", resolverPrazoPrescricao(processo));
        out.put("possibilidadeCumprimento", resolverPossibilidadeCumprimento(processo));
        out.put("possivelAcaoRescisoria", true);
        out.put("prazoAcaoRescisoria", "2 anos a partir do trânsito em julgado (Art. 975 CPC)");
        out.put("descriptor", profile.descriptor());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("warnings", profile.warnings());
        out.put("profile", profile.toMap());
        return out;
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private ProcessoLifecycleDecision enforceLifecycle(Processo processo, ProcessoLifecycleAction action) {
        ProcessoLifecycleDecision decision = lifecycleMachine.preview(processo, action);
        if (!decision.permitida()) {
            throw new IllegalStateException(decision.motivo());
        }
        return decision;
    }

    private void assertIncidentEligible(Processo processo) {
        if (!isExecutionEligible(processo)) {
            throw new IllegalStateException("Incidente executivo exige fase de cumprimento, execução ou título pós-julgamento apto.");
        }
        if (processo.getStatusProcesso() == StatusProcesso.ARQUIVADO) {
            throw new IllegalStateException("Processo arquivado exige desarquivamento controlado antes do incidente executivo.");
        }
    }

    private void assertTerminalEligible(Processo processo) {
        if (!isExecutionEligible(processo)) {
            throw new IllegalStateException("Satisfação terminal exige trilha executiva materialmente instaurada.");
        }
        if (processo.getStatusProcesso() == StatusProcesso.ARQUIVADO) {
            throw new IllegalStateException("Processo arquivado exige reabertura controlada antes da consolidação terminal.");
        }
    }

    private boolean isExecutionEligible(Processo processo) {
        if (processo == null) {
            return false;
        }
        if (processo.getFaseAtual() == FaseProcessual.CUMPRIMENTO_SENTENCA
                || processo.getFaseAtual() == FaseProcessual.EXECUCAO
                || processo.getFaseAtual() == FaseProcessual.EXECUTORIA
                || processo.getFaseAtual() == FaseProcessual.PENHORA) {
            return true;
        }
        return processo.getStatusProcesso() == StatusProcesso.TRANSITO_EM_JULGADO
                || processo.getStatusProcesso() == StatusProcesso.CUMPRIMENTO_SENTENCA
                || processo.getStatusProcesso() == StatusProcesso.JULGADO
                || processo.getStatusProcesso() == StatusProcesso.SENTENCA_PROFERIDA;
    }

    private LinkedHashMap<String, Object> baseProfilePayload(Processo processo,
                                                             PostJudgmentOperationalProfile profile,
                                                             ProcessoLifecycleDecision decision) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processo.getId());
        out.put("numero", processo.getNumeroProcesso());
        out.put("operationDescriptor", profile.descriptor());
        out.put("resJudicataScope", profile.resJudicataScope());
        out.put("executionTrack", profile.executionTrack());
        out.put("archiveTrack", profile.archiveTrack());
        out.put("reviewDesk", profile.reviewDesk());
        out.put("retentionMode", profile.retentionMode());
        out.put("satisfactionMode", profile.satisfactionMode());
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("lifecycleTransitionKey", decision.transitionKey());
        out.put("lifecycleWarnings", decision.alertas());
        out.put("profile", profile.toMap());
        return out;
    }

    private String joinDescription(String fundamentacao, PostJudgmentOperationalProfile profile) {
        String base = fundamentacao == null ? "" : fundamentacao.trim();
        if (base.isBlank()) {
            return "Perfil: " + profile.descriptor();
        }
        return base + " | Perfil: " + profile.descriptor();
    }

    private boolean isExcluirCoisaJulgadaMaterial(Processo processo) {
        String fase = faseToken(processo);
        return fase != null && (fase.contains("EXTINTO_SEM_RESOLUCAO") || fase.contains("ARQUIVADO_PROVISORIAMENTE"));
    }

    private String resolverPrazoPrescricao(Processo processo) {
        if (processo.getRito() == null) {
            return "5 anos (Art. 206 §5º CC)";
        }
        return switch (processo.getRito()) {
            case EXECUCAO_FISCAL -> "5 anos (Lei 6.830/80 Art. 40)";
            case TRABALHISTA_ORDINARIO, TRABALHISTA_SUMARISSIMO, TRABALHISTA_SUMARIO_ALCADA, TRABALHISTA_INQUERITO_FALTA_GRAVE, TRABALHISTA_ACAO_CUMPRIMENTO, TRABALHISTA_CUMPRIMENTO_SENTENCA, TRABALHISTA_EXECUCAO -> "2 anos (Art. 7º XXIX CF)";
            default -> "5 anos (Art. 206 §5º CC)";
        };
    }

    private String resolverPossibilidadeCumprimento(Processo processo) {
        String fase = faseToken(processo);
        if (fase == null) {
            return "VERIFICAR_COM_ADVOGADO";
        }
        if (fase.contains("EXTINTO")) {
            return "IMPOSSIVEL_EXTINCAO_SEM_RESOLUCAO";
        }
        if (processo.getStatusProcesso() == StatusProcesso.ARQUIVADO) {
            return "DEPENDENTE_DE_DESARQUIVAMENTO_CONTROLADO";
        }
        return "POSSIVEL_CUMPRIMENTO_VOLUNTARIO_OU_FORCADO";
    }

    private String faseToken(Processo processo) {
        return processo.getFaseAtual() == null ? null : processo.getFaseAtual().name();
    }

    private double decimalAsDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0D;
        }
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
