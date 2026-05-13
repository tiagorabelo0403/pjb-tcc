package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransitoJulgadoTerminalWorkflowSupport {

    private final ExecutionClosureGovernanceResolver executionClosureGovernanceResolver;
    private final ExecutionSatisfactionResolver executionSatisfactionResolver;
    private final TerminalArchiveLinkResolver terminalArchiveLinkResolver;
    private final WorkItemRepository workItemRepository;
    private final PainelServiceCommons commons;
    private final TransitoJulgadoNarrativeSupport narrativeSupport;
    private final ExecutionMeshStateService executionMeshStateService;

    public TransitoJulgadoTerminalWorkflowSupport(
            ExecutionClosureGovernanceResolver executionClosureGovernanceResolver,
            ExecutionSatisfactionResolver executionSatisfactionResolver,
            TerminalArchiveLinkResolver terminalArchiveLinkResolver,
            WorkItemRepository workItemRepository,
            PainelServiceCommons commons,
            TransitoJulgadoNarrativeSupport narrativeSupport,
            ExecutionMeshStateService executionMeshStateService
    ) {
        this.executionClosureGovernanceResolver = executionClosureGovernanceResolver;
        this.executionSatisfactionResolver = executionSatisfactionResolver;
        this.terminalArchiveLinkResolver = terminalArchiveLinkResolver;
        this.workItemRepository = workItemRepository;
        this.commons = commons;
        this.narrativeSupport = narrativeSupport;
        this.executionMeshStateService = executionMeshStateService;
    }

    public Map<String, Object> consolidarFechamentoExecutivo(Processo processo,
                                                             Long processoId,
                                                             Usuario usuario,
                                                             String modoFechamento,
                                                             String preferencia,
                                                             String subrogacao,
                                                             double percentualSatisfeito,
                                                             double saldoRemanescente,
                                                             String motivo) {
        ExecutionClosureGovernanceProfile profile = executionClosureGovernanceResolver.resolve(
                processo,
                modoFechamento,
                preferencia,
                subrogacao,
                percentualSatisfeito,
                saldoRemanescente,
                motivo);
        String dedupKey = UUID.nameUUIDFromBytes(("CONSOLIDACAO_FECHAMENTO_EXECUTIVO:" + processoId + ':' + profile.closureMode() + ':' + profile.archiveReadiness() + ':' + firstNonBlank(motivo, "SEM_MOTIVO")).getBytes(StandardCharsets.UTF_8)).toString();
        Optional<WorkItem> existente = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, dedupKey, WorkItemStatus.CANCELADO);
        if (existente.isPresent()) {
            WorkItem item = existente.get();
            return Map.of(
                    "status", "DUPLICATA_IGNORADA",
                    "processoId", processoId,
                    "workItemId", item.getId(),
                    "dedupKey", dedupKey,
                    "descriptor", profile.descriptor());
        }
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(FaseProcessual.EXECUCAO)
                .templateCode(dedupKey)
                .type(WorkItemType.CERTIDAO)
                .titulo("Consolidação do Fechamento Executivo — " + processo.getNumeroProcesso())
                .descricao(narrativeSupport.buildClosureGovernanceDescription(profile, motivo, percentualSatisfeito, saldoRemanescente))
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
        workItemRepository.save(item);
        executionMeshStateService.recordClosureGovernance(processo, profile, item, motivo, percentualSatisfeito, saldoRemanescente);
        commons.publishUserHistory(usuario, "EXECUCAO", "CONSOLIDACAO_FECHAMENTO_EXECUTIVO", "Consolidação do fechamento executivo registrada: " + profile.closureMode(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CONSOLIDACAO_FECHAMENTO_EXECUTIVO_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("closureMode", profile.closureMode());
        out.put("closureConsistencyStatus", profile.closureConsistencyStatus());
        out.put("archiveReadiness", profile.archiveReadiness());
        out.put("residualDispositionMode", profile.residualDispositionMode());
        out.put("preferenceClosureMode", profile.preferenceClosureMode());
        out.put("subrogationClosureMode", profile.subrogationClosureMode());
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("assignedRole", profile.assignedRole().name());
        out.put("percentualSatisfeito", percentualSatisfeito);
        out.put("saldoRemanescente", saldoRemanescente);
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("profile", profile.toMap());
        return out;
    }


    public Map<String, Object> registrarSatisfacaoTerminal(Processo processo,
                                                           Long processoId,
                                                           Usuario usuario,
                                                           String modo,
                                                           double percentualSatisfeito,
                                                           double saldoRemanescente,
                                                           String fundamento) {
        ExecutionSatisfactionProfile profile = executionSatisfactionResolver.resolve(processo, modo, percentualSatisfeito, saldoRemanescente, fundamento);
        String dedupKey = UUID.nameUUIDFromBytes(("SATISFACAO_TERMINAL:" + processoId + ':' + profile.terminalDisposition() + ':' + percentualSatisfeito + ':' + saldoRemanescente).getBytes(StandardCharsets.UTF_8)).toString();
        Optional<WorkItem> existente = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, dedupKey, WorkItemStatus.CANCELADO);
        if (existente.isPresent()) {
            WorkItem item = existente.get();
            return Map.of(
                    "status", "DUPLICATA_IGNORADA",
                    "processoId", processoId,
                    "workItemId", item.getId(),
                    "dedupKey", dedupKey,
                    "descriptor", profile.descriptor()
            );
        }
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(narrativeSupport.resolveExecutionPhaseForTerminal(processo, profile))
                .templateCode(dedupKey)
                .type(narrativeSupport.resolveWorkItemTypeForTerminal(profile))
                .titulo(narrativeSupport.buildTerminalTitle(profile, processo))
                .descricao(narrativeSupport.buildTerminalDescription(fundamento, profile, percentualSatisfeito, saldoRemanescente))
                .queueCode(profile.queueCode())
                .inboxKey(profile.inboxKey())
                .assignedRole(profile.assignedRole())
                .status(narrativeSupport.resolveWorkItemStatusForTerminal(profile))
                .prioridade(profile.priority())
                .blocking(profile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(profile.baseLegal())
                .dueAt(profile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(item);
        executionMeshStateService.recordTerminalDisposition(processo, profile, item, percentualSatisfeito, saldoRemanescente, fundamento);
        commons.publishUserHistory(usuario, "EXECUCAO", "SATISFACAO_TERMINAL_REGISTRADA", "Satisfação terminal registrada: " + profile.terminalDisposition(), processo, processoId);

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "SATISFACAO_TERMINAL_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("closureMode", profile.closureMode());
        out.put("satisfactionMode", profile.satisfactionMode());
        out.put("terminalDisposition", profile.terminalDisposition());
        out.put("residualMode", profile.residualMode());
        out.put("retentionMode", profile.retentionMode());
        out.put("reopenMode", profile.reopenMode());
        out.put("percentualSatisfeito", percentualSatisfeito);
        out.put("saldoRemanescente", saldoRemanescente);
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("assignedRole", profile.assignedRole().name());
        TerminalArchiveLinkProfile preview = terminalArchiveLinkResolver.resolve(
                processo,
                "PREVIEW",
                profile.terminalDisposition(),
                fundamento,
                percentualSatisfeito,
                saldoRemanescente);
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("terminalProfile", profile.toMap());
        out.put("archiveLinkPreview", preview.toMap());
        return out;
    }

    public TerminalArchiveLinkProfile resolveArchiveLinkProfile(Processo processo,
                                                                String operacao,
                                                                String disposicaoTerminal,
                                                                String motivo,
                                                                double percentualSatisfeito,
                                                                double saldoRemanescente) {
        return terminalArchiveLinkResolver.resolve(processo, operacao, disposicaoTerminal, motivo, percentualSatisfeito, saldoRemanescente);
    }

    public Map<String, Object> vincularArquivamentoTerminal(Processo processo,
                                                            Long processoId,
                                                            Usuario usuario,
                                                            String operacao,
                                                            String disposicaoTerminal,
                                                            String motivo,
                                                            double percentualSatisfeito,
                                                            double saldoRemanescente) {
        TerminalArchiveLinkProfile profile = terminalArchiveLinkResolver.resolve(processo, operacao, disposicaoTerminal, motivo, percentualSatisfeito, saldoRemanescente);
        String dedupKey = UUID.nameUUIDFromBytes(("VINCULO_ARQUIVAMENTO_TERMINAL:" + processoId + ':' + profile.operationType() + ':' + profile.archiveLinkMode() + ':' + firstNonBlank(motivo, "SEM_MOTIVO")).getBytes(StandardCharsets.UTF_8)).toString();
        Optional<WorkItem> existente = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, dedupKey, WorkItemStatus.CANCELADO);
        if (existente.isPresent()) {
            WorkItem item = existente.get();
            return Map.of(
                    "status", "DUPLICATA_IGNORADA",
                    "processoId", processoId,
                    "workItemId", item.getId(),
                    "dedupKey", dedupKey,
                    "descriptor", profile.descriptor());
        }
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(FaseProcessual.EXECUCAO)
                .templateCode(dedupKey)
                .type(WorkItemType.CERTIDAO)
                .titulo("Vínculo Arquivamento/Terminal — " + processo.getNumeroProcesso())
                .descricao(narrativeSupport.buildArchiveLinkDescription(profile, motivo, percentualSatisfeito, saldoRemanescente))
                .queueCode(profile.archiveQueue())
                .inboxKey(profile.archiveInbox())
                .assignedRole(profile.assignedRole())
                .status(profile.blocking() ? WorkItemStatus.PENDENTE : WorkItemStatus.CONCLUIDO)
                .prioridade(profile.priority())
                .blocking(profile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(profile.baseLegal())
                .dueAt(profile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(item);
        executionMeshStateService.recordArchiveLinkage(processo, profile, item, motivo);
        commons.publishUserHistory(usuario, "EXECUCAO", "VINCULO_ARQUIVAMENTO_TERMINAL_REGISTRADO", "Vínculo arquivo/terminal registrado: " + profile.archiveLinkMode(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "VINCULO_ARQUIVAMENTO_TERMINAL_REGISTRADO");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("archiveEligibility", profile.archiveEligibility());
        out.put("archiveLinkMode", profile.archiveLinkMode());
        out.put("archiveReviewDesk", profile.archiveReviewDesk());
        out.put("retentionClass", profile.retentionClass());
        out.put("reactivationMode", profile.reactivationMode());
        out.put("queueCode", profile.archiveQueue());
        out.put("inboxKey", profile.archiveInbox());
        out.put("assignedRole", profile.assignedRole().name());
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("profile", profile.toMap());
        return out;
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
