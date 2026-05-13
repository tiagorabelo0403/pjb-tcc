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
public class TransitoJulgadoPatrimonialWorkflowSupport {

    private final PatrimonialConstrictionResolver patrimonialConstrictionResolver;
    private final ExternalConstrictionResolver externalConstrictionResolver;
    private final ExternalConstrictionContingencyResolver externalConstrictionContingencyResolver;
    private final ExternalConstrictionReconciliationResolver externalConstrictionReconciliationResolver;
    private final WorkItemRepository workItemRepository;
    private final PainelServiceCommons commons;
    private final TransitoJulgadoNarrativeSupport narrativeSupport;
    private final ExecutionMeshStateService executionMeshStateService;

    public TransitoJulgadoPatrimonialWorkflowSupport(
            PatrimonialConstrictionResolver patrimonialConstrictionResolver,
            ExternalConstrictionResolver externalConstrictionResolver,
            ExternalConstrictionContingencyResolver externalConstrictionContingencyResolver,
            ExternalConstrictionReconciliationResolver externalConstrictionReconciliationResolver,
            WorkItemRepository workItemRepository,
            PainelServiceCommons commons,
            TransitoJulgadoNarrativeSupport narrativeSupport,
            ExecutionMeshStateService executionMeshStateService
    ) {
        this.patrimonialConstrictionResolver = patrimonialConstrictionResolver;
        this.externalConstrictionResolver = externalConstrictionResolver;
        this.externalConstrictionContingencyResolver = externalConstrictionContingencyResolver;
        this.externalConstrictionReconciliationResolver = externalConstrictionReconciliationResolver;
        this.workItemRepository = workItemRepository;
        this.commons = commons;
        this.narrativeSupport = narrativeSupport;
        this.executionMeshStateService = executionMeshStateService;
    }

    public Map<String, Object> praticarConstricaoPatrimonial(Processo processo,
                                                             Long processoId,
                                                             Usuario usuario,
                                                             PostJudgmentOperationalProfile operationalProfile,
                                                             String ato,
                                                             String bem,
                                                             String detalhe,
                                                             String convenio,
                                                             double valorOperacao) {
        PatrimonialConstrictionProfile patrimonialProfile = patrimonialConstrictionResolver.resolve(processo, ato, bem, detalhe, convenio, valorOperacao);
        String dedupKey = UUID.nameUUIDFromBytes(("CONSTRICAO_PATRIMONIAL:" + processoId + ':' + patrimonialProfile.actType() + ':' + patrimonialProfile.assetKind() + ':' + firstNonBlank(convenio, "SEM_CONVENIO")).getBytes(StandardCharsets.UTF_8)).toString();
        Optional<WorkItem> existente = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, dedupKey, WorkItemStatus.CANCELADO);
        if (existente.isPresent()) {
            WorkItem item = existente.get();
            return Map.of(
                    "status", "DUPLICATA_IGNORADA",
                    "processoId", processoId,
                    "workItemId", item.getId(),
                    "dedupKey", dedupKey,
                    "descriptor", patrimonialProfile.descriptor()
            );
        }
        WorkItem item = WorkItem.builder()
                .processo(processo)
                .faseOrigem(narrativeSupport.resolveExecutionPhaseForPatrimonial(processo, patrimonialProfile))
                .templateCode(dedupKey)
                .type(narrativeSupport.resolveWorkItemTypeForPatrimonial(patrimonialProfile))
                .titulo(narrativeSupport.buildPatrimonialTitle(patrimonialProfile, processo))
                .descricao(narrativeSupport.buildPatrimonialDescription(bem, detalhe, convenio, operationalProfile, patrimonialProfile, valorOperacao))
                .queueCode(patrimonialProfile.queueCode())
                .inboxKey(patrimonialProfile.inboxKey())
                .assignedRole(patrimonialProfile.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(patrimonialProfile.priority())
                .blocking(patrimonialProfile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(patrimonialProfile.baseLegal())
                .dueAt(patrimonialProfile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(item);
        executionMeshStateService.recordPatrimonialConstriction(processo, patrimonialProfile, item, detalhe, convenio, valorOperacao);
        commons.publishUserHistory(usuario, "EXECUCAO", "CONSTRICAO_PATRIMONIAL_REGISTRADA", "Constrição patrimonial registrada: " + patrimonialProfile.assetKind(), processo, processoId);

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CONSTRICAO_PATRIMONIAL_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", patrimonialProfile.descriptor());
        out.put("actType", patrimonialProfile.actType());
        out.put("assetKind", patrimonialProfile.assetKind());
        out.put("assetClass", patrimonialProfile.assetClass());
        out.put("queueCode", patrimonialProfile.queueCode());
        out.put("inboxKey", patrimonialProfile.inboxKey());
        out.put("assignedRole", patrimonialProfile.assignedRole().name());
        out.put("constrictionMode", patrimonialProfile.constrictionMode());
        out.put("registryMode", patrimonialProfile.registryMode());
        out.put("evaluationMode", patrimonialProfile.evaluationMode());
        out.put("expropriationMode", patrimonialProfile.expropriationMode());
        out.put("warnings", patrimonialProfile.warnings());
        out.put("fundamentos", patrimonialProfile.fundamentos());
        out.put("reviewChecklist", patrimonialProfile.reviewChecklist());
        out.put("operationalProfile", operationalProfile.toMap());
        out.put("patrimonialProfile", patrimonialProfile.toMap());
        return out;
    }


    public Map<String, Object> integrarConstricaoExterna(Processo processo,
                                                         Long processoId,
                                                         Usuario usuario,
                                                         String ato,
                                                         String bem,
                                                         String convenio,
                                                         String referenciaExterna,
                                                         double valorOperacao) {
        ExternalConstrictionProfile profile = externalConstrictionResolver.resolve(processo, ato, bem, convenio, valorOperacao);
        String dedupKey = UUID.nameUUIDFromBytes(("INTEGRACAO_CONSTRICAO:" + processoId + ':' + profile.gatewayCode() + ':' + profile.assetKind() + ':' + firstNonBlank(referenciaExterna, "SEM_REFERENCIA")).getBytes(StandardCharsets.UTF_8)).toString();
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
                .faseOrigem(FaseProcessual.PENHORA)
                .templateCode(dedupKey)
                .type(WorkItemType.EXPEDICAO)
                .titulo(narrativeSupport.buildExternalTitle(profile, processo))
                .descricao(narrativeSupport.buildExternalDescription(referenciaExterna, profile, valorOperacao))
                .queueCode(profile.queueCode())
                .inboxKey(profile.inboxKey())
                .assignedRole(profile.assignedRole())
                .status(WorkItemStatus.EM_EXECUCAO)
                .prioridade(profile.priority())
                .blocking(profile.blocking())
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .baseLegal(firstNonBlank(profile.metadata().get("baseLegal") != null ? String.valueOf(profile.metadata().get("baseLegal")) : null, profile.fundamentos().isEmpty() ? null : profile.fundamentos().getFirst()))
                .dueAt(profile.dueAtFrom(Instant.now()))
                .build();
        workItemRepository.save(item);
        executionMeshStateService.recordExternalConstriction(processo, profile, item, referenciaExterna, valorOperacao);
        commons.publishUserHistory(usuario, "EXECUCAO", "INTEGRACAO_CONSTRICAO_EXTERNA", "Integração de constrição externa registrada: " + profile.gatewayCode(), processo, processoId);

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "INTEGRACAO_CONSTRICAO_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("gatewayCode", profile.gatewayCode());
        out.put("assetKind", profile.assetKind());
        out.put("statusTarget", profile.statusTarget());
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("assignedRole", profile.assignedRole().name());
        out.put("requestMode", profile.requestMode());
        out.put("protocolMode", profile.protocolMode());
        out.put("responseMode", profile.responseMode());
        out.put("contingencyMode", profile.contingencyMode());
        out.put("reconciliationMode", profile.reconciliationMode());
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("externalProfile", profile.toMap());
        return out;
    }


    public Map<String, Object> reconciliarConstricaoExterna(Processo processo,
                                                            Long processoId,
                                                            Usuario usuario,
                                                            String bem,
                                                            String convenio,
                                                            String statusExterno,
                                                            String referenciaExterna,
                                                            double valorOperacao) {
        ExternalConstrictionReconciliationProfile profile = externalConstrictionReconciliationResolver.resolve(processo, bem, convenio, statusExterno, referenciaExterna, valorOperacao);
        String dedupKey = UUID.nameUUIDFromBytes(("RECONCILIACAO_CONSTRICAO:" + processoId + ':' + profile.gatewayCode() + ':' + profile.externalStatus() + ':' + firstNonBlank(referenciaExterna, "SEM_REFERENCIA")).getBytes(StandardCharsets.UTF_8)).toString();
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
                .faseOrigem(FaseProcessual.PENHORA)
                .templateCode(dedupKey)
                .type(WorkItemType.DILIGENCIA)
                .titulo(narrativeSupport.buildReconciliationTitle(profile, processo))
                .descricao(narrativeSupport.buildReconciliationDescription(profile, referenciaExterna, valorOperacao))
                .queueCode(profile.queueCode())
                .inboxKey(profile.inboxKey())
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
        executionMeshStateService.recordExternalReconciliation(processo, profile, item, referenciaExterna, valorOperacao);
        commons.publishUserHistory(usuario, "EXECUCAO", "RECONCILIACAO_CONSTRICAO_REGISTRADA", "Reconciliação externa registrada: " + profile.reconciliationStatus(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "RECONCILIACAO_CONSTRICAO_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("gatewayCode", profile.gatewayCode());
        out.put("externalStatus", profile.externalStatus());
        out.put("reconciliationStatus", profile.reconciliationStatus());
        out.put("contingencyDesk", profile.contingencyDesk());
        out.put("reconciliationDesk", profile.reconciliationDesk());
        out.put("proofDesk", profile.proofDesk());
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("assignedRole", profile.assignedRole().name());
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("profile", profile.toMap());
        return out;
    }


    public Map<String, Object> deflagrarContingenciaConstricaoExterna(Processo processo,
                                                                      Long processoId,
                                                                      Usuario usuario,
                                                                      String bem,
                                                                      String convenio,
                                                                      String statusExterno,
                                                                      String referenciaExterna,
                                                                      double valorOperacao) {
        ExternalConstrictionContingencyProfile profile = externalConstrictionContingencyResolver.resolve(processo, bem, convenio, statusExterno, referenciaExterna, valorOperacao);
        String dedupKey = UUID.nameUUIDFromBytes(("CONTINGENCIA_CONSTRICAO:" + processoId + ':' + profile.gatewayCode() + ':' + profile.contingencyMode() + ':' + firstNonBlank(referenciaExterna, "SEM_REFERENCIA")).getBytes(StandardCharsets.UTF_8)).toString();
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
                .faseOrigem(FaseProcessual.PENHORA)
                .templateCode(dedupKey)
                .type(WorkItemType.DILIGENCIA)
                .titulo(narrativeSupport.buildContingencyTitle(profile, processo))
                .descricao(narrativeSupport.buildContingencyDescription(profile, referenciaExterna, valorOperacao))
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
        executionMeshStateService.recordExternalContingency(processo, profile, item, referenciaExterna, valorOperacao);
        commons.publishUserHistory(usuario, "EXECUCAO", "CONTINGENCIA_CONSTRICAO_EXTERNA_REGISTRADA", "Contingência externa registrada: " + profile.contingencyMode(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CONTINGENCIA_CONSTRICAO_EXTERNA_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("gatewayCode", profile.gatewayCode());
        out.put("assetKind", profile.assetKind());
        out.put("contingencyMode", profile.contingencyMode());
        out.put("fallbackChannel", profile.fallbackChannel());
        out.put("replayMode", profile.replayMode());
        out.put("manualReviewDesk", profile.manualReviewDesk());
        out.put("proofGapMode", profile.proofGapMode());
        out.put("escalationLevel", profile.escalationLevel());
        out.put("finalizationTarget", profile.finalizationTarget());
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
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
