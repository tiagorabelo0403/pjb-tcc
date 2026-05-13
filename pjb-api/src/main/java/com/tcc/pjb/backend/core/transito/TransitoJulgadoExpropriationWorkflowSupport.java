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
public class TransitoJulgadoExpropriationWorkflowSupport {

    private final ExpropriationGovernanceResolver expropriationGovernanceResolver;
    private final ExpropriationAuctionCycleResolver expropriationAuctionCycleResolver;
    private final ExpropriationHomologationResolver expropriationHomologationResolver;
    private final ExpropriationSettlementResolver expropriationSettlementResolver;
    private final ExecutionEnforcementResolver executionEnforcementResolver;
    private final WorkItemRepository workItemRepository;
    private final PainelServiceCommons commons;
    private final TransitoJulgadoNarrativeSupport narrativeSupport;
    private final ExecutionMeshStateService executionMeshStateService;

    public TransitoJulgadoExpropriationWorkflowSupport(
            ExpropriationGovernanceResolver expropriationGovernanceResolver,
            ExpropriationAuctionCycleResolver expropriationAuctionCycleResolver,
            ExpropriationHomologationResolver expropriationHomologationResolver,
            ExpropriationSettlementResolver expropriationSettlementResolver,
            ExecutionEnforcementResolver executionEnforcementResolver,
            WorkItemRepository workItemRepository,
            PainelServiceCommons commons,
            TransitoJulgadoNarrativeSupport narrativeSupport,
            ExecutionMeshStateService executionMeshStateService
    ) {
        this.expropriationGovernanceResolver = expropriationGovernanceResolver;
        this.expropriationAuctionCycleResolver = expropriationAuctionCycleResolver;
        this.expropriationHomologationResolver = expropriationHomologationResolver;
        this.expropriationSettlementResolver = expropriationSettlementResolver;
        this.executionEnforcementResolver = executionEnforcementResolver;
        this.workItemRepository = workItemRepository;
        this.commons = commons;
        this.narrativeSupport = narrativeSupport;
        this.executionMeshStateService = executionMeshStateService;
    }

    public Map<String, Object> homologarExpropriacaoFinal(Processo processo,
                                                          Long processoId,
                                                          Usuario usuario,
                                                          String ato,
                                                          String bem,
                                                          String modalidade,
                                                          String adquirente,
                                                          double valorArrematacao) {
        ExpropriationHomologationProfile profile = expropriationHomologationResolver.resolve(processo, ato, bem, modalidade, adquirente, valorArrematacao);
        String dedupKey = UUID.nameUUIDFromBytes(("HOMOLOGACAO_EXPROPRIACAO:" + processoId + ':' + profile.actType() + ':' + profile.assetKind() + ':' + firstNonBlank(adquirente, "SEM_ADQUIRENTE")).getBytes(StandardCharsets.UTF_8)).toString();
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
                .type(WorkItemType.CERTIDAO)
                .titulo(narrativeSupport.buildHomologationTitle(profile, processo))
                .descricao(narrativeSupport.buildHomologationDescription(profile, adquirente, valorArrematacao))
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
        executionMeshStateService.recordExpropriationHomologation(processo, profile, item, adquirente, valorArrematacao);
        commons.publishUserHistory(usuario, "EXECUCAO", "HOMOLOGACAO_EXPROPRIACAO_FINAL", "Homologação final registrada: " + profile.homologationMode(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "HOMOLOGACAO_EXPROPRIACAO_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("actType", profile.actType());
        out.put("assetKind", profile.assetKind());
        out.put("homologationMode", profile.homologationMode());
        out.put("titleTransferMode", profile.titleTransferMode());
        out.put("possessionDeliveryMode", profile.possessionDeliveryMode());
        out.put("depositReleaseMode", profile.depositReleaseMode());
        out.put("preferenceReviewDesk", profile.preferenceReviewDesk());
        out.put("settlementTriggerMode", profile.settlementTriggerMode());
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("assignedRole", profile.assignedRole().name());
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("profile", profile.toMap());
        return out;
    }


    public Map<String, Object> liquidarProdutoExpropriacao(Processo processo,
                                                           Long processoId,
                                                           Usuario usuario,
                                                           String bem,
                                                           String modoProduto,
                                                           String preferencia,
                                                           String subrogacao,
                                                           double valorProduto,
                                                           double saldoExecutado,
                                                           double saldoCredor) {
        ExpropriationSettlementProfile profile = expropriationSettlementResolver.resolve(processo, bem, modoProduto, preferencia, subrogacao, valorProduto, saldoExecutado, saldoCredor);
        String dedupKey = UUID.nameUUIDFromBytes(("LIQUIDACAO_PRODUTO_EXPROPRIACAO:" + processoId + ':' + profile.assetKind() + ':' + profile.settlementMode() + ':' + valorProduto + ':' + saldoCredor).getBytes(StandardCharsets.UTF_8)).toString();
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
                .type(WorkItemType.CALCULO)
                .titulo(narrativeSupport.buildSettlementTitle(profile, processo))
                .descricao(narrativeSupport.buildSettlementDescription(profile, valorProduto, saldoExecutado, saldoCredor))
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
        executionMeshStateService.recordExpropriationSettlement(processo, profile, item, valorProduto, saldoExecutado, saldoCredor);
        commons.publishUserHistory(usuario, "EXECUCAO", "LIQUIDACAO_PRODUTO_EXPROPRIACAO", "Liquidação do produto registrada: " + profile.settlementMode(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "LIQUIDACAO_PRODUTO_EXPROPRIACAO_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("assetKind", profile.assetKind());
        out.put("settlementMode", profile.settlementMode());
        out.put("proceedsMode", profile.proceedsMode());
        out.put("preferenceMode", profile.preferenceMode());
        out.put("subrogationMode", profile.subrogationMode());
        out.put("balanceMode", profile.balanceMode());
        out.put("surplusMode", profile.surplusMode());
        out.put("terminalDispositionHint", profile.terminalDispositionHint());
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("assignedRole", profile.assignedRole().name());
        out.put("valorProduto", valorProduto);
        out.put("saldoExecutado", saldoExecutado);
        out.put("saldoCredor", saldoCredor);
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("profile", profile.toMap());
        return out;
    }


    public Map<String, Object> governarExpropriacao(Processo processo,
                                                    Long processoId,
                                                    Usuario usuario,
                                                    String ato,
                                                    String bem,
                                                    String modalidade,
                                                    double valorReferencia) {
        ExpropriationGovernanceProfile profile = expropriationGovernanceResolver.resolve(processo, ato, bem, modalidade, valorReferencia);
        String dedupKey = UUID.nameUUIDFromBytes(("EXPROPRIACAO:" + processoId + ':' + profile.actType() + ':' + firstNonBlank(bem, "SEM_BEM") + ':' + firstNonBlank(modalidade, "SEM_MODALIDADE")).getBytes(StandardCharsets.UTF_8)).toString();
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
                .faseOrigem(narrativeSupport.resolveExecutionPhaseForAct(processo, executionEnforcementResolver.resolve(processo, ato, modalidade, valorReferencia)))
                .templateCode(dedupKey)
                .type(narrativeSupport.resolveWorkItemTypeForExpropriation(profile))
                .titulo(narrativeSupport.buildExpropriationTitle(profile, processo))
                .descricao(narrativeSupport.buildExpropriationDescription(profile, modalidade, valorReferencia))
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
        executionMeshStateService.recordExpropriationGovernance(processo, profile, item, modalidade, valorReferencia);
        commons.publishUserHistory(usuario, "EXECUCAO", "GOVERNANCA_EXPROPRIACAO_REGISTRADA", "Governança de expropriação registrada: " + profile.expropriationMode(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "GOVERNANCA_EXPROPRIACAO_REGISTRADA");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("expropriationMode", profile.expropriationMode());
        out.put("sessionMode", profile.sessionMode());
        out.put("publicationMode", profile.publicationMode());
        out.put("priceFloorMode", profile.priceFloorMode());
        out.put("preferenceDesk", profile.preferenceDesk());
        out.put("fraudReviewDesk", profile.fraudReviewDesk());
        out.put("queueCode", profile.queueCode());
        out.put("inboxKey", profile.inboxKey());
        out.put("assignedRole", profile.assignedRole().name());
        out.put("warnings", profile.warnings());
        out.put("fundamentos", profile.fundamentos());
        out.put("reviewChecklist", profile.reviewChecklist());
        out.put("profile", profile.toMap());
        return out;
    }


    public Map<String, Object> planejarCicloLeilaoExpropriatorio(Processo processo,
                                                                 Long processoId,
                                                                 Usuario usuario,
                                                                 String ato,
                                                                 String bem,
                                                                 String modalidade,
                                                                 int tentativa,
                                                                 double valorReferencia) {
        ExpropriationAuctionCycleProfile profile = expropriationAuctionCycleResolver.resolve(processo, ato, bem, modalidade, tentativa, valorReferencia);
        String dedupKey = UUID.nameUUIDFromBytes(("CICLO_LEILAO:" + processoId + ':' + profile.actType() + ':' + profile.assetKind() + ':' + tentativa + ':' + firstNonBlank(modalidade, "SEM_MODALIDADE")).getBytes(StandardCharsets.UTF_8)).toString();
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
                .type(profile.actType().equals("ADJUDICACAO") ? WorkItemType.DECISAO : WorkItemType.EXPEDICAO)
                .titulo(narrativeSupport.buildAuctionCycleTitle(profile, processo, tentativa))
                .descricao(narrativeSupport.buildAuctionCycleDescription(profile, modalidade, tentativa, valorReferencia))
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
        executionMeshStateService.recordAuctionCycle(processo, profile, item, modalidade, tentativa, valorReferencia);
        commons.publishUserHistory(usuario, "EXECUCAO", "CICLO_LEILAO_EXPROPRIATORIO_REGISTRADO", "Ciclo de leilão expropriatório registrado: " + profile.cycleMode(), processo, processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CICLO_LEILAO_EXPROPRIATORIO_REGISTRADO");
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("workItemId", item.getId());
        out.put("dedupKey", dedupKey);
        out.put("descriptor", profile.descriptor());
        out.put("cycleMode", profile.cycleMode());
        out.put("pracaMode", profile.pracaMode());
        out.put("bidWindowMode", profile.bidWindowMode());
        out.put("publicationRefreshMode", profile.publicationRefreshMode());
        out.put("remicaoMode", profile.remicaoMode());
        out.put("parcelamentoMode", profile.parcelamentoMode());
        out.put("antiCollusionDesk", profile.antiCollusionDesk());
        out.put("homologationDesk", profile.homologationDesk());
        out.put("settlementDeadlineMode", profile.settlementDeadlineMode());
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
